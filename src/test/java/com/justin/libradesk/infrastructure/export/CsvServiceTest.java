package com.justin.libradesk.infrastructure.export;

import com.justin.libradesk.domain.enumtype.PatronStatus;
import com.justin.libradesk.domain.enumtype.PatronType;
import com.justin.libradesk.domain.model.Book;
import com.justin.libradesk.domain.model.Patron;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CsvServiceTest {

    private final CsvService csvService = new CsvService();

    @Test
    void booksRoundTripThroughCsv(@TempDir Path dir) {
        File file = dir.resolve("books.csv").toFile();
        Book a = new Book(1L, "978-1", "Clean Code", null, null, 2008, LocalDateTime.now());
        Book b = new Book(2L, null, "No ISBN", null, null, null, LocalDateTime.now());

        csvService.writeBooks(file, List.of(a, b));
        List<Book> read = csvService.readBooks(file);

        assertEquals(2, read.size());
        assertEquals("Clean Code", read.get(0).getTitle());
        assertEquals("978-1", read.get(0).getIsbn());
        assertEquals(2008, read.get(0).getPublishedYear());
        assertNull(read.get(1).getIsbn());
        assertNull(read.get(1).getPublishedYear());
    }

    @Test
    void patronsRoundTripAndDefaultToActive(@TempDir Path dir) {
        File file = dir.resolve("patrons.csv").toFile();
        Patron p = new Patron(1L, "M1", "Alice", "a@x.com", "555",
                PatronType.STAFF, PatronStatus.SUSPENDED, LocalDateTime.now());

        csvService.writePatrons(file, List.of(p));
        List<Patron> read = csvService.readPatrons(file);

        assertEquals(1, read.size());
        assertEquals("M1", read.get(0).getMembershipNo());
        assertEquals(PatronType.STAFF, read.get(0).getPatronType());
        // Imported patrons always start ACTIVE regardless of the source row.
        assertEquals(PatronStatus.ACTIVE, read.get(0).getStatus());
    }

    @Test
    void writesAHeaderRow(@TempDir Path dir) throws IOException {
        File file = dir.resolve("books.csv").toFile();
        csvService.writeBooks(file, List.of());

        assertEquals("isbn,title,published_year", Files.readAllLines(file.toPath()).get(0));
    }
}
