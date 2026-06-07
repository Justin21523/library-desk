package com.justin.libradesk.domain.model;

import java.time.LocalDate;

/**
 * A date the library is closed. Due dates roll off these days and overdue fines
 * do not accrue on them. The date itself is the primary key.
 */
public record CalendarDay(LocalDate date, String note) {
}
