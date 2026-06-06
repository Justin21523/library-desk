package com.justin.libradesk.repository.jdbc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcSettingsRepositoryIT extends AbstractRepositoryIT {

    private JdbcSettingsRepository repository;

    @BeforeEach
    void setUp() {
        repository = new JdbcSettingsRepository(databaseManager);
    }

    @Test
    void putInsertsThenUpdatesOnConflict() {
        repository.put("loan.period.days", "14");
        assertEquals("14", repository.find("loan.period.days").orElseThrow());

        repository.put("loan.period.days", "21");
        assertEquals("21", repository.find("loan.period.days").orElseThrow());
    }

    @Test
    void findMissingKeyReturnsEmpty() {
        assertTrue(repository.find("nope").isEmpty());
    }

    @Test
    void findAllReturnsEveryStoredSetting() {
        repository.put("a", "1");
        repository.put("b", "2");

        assertEquals(2, repository.findAll().size());
    }
}
