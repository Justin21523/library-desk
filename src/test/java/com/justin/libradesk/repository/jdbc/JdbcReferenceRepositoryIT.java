package com.justin.libradesk.repository.jdbc;

import com.justin.libradesk.domain.model.Author;
import com.justin.libradesk.domain.model.Category;
import com.justin.libradesk.domain.model.Publisher;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcReferenceRepositoryIT extends AbstractRepositoryIT {

    @Test
    void authorRoundTripUpdateAndDelete() {
        JdbcAuthorRepository repository = new JdbcAuthorRepository(databaseManager);

        Author saved = repository.save(new Author(null, "Robert Martin"));
        assertTrue(saved.id() != null);
        assertEquals("Robert Martin", repository.findById(saved.id()).orElseThrow().name());

        repository.save(new Author(saved.id(), "Uncle Bob"));
        assertEquals("Uncle Bob", repository.findById(saved.id()).orElseThrow().name());

        repository.deleteById(saved.id());
        assertTrue(repository.findById(saved.id()).isEmpty());
    }

    @Test
    void publisherRoundTrip() {
        JdbcPublisherRepository repository = new JdbcPublisherRepository(databaseManager);

        Publisher saved = repository.save(new Publisher(null, "O'Reilly"));
        assertEquals(1, repository.findAll().size());
        assertEquals("O'Reilly", repository.findById(saved.id()).orElseThrow().name());
    }

    @Test
    void categoryRoundTrip() {
        JdbcCategoryRepository repository = new JdbcCategoryRepository(databaseManager);

        Category saved = repository.save(new Category(null, "Software"));
        assertEquals(1, repository.findAll().size());
        assertEquals("Software", repository.findById(saved.id()).orElseThrow().name());
    }
}
