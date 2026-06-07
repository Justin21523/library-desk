package com.justin.libradesk.infrastructure.marc;

import com.justin.libradesk.domain.enumtype.ClassificationScheme;
import com.justin.libradesk.domain.model.Book;
import org.marc4j.marc.DataField;
import org.marc4j.marc.MarcFactory;
import org.marc4j.marc.Record;
import org.marc4j.marc.Subfield;
import org.marc4j.marc.VariableField;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts between marc4j {@link Record}s and {@link MarcData}. Covers the common
 * monograph fields: 245 title, 100/700 authors, 264/260 publication, 020 ISBN,
 * 250 edition, 300 extent, 490 series, 520 summary, 650 subjects, 001 control no.
 */
public class MarcMapper {

    private static final Pattern YEAR = Pattern.compile("(\\d{4})");
    private final MarcFactory factory = MarcFactory.newInstance();

    // --- MARC -> domain ---

    public MarcData toMarcData(Record record) {
        Book book = new Book();
        book.setControlNumber(record.getControlNumber());
        book.setTitle(title(record));
        book.setIsbn(firstSubfield(record, "020", 'a', value -> value.split(" ")[0]));
        book.setEdition(firstSubfield(record, "250", 'a', MarcMapper::trim));
        book.setExtent(firstSubfield(record, "300", 'a', MarcMapper::trim));
        book.setSeries(firstSubfield(record, "490", 'a', MarcMapper::trim));
        book.setSummary(firstSubfield(record, "520", 'a', MarcMapper::trim));

        DataField publication = (DataField) firstOf(record, "264", "260");
        String publisherName = null;
        if (publication != null) {
            book.setPubPlace(subfield(publication, 'a', MarcMapper::trim));
            publisherName = subfield(publication, 'b', MarcMapper::trim);
            book.setPublishedYear(year(subfield(publication, 'c', s -> s)));
        }

        List<String> authors = new ArrayList<>();
        authors.addAll(subfields(record, "100", 'a'));
        authors.addAll(subfields(record, "700", 'a'));

        List<String> subjects = new ArrayList<>(subfields(record, "650", 'a'));

        // Call number: prefer DDC (082) then LCC (050).
        String ddc = firstSubfield(record, "082", 'a', MarcMapper::trim);
        if (ddc != null) {
            book.setCallNumber(ddc);
            book.setClassificationScheme(ClassificationScheme.DDC);
        } else {
            String lcc = firstSubfield(record, "050", 'a', MarcMapper::trim);
            if (lcc != null) {
                book.setCallNumber(lcc);
                book.setClassificationScheme(ClassificationScheme.LCC);
            }
        }

        return new MarcData(book, authors, subjects, publisherName);
    }

    // --- domain -> MARC ---

    public Record toRecord(MarcData data) {
        Book book = data.book();
        Record record = factory.newRecord("00000nam a2200000 a 4500");
        if (book.getControlNumber() != null) {
            record.addVariableField(factory.newControlField("001", book.getControlNumber()));
        }
        if (book.getIsbn() != null) {
            record.addVariableField(dataField("020", 'a', book.getIsbn()));
        }
        if (book.getCallNumber() != null) {
            String tag = book.getClassificationScheme() == ClassificationScheme.LCC ? "050" : "082";
            record.addVariableField(dataField(tag, 'a', book.getCallNumber()));
        }
        for (String author : data.authorNames()) {
            record.addVariableField(dataField("100", 'a', author));
        }
        if (book.getTitle() != null) {
            record.addVariableField(dataField("245", 'a', book.getTitle()));
        }
        if (book.getEdition() != null) {
            record.addVariableField(dataField("250", 'a', book.getEdition()));
        }
        DataField publication = factory.newDataField("264", ' ', '1');
        addSubfield(publication, 'a', book.getPubPlace());
        addSubfield(publication, 'b', data.publisherName());
        addSubfield(publication, 'c', book.getPublishedYear() == null ? null : book.getPublishedYear().toString());
        if (!publication.getSubfields().isEmpty()) {
            record.addVariableField(publication);
        }
        if (book.getExtent() != null) {
            record.addVariableField(dataField("300", 'a', book.getExtent()));
        }
        if (book.getSeries() != null) {
            record.addVariableField(dataField("490", 'a', book.getSeries()));
        }
        if (book.getSummary() != null) {
            record.addVariableField(dataField("520", 'a', book.getSummary()));
        }
        for (String subject : data.subjectTerms()) {
            record.addVariableField(dataField("650", 'a', subject));
        }
        return record;
    }

    private String title(Record record) {
        DataField field = (DataField) record.getVariableField("245");
        if (field == null) {
            return null;
        }
        StringBuilder title = new StringBuilder();
        appendSubfield(title, field, 'a');
        appendSubfield(title, field, 'b');
        return trim(title.toString());
    }

    private void appendSubfield(StringBuilder builder, DataField field, char code) {
        Subfield subfield = field.getSubfield(code);
        if (subfield != null) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(subfield.getData());
        }
    }

    private DataField dataField(String tag, char code, String value) {
        DataField field = factory.newDataField(tag, ' ', ' ');
        field.addSubfield(factory.newSubfield(code, value));
        return field;
    }

    private void addSubfield(DataField field, char code, String value) {
        if (value != null && !value.isBlank()) {
            field.addSubfield(factory.newSubfield(code, value));
        }
    }

    private VariableField firstOf(Record record, String... tags) {
        for (String tag : tags) {
            VariableField field = record.getVariableField(tag);
            if (field != null) {
                return field;
            }
        }
        return null;
    }

    private String firstSubfield(Record record, String tag, char code, java.util.function.Function<String, String> map) {
        DataField field = (DataField) record.getVariableField(tag);
        return field == null ? null : subfield(field, code, map);
    }

    private String subfield(DataField field, char code, java.util.function.Function<String, String> map) {
        Subfield subfield = field.getSubfield(code);
        return subfield == null ? null : map.apply(subfield.getData());
    }

    private List<String> subfields(Record record, String tag, char code) {
        List<String> values = new ArrayList<>();
        for (VariableField field : record.getVariableFields(tag)) {
            Subfield subfield = ((DataField) field).getSubfield(code);
            if (subfield != null) {
                values.add(trim(subfield.getData()));
            }
        }
        return values;
    }

    private Integer year(String value) {
        if (value == null) {
            return null;
        }
        Matcher matcher = YEAR.matcher(value);
        return matcher.find() ? Integer.valueOf(matcher.group(1)) : null;
    }

    /**
     * Trims whitespace and trailing ISBD area separators ({@code / : ; ,}) MARC
     * subfields often carry. Periods are kept — they are frequently part of an
     * abbreviation (e.g. "3rd ed.") rather than a separator.
     */
    private static String trim(String value) {
        if (value == null) {
            return null;
        }
        return value.strip().replaceAll("[\\s/:;,]+$", "").strip();
    }
}
