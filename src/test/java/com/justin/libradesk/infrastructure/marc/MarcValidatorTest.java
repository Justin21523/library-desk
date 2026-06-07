package com.justin.libradesk.infrastructure.marc;

import org.junit.jupiter.api.Test;
import org.marc4j.marc.Record;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarcValidatorTest {

    private final MarcLineCodec codec = new MarcLineCodec();
    private final MarcValidator validator = new MarcValidator();

    @Test
    void acceptsAWellFormedRecord() {
        Record record = codec.toRecord("00000nam a2200000 a 4500", List.of(
                new MarcLine("008", "", "200101s2018    xxu"),
                new MarcLine("245", "10", "$aEffective Java")));

        assertFalse(validator.hasErrors(validator.validate(record)));
    }

    @Test
    void flagsMissingTitleAsError() {
        Record record = codec.toRecord("00000nam a2200000 a 4500", List.of(
                new MarcLine("008", "", "200101s2018    xxu"),
                new MarcLine("260", " ", "$aBoston")));

        List<String> issues = validator.validate(record);
        assertTrue(validator.hasErrors(issues));
        assertTrue(issues.stream().anyMatch(i -> i.contains("245")));
    }

    @Test
    void warnsOnBadIsbn() {
        Record record = codec.toRecord("00000nam a2200000 a 4500", List.of(
                new MarcLine("245", "10", "$aTitle"),
                new MarcLine("020", " ", "$a9780134685992")));  // bad check digit

        assertTrue(validator.validate(record).stream().anyMatch(i -> i.startsWith("WARN") && i.contains("ISBN")));
    }
}
