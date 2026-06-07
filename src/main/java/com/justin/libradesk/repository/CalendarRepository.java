package com.justin.libradesk.repository;

import com.justin.libradesk.domain.model.CalendarDay;

import java.time.LocalDate;
import java.util.List;

public interface CalendarRepository {

    /** @return {@code true} if the library is closed on the given date. */
    boolean isClosed(LocalDate date);

    /** @return closed days within the inclusive range, ordered by date. */
    List<CalendarDay> findRange(LocalDate from, LocalDate to);

    List<CalendarDay> findAll();

    /** Adds (or updates the note of) a closed date. */
    void add(CalendarDay day);

    void remove(LocalDate date);
}
