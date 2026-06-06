package com.justin.libradesk.domain.service;

import com.justin.libradesk.domain.enumtype.ReservationStatus;
import com.justin.libradesk.domain.model.Reservation;
import com.justin.libradesk.repository.ReservationRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Manages the reservation (hold) queue for books whose copies are unavailable.
 *
 * <p>Phase 1 skeleton: the queueing logic is expressed against the repository
 * interface; it becomes functional once {@code JdbcReservationRepository} is
 * implemented in Phase 2.
 */
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final AuditLogService auditLogService;
    private final Clock clock;

    public ReservationService(ReservationRepository reservationRepository,
                              AuditLogService auditLogService,
                              Clock clock) {
        this.reservationRepository = reservationRepository;
        this.auditLogService = auditLogService;
        this.clock = clock;
    }

    /**
     * Places a patron at the back of a book's hold queue.
     *
     * @return the created reservation with its assigned queue position
     */
    public Reservation reserve(Long bookId, Long patronId, String actor) {
        int nextPosition = reservationRepository.maxQueuePosition(bookId) + 1;
        Reservation reservation = new Reservation(null, bookId, patronId,
                LocalDateTime.now(clock), nextPosition, ReservationStatus.PENDING);
        Reservation saved = reservationRepository.save(reservation);
        auditLogService.record(actor, "RESERVATION_CREATED", "Reservation", saved.getId(),
                "book=" + bookId + " patron=" + patronId + " position=" + nextPosition);
        return saved;
    }

    public List<Reservation> queueFor(Long bookId) {
        return reservationRepository.findQueueByBook(bookId);
    }

    // TODO(phase2): promote next reservation to READY on return; cancel/expire holds.
}
