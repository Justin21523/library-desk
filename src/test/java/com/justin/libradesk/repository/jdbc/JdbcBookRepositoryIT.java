package com.justin.libradesk.repository.jdbc;

import com.justin.libradesk.domain.model.Book;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcBookRepositoryIT extends AbstractRepositoryIT {

    private JdbcBookRepository repository;

    @BeforeEach
    void setUp() {
        repository = new JdbcBookRepository(databaseManager);
    }

    private Book newBook(String isbn, String title) {
        return new Book(null, isbn, title, null, null, null, FIXED);
    }

    @Test
    void savesAndReadsBackWithNullOptionalFields() {
        Book saved = repository.save(newBook("978-1", "Clean Architecture"));

        assertTrue(saved.getId() != null);
        Optional<Book> found = repository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("Clean Architecture", found.get().getTitle());
        assertEquals("978-1", found.get().getIsbn());
        assertNull(found.get().getPublisherId());
        assertNull(found.get().getCategoryId());
        assertNull(found.get().getPublishedYear());
        assertTrue(found.get().getAuthorIds().isEmpty());
    }

    @Test
    void persistsScalarFieldsWhenPresent() {
        Book book = new Book(null, "978-2", "Effective Java", null, null, 2018, FIXED);

        Book saved = repository.save(book);

        Book found = repository.findById(saved.getId()).orElseThrow();
        assertEquals(2018, found.getPublishedYear());
    }

    @Test
    void syncsAuthorLinksOnSaveAndUpdate() throws SQLException {
        long author1 = insertAuthor("Robert Martin");
        long author2 = insertAuthor("James Gosling");

        Book book = newBook("978-3", "Multi-author");
        book.getAuthorIds().add(author1);
        book.getAuthorIds().add(author2);
        Book saved = repository.save(book);

        List<Long> reloaded = repository.findById(saved.getId()).orElseThrow().getAuthorIds();
        assertEquals(List.of(author1, author2), reloaded);

        // Updating the author set replaces (not appends to) the links.
        saved.getAuthorIds().clear();
        saved.getAuthorIds().add(author2);
        repository.save(saved);

        assertEquals(List.of(author2),
                repository.findById(saved.getId()).orElseThrow().getAuthorIds());
    }

    @Test
    void findByIsbnAndSearchByTitle() {
        repository.save(newBook("111", "Domain Driven Design"));
        repository.save(newBook("222", "Driven by Tests"));

        assertTrue(repository.findByIsbn("111").isPresent());
        assertTrue(repository.findByIsbn("999").isEmpty());
        // ILIKE is case-insensitive and matches a fragment.
        assertEquals(2, repository.searchByTitle("driven").size());
        assertEquals(1, repository.searchByTitle("Design").size());
    }

    @Test
    void deleteRemovesBook() {
        Book saved = repository.save(newBook("333", "Throwaway"));

        repository.deleteById(saved.getId());

        assertTrue(repository.findById(saved.getId()).isEmpty());
    }

    @Test
    void persistsMarcFieldsAndSubjectLinks() {
        long subjectId = insertSubject("Java (Computer program language)");
        Book book = newBook("978-9", "Effective Java");
        book.setEdition("3rd ed.");
        book.setExtent("xvi, 412 pages");
        book.setLanguage("eng");
        book.setControlNumber("ocm12345");
        book.getSubjectIds().add(subjectId);

        Book saved = repository.save(book);

        Book found = repository.findById(saved.getId()).orElseThrow();
        assertEquals("3rd ed.", found.getEdition());
        assertEquals("eng", found.getLanguage());
        assertEquals("ocm12345", found.getControlNumber());
        assertEquals(List.of(subjectId), found.getSubjectIds());
    }

    private long insertSubject(String term) {
        try (var c = databaseManager.getConnection();
             var ps = c.prepareStatement(
                     "INSERT INTO subjects (term) VALUES (?)", java.sql.Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, term);
            ps.executeUpdate();
            try (var keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong("id");
            }
        } catch (java.sql.SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
