package com.justin.libradesk.domain.enumtype;

/**
 * Status of a single serial issue: expected but not yet arrived, received,
 * overdue ({@link #LATE}), or claimed from the supplier.
 */
public enum IssueStatus {
    EXPECTED,
    RECEIVED,
    LATE,
    CLAIMED
}
