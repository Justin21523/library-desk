package com.justin.libradesk.infrastructure.marc;

import com.justin.libradesk.domain.enumtype.MaterialType;
import com.justin.libradesk.domain.model.Book;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.marc4j.marc.Record;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarcServiceTest {

    private final MarcService marcService = new MarcService();

    private MarcData sample() {
        Book book = new Book(null, "9780134685991", "Effective Java", null, null, 2018, LocalDateTime.now());
        book.setEdition("3rd ed.");
        book.setPubPlace("Boston");
        book.setExtent("xvi, 412 pages");
        return new MarcData(book, List.of("Bloch, Joshua"), List.of("Java (Computer program language)"),
                "Addison-Wesley");
    }

    @Test
    void mapsAcommonFieldsToXmlAndBack() {
        String xml = marcService.toXmlString(sample());

        MarcData parsed = marcService.fromXmlString(xml);
        Book book = parsed.book();
        assertEquals("Effective Java", book.getTitle());
        assertEquals("9780134685991", book.getIsbn());
        assertEquals("3rd ed.", book.getEdition());
        assertEquals(2018, book.getPublishedYear());
        assertEquals("Addison-Wesley", parsed.publisherName());
        assertTrue(parsed.authorNames().contains("Bloch, Joshua"));
        assertTrue(parsed.subjectTerms().contains("Java (Computer program language)"));
    }

    @Test
    void roundTripsThroughBinaryMrcFile(@TempDir Path dir) {
        File file = dir.resolve("out.mrc").toFile();
        marcService.writeMrc(file, List.of(sample()));

        List<MarcData> read = marcService.read(file);
        assertEquals(1, read.size());
        assertEquals("Effective Java", read.get(0).book().getTitle());
        // The original record is retained for fidelity.
        assertTrue(read.get(0).book().getMarcXml().contains("Effective Java"));
    }

    @Test
    void derivesMaterialTypeFromLeader() {
        MarcLineCodec codec = new MarcLineCodec();
        Record serial = codec.toRecord("00000nas a2200000 a 4500",
                List.of(new MarcLine("245", "00", "$aA Journal")));

        MarcData data = marcService.fromXmlString(marcService.toXml(serial));

        assertEquals(MaterialType.SERIAL, data.book().getMaterialType());
    }

    @Test
    void roundTripsThroughMarcXmlFile(@TempDir Path dir) {
        File file = dir.resolve("out.xml").toFile();
        marcService.writeMarcXml(file, List.of(sample()));

        List<MarcData> read = marcService.read(file);
        assertEquals(1, read.size());
        assertEquals("9780134685991", read.get(0).book().getIsbn());
    }
}
