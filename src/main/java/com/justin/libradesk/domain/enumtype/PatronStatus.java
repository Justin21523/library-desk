package com.justin.libradesk.domain.enumtype;

/**
 * Account status of a patron. Only {@link #ACTIVE} patrons may borrow.
 */
public enum PatronStatus {
    ACTIVE,
    SUSPENDED,
    EXPIRED
}
