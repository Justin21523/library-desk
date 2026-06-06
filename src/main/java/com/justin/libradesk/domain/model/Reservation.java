package com.justin.libradesk.domain.model;

import com.justin.libradesk.domain.enumtype.ReservationStatus;

import java.time.LocalDateTime;

/**
 * A hold placed by a {@link Patron} on a {@link Book} whose copies are
 * currently unavailable. Reservations form a FIFO queue per book.
 */
public class Reservation {

    private Long id;
    private Long bookId;
    private Long patronId;
    private LocalDateTime reservedAt;
    private LocalDateTime readyAt;
    private int queuePosition;
    private ReservationStatus status;

    public Reservation() {
    }

    public Reservation(Long id, Long bookId, Long patronId, LocalDateTime reservedAt,
                       int queuePosition, ReservationStatus status) {
        this.id = id;
        this.bookId = bookId;
        this.patronId = patronId;
        this.reservedAt = reservedAt;
        this.queuePosition = queuePosition;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBookId() {
        return bookId;
    }

    public void setBookId(Long bookId) {
        this.bookId = bookId;
    }

    public Long getPatronId() {
        return patronId;
    }

    public void setPatronId(Long patronId) {
        this.patronId = patronId;
    }

    public LocalDateTime getReservedAt() {
        return reservedAt;
    }

    public void setReservedAt(LocalDateTime reservedAt) {
        this.reservedAt = reservedAt;
    }

    public LocalDateTime getReadyAt() {
        return readyAt;
    }

    public void setReadyAt(LocalDateTime readyAt) {
        this.readyAt = readyAt;
    }

    public int getQueuePosition() {
        return queuePosition;
    }

    public void setQueuePosition(int queuePosition) {
        this.queuePosition = queuePosition;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }
}
