package com.justin.libradesk.repository;

import com.justin.libradesk.domain.model.Reservation;

import java.util.List;

public interface ReservationRepository extends Repository<Reservation, Long> {

    /** @return pending reservations for a book ordered by queue position. */
    List<Reservation> findQueueByBook(Long bookId);

    /** @return the highest queue position currently used for a book, or 0 if none. */
    int maxQueuePosition(Long bookId);
}
