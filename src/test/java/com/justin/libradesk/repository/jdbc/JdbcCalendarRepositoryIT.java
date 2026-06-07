package com.justin.libradesk.repository.jdbc;

import com.justin.libradesk.domain.model.CalendarDay;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcCalendarRepositoryIT extends AbstractRepositoryIT {

    private JdbcCalendarRepository repository;

    @BeforeEach
    void setUp() {
        repository = new JdbcCalendarRepository(databaseManager);
    }

    @Test
    void addedDayIsClosedAndListedInRange() {
        LocalDate day = LocalDate.of(2026, 7, 4);
        repository.add(new CalendarDay(day, "Holiday"));

        assertTrue(repository.isClosed(day));
        assertFalse(repository.isClosed(day.plusDays(1)));
        assertEquals(1, repository.findRange(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31)).size());
    }

    @Test
    void addUpdatesNoteOnConflict() {
        LocalDate day = LocalDate.of(2026, 12, 25);
        repository.add(new CalendarDay(day, "Closed"));
        repository.add(new CalendarDay(day, "Christmas"));

        assertEquals("Christmas", repository.findAll().get(0).note());
    }

    @Test
    void removeDeletesClosedDay() {
        LocalDate day = LocalDate.of(2026, 1, 1);
        repository.add(new CalendarDay(day, "New Year"));
        repository.remove(day);

        assertFalse(repository.isClosed(day));
    }
}
