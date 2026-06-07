package com.justin.libradesk.repository.jdbc;

import com.justin.libradesk.domain.model.Book;
import com.justin.libradesk.domain.model.Holding;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcHoldingRepositoryIT extends AbstractRepositoryIT {

    private JdbcHoldingRepository repository;
    private Long bookId;

    @BeforeEach
    void setUp() {
        repository = new JdbcHoldingRepository(databaseManager);
        bookId = new JdbcBookRepository(databaseManager)
                .save(new Book(null, "isbn", "Host", null, null, null, FIXED)).getId();
    }

    @Test
    void savesAndListsByBook() {
        Holding saved = repository.save(new Holding(null, bookId, null, "QA76", "v.1-3", "note"));

        assertTrue(saved.id() != null);
        assertEquals(1, repository.findByBook(bookId).size());
        assertEquals("QA76", repository.findById(saved.id()).orElseThrow().callNumber());
    }

    @Test
    void deletesHolding() {
        Holding saved = repository.save(new Holding(null, bookId, null, "QA76", null, null));
        repository.deleteById(saved.id());
        assertTrue(repository.findByBook(bookId).isEmpty());
    }
}
