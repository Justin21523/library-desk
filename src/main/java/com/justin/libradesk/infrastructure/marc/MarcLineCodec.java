package com.justin.libradesk.infrastructure.marc;

import org.marc4j.marc.ControlField;
import org.marc4j.marc.DataField;
import org.marc4j.marc.Leader;
import org.marc4j.marc.MarcFactory;
import org.marc4j.marc.Record;
import org.marc4j.marc.Subfield;
import org.marc4j.marc.VariableField;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts between a marc4j {@link Record} and the flat, editable {@link MarcLine}
 * representation used by the MARC editor. Subfields are encoded inline with a
 * {@code $} delimiter (e.g. {@code $aTitle :$bsubtitle}); control fields (00x)
 * carry their raw value as the content.
 */
public class MarcLineCodec {

    /** A decoded record: the 24-char leader plus its field lines. */
    public record DecodedMarc(String leader, List<MarcLine> fields) {
    }

    private static final char DELIMITER = '$';

    private final MarcFactory factory = MarcFactory.newInstance();

    public DecodedMarc fromRecord(Record record) {
        List<MarcLine> fields = new ArrayList<>();
        for (VariableField field : record.getVariableFields()) {
            if (field instanceof ControlField control) {
                fields.add(new MarcLine(control.getTag(), "", nullToEmpty(control.getData())));
            } else if (field instanceof DataField data) {
                fields.add(new MarcLine(data.getTag(),
                        "" + data.getIndicator1() + data.getIndicator2(), subfieldString(data)));
            }
        }
        Leader leader = record.getLeader();
        return new DecodedMarc(leader == null ? "" : leader.toString(), fields);
    }

    public Record toRecord(String leader, List<MarcLine> fields) {
        Record record = factory.newRecord();
        if (leader != null && leader.strip().length() == 24) {
            record.setLeader(factory.newLeader(leader));
        }
        for (MarcLine line : fields) {
            String tag = line.tag() == null ? "" : line.tag().trim();
            if (tag.length() != 3) {
                continue; // skip malformed rows
            }
            if (isControlTag(tag)) {
                record.addVariableField(factory.newControlField(tag, nullToEmpty(line.content())));
            } else {
                DataField data = factory.newDataField(tag,
                        indicator(line.indicators(), 0), indicator(line.indicators(), 1));
                for (Subfield subfield : parseSubfields(line.content())) {
                    data.addSubfield(subfield);
                }
                record.addVariableField(data);
            }
        }
        return record;
    }

    private String subfieldString(DataField field) {
        StringBuilder sb = new StringBuilder();
        for (Subfield subfield : field.getSubfields()) {
            sb.append(DELIMITER).append(subfield.getCode()).append(subfield.getData());
        }
        return sb.toString();
    }

    private List<Subfield> parseSubfields(String content) {
        List<Subfield> subfields = new ArrayList<>();
        if (content == null || content.isBlank()) {
            return subfields;
        }
        for (String token : content.split("\\" + DELIMITER)) {
            if (token.isEmpty()) {
                continue; // text before the first delimiter, or empties
            }
            char code = token.charAt(0);
            String data = token.substring(1);
            subfields.add(factory.newSubfield(code, data));
        }
        return subfields;
    }

    private static boolean isControlTag(String tag) {
        return tag.startsWith("00");
    }

    private static char indicator(String indicators, int index) {
        if (indicators == null || indicators.length() <= index) {
            return ' ';
        }
        char ch = indicators.charAt(index);
        return ch == '_' ? ' ' : ch;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
