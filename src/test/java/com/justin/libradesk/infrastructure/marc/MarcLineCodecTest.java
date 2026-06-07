package com.justin.libradesk.infrastructure.marc;

import org.junit.jupiter.api.Test;
import org.marc4j.marc.DataField;
import org.marc4j.marc.Record;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MarcLineCodecTest {

    private final MarcLineCodec codec = new MarcLineCodec();

    @Test
    void encodesControlAndDataFieldsToLines() {
        Record record = codec.toRecord("00000nam a2200000 a 4500", List.of(
                new MarcLine("008", "", "200101s2018    xxu"),
                new MarcLine("245", "10", "$aEffective Java :$bsubtitle")));

        MarcLineCodec.DecodedMarc decoded = codec.fromRecord(record);

        assertEquals(2, decoded.fields().size());
        assertEquals("008", decoded.fields().get(0).tag());
        assertEquals("200101s2018    xxu", decoded.fields().get(0).content());
        assertEquals("245", decoded.fields().get(1).tag());
        assertEquals("10", decoded.fields().get(1).indicators());
        assertEquals("$aEffective Java :$bsubtitle", decoded.fields().get(1).content());
    }

    @Test
    void parsesInlineSubfieldsIntoMarcSubfields() {
        Record record = codec.toRecord("00000nam a2200000 a 4500",
                List.of(new MarcLine("245", "10", "$aTitle :$bsub :$cresp")));

        DataField title = (DataField) record.getVariableField("245");
        assertEquals('1', title.getIndicator1());
        assertEquals('0', title.getIndicator2());
        assertEquals("Title :", title.getSubfield('a').getData());
        assertEquals("sub :", title.getSubfield('b').getData());
        assertEquals("resp", title.getSubfield('c').getData());
    }

    @Test
    void underscoreIndicatorBecomesBlank() {
        Record record = codec.toRecord("00000nam a2200000 a 4500",
                List.of(new MarcLine("650", "_0", "$aJava")));

        DataField subject = (DataField) record.getVariableField("650");
        assertEquals(' ', subject.getIndicator1());
        assertEquals('0', subject.getIndicator2());
    }
}
