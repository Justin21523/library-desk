package com.justin.libradesk.domain.enumtype;

/**
 * Publication frequency of a serial subscription. Drives the predicted date of
 * the next expected issue.
 */
public enum Frequency {
    WEEKLY(7),
    MONTHLY(30),
    QUARTERLY(91),
    ANNUAL(365);

    private final int days;

    Frequency(int days) {
        this.days = days;
    }

    /** @return the approximate number of days between issues. */
    public int days() {
        return days;
    }
}
