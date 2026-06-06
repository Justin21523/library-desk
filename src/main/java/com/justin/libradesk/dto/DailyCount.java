package com.justin.libradesk.dto;

import java.time.LocalDate;

/** A date/count pair for time-series report aggregates (e.g. loans per day). */
public record DailyCount(LocalDate date, long count) {
}
