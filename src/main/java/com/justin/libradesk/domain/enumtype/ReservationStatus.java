package com.justin.libradesk.domain.enumtype;

/**
 * Status of a reservation within a book's hold queue.
 */
public enum ReservationStatus {
    PENDING,
    READY,
    FULFILLED,
    CANCELLED,
    EXPIRED
}
