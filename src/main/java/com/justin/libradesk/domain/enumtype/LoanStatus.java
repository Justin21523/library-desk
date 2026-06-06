package com.justin.libradesk.domain.enumtype;

/**
 * Status of a loan. {@link #OVERDUE} is derived from the due date while the
 * loan is still {@link #ACTIVE}; it is persisted once detected.
 */
public enum LoanStatus {
    ACTIVE,
    RETURNED,
    OVERDUE
}
