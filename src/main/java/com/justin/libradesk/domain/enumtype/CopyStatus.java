package com.justin.libradesk.domain.enumtype;

/**
 * Lifecycle status of a single physical {@code BookCopy}.
 * Only {@link #AVAILABLE} copies may be loaned out.
 */
public enum CopyStatus {
    AVAILABLE,
    ON_LOAN,
    RESERVED,
    LOST,
    DAMAGED
}
