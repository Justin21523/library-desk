package com.justin.libradesk.infrastructure.marc;

import com.justin.libradesk.domain.enumtype.MaterialType;

import java.util.List;

/**
 * MARC workform templates that pre-fill a new record in the editor. The Leader
 * positions 6/7 (type/bibliographic level) and the seed fields differ per
 * material type, matching how a cataloguer starts a record.
 */
public final class MarcTemplates {

    private MarcTemplates() {
    }

    public static MarcLineCodec.DecodedMarc forType(MaterialType type) {
        return switch (type) {
            case SERIAL -> new MarcLineCodec.DecodedMarc("00000nas a2200000 a 4500", List.of(
                    new MarcLine("008", "", ""),
                    new MarcLine("022", " ", "$a"),
                    new MarcLine("245", "00", "$a"),
                    new MarcLine("264", " 1", "$a$b$c"),
                    new MarcLine("310", " ", "$a")));
            case EBOOK -> new MarcLineCodec.DecodedMarc("00000nmm a2200000 a 4500", List.of(
                    new MarcLine("008", "", ""),
                    new MarcLine("020", " ", "$a"),
                    new MarcLine("245", "10", "$a"),
                    new MarcLine("264", " 1", "$a$b$c"),
                    new MarcLine("856", "40", "$u")));
            default -> new MarcLineCodec.DecodedMarc("00000nam a2200000 a 4500", List.of(
                    new MarcLine("008", "", ""),
                    new MarcLine("020", " ", "$a"),
                    new MarcLine("100", "1", "$a"),
                    new MarcLine("245", "10", "$a"),
                    new MarcLine("264", " 1", "$a$b$c"),
                    new MarcLine("300", " ", "$a")));
        };
    }
}
