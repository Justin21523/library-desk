package com.justin.libradesk.repository;

import com.justin.libradesk.domain.model.Reservation;

import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends Repository<Reservation, Long> {

    /** @return pending reservations for a book ordered by queue position. */
    List<Reservation> findQueueByBook(Long bookId);

    /** @return the head of a book's pending queue (lowest position), if any. */
    Optional<Reservation> findNextPending(Long bookId);

    /** @return an existing PENDING/READY reservation for this patron on this book, if any. */
    Optional<Reservation> findActiveByBookAndPatron(Long bookId, Long patronId);

    /** @return all PENDING/READY reservations across all books, for display. */
    List<Reservation> findAllActive();

    /** @return a patron's PENDING/READY reservations (their active holds). */
    List<Reservation> findActiveByPatron(Long patronId);

    /** @return READY reservations whose ready_at is before the cutoff (expired holds). */
    List<Reservation> findReadyExpired(java.time.LocalDateTime cutoff);

    /** @return the highest queue position currently used (PENDING/READY) for a book, or 0 if none. */
    int maxQueuePosition(Long bookId);
}
