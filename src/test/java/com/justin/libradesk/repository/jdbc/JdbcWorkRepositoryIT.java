package com.justin.libradesk.repository.jdbc;

import com.justin.libradesk.domain.model.Work;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcWorkRepositoryIT extends AbstractRepositoryIT {

    private JdbcWorkRepository repository;

    @BeforeEach
    void setUp() {
        repository = new JdbcWorkRepository(databaseManager);
    }

    @Test
    void savesAndResolvesByKey() {
        Work saved = repository.save(new Work(null, "clean code|martin", "Clean Code", "Martin"));

        assertTrue(saved.id() != null);
        assertEquals("Clean Code", repository.findByKey("clean code|martin").orElseThrow().title());
        assertEquals(saved.id(), repository.findById(saved.id()).orElseThrow().id());
        assertTrue(repository.findByKey("missing").isEmpty());
    }

    @Test
    void deletesWork() {
        Work saved = repository.save(new Work(null, "k", "T", null));
        repository.deleteById(saved.id());
        assertTrue(repository.findById(saved.id()).isEmpty());
    }
}
