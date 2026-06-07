package com.justin.libradesk.repository.jdbc;

import com.justin.libradesk.domain.model.Book;
import com.justin.libradesk.domain.model.ELink;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class JdbcELinkRepositoryIT extends AbstractRepositoryIT {

    private JdbcELinkRepository repository;
    private Long bookId;

    @BeforeEach
    void setUp() {
        repository = new JdbcELinkRepository(databaseManager);
        bookId = new JdbcBookRepository(databaseManager)
                .save(new Book(null, "isbn", "Linked", null, null, null, FIXED)).getId();
    }

    @Test
    void savesWithNullStatusThenUpdatesAfterCheck() {
        ELink saved = repository.save(new ELink(null, bookId, "https://example.com", "Site", null, null));
        assertNull(repository.findById(saved.id()).orElseThrow().lastStatus());

        repository.save(new ELink(saved.id(), bookId, "https://example.com", "Site", 200, FIXED));

        ELink checked = repository.findByBook(bookId).get(0);
        assertEquals(200, checked.lastStatus());
        assertEquals(FIXED, checked.lastChecked());
    }
}
