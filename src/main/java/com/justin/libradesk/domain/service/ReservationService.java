package com.justin.libradesk.domain.service;

import com.justin.libradesk.domain.enumtype.PatronStatus;
import com.justin.libradesk.domain.enumtype.ReservationStatus;
import com.justin.libradesk.domain.model.Patron;
import com.justin.libradesk.domain.model.Reservation;
import com.justin.libradesk.repository.BookRepository;
import com.justin.libradesk.repository.PatronRepository;
import com.justin.libradesk.repository.ReservationRepository;
import com.justin.libradesk.validation.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Manages the reservation (hold) queue for books. Reservations form a FIFO
 * queue per book; when a copy is returned the circulation service asks this
 * service to promote the next waiting patron (see {@link #promoteNext}).
 */
public class ReservationService {

    private static final Logger log = LoggerFactory.getLogger(ReservationService.class);

    private final ReservationRepository reservationRepository;
    private final PatronRepository patronRepository;
    private final BookRepository bookRepository;
    private final AuditLogService auditLogService;
    private final SettingsService settingsService;
    private final Clock clock;

    public ReservationService(ReservationRepository reservationRepository,
                              PatronRepository patronRepository,
                              BookRepository bookRepository,
                              AuditLogService auditLogService,
                              SettingsService settingsService,
                              Clock clock) {
        this.reservationRepository = reservationRepository;
        this.patronRepository = patronRepository;
        this.bookRepository = bookRepository;
        this.auditLogService = auditLogService;
        this.settingsService = settingsService;
        this.clock = clock;
    }

    /**
     * Places a patron at the back of a book's hold queue.
     *
     * @throws ValidationException if the book/patron is missing, the patron is
     *                             not active, or already holds this book
     */
    public Reservation reserve(Long bookId, Long patronId, String actor) {
        bookRepository.findById(bookId)
                .orElseThrow(() -> new ValidationException("Book not found: " + bookId));
        Patron patron = patronRepository.findById(patronId)
                .orElseThrow(() -> new ValidationException("Patron not found: " + patronId));
        if (patron.getStatus() != PatronStatus.ACTIVE) {
            throw new ValidationException("Patron is not active (status: " + patron.getStatus() + ")");
        }
        reservationRepository.findActiveByBookAndPatron(bookId, patronId).ifPresent(existing -> {
            throw new ValidationException("Patron already has an active reservation for this book");
        });

        int nextPosition = reservationRepository.maxQueuePosition(bookId) + 1;
        Reservation reservation = new Reservation(null, bookId, patronId,
                LocalDateTime.now(clock), nextPosition, ReservationStatus.PENDING);
        Reservation saved = reservationRepository.save(reservation);
        auditLogService.record(actor, "RESERVATION_CREATED", "Reservation", saved.getId(),
                "book=" + bookId + " patron=" + patronId + " position=" + nextPosition);
        return saved;
    }

    /**
     * Promotes the head of a book's pending queue to {@link ReservationStatus#READY}.
     * Called when a copy of the book becomes free (on return).
     *
     * @return the promoted reservation, or empty if the queue is empty
     */
    public Optional<Reservation> promoteNext(Long bookId, String actor) {
        Optional<Reservation> next = reservationRepository.findNextPending(bookId);
        next.ifPresent(reservation -> {
            reservation.setStatus(ReservationStatus.READY);
            reservation.setReadyAt(LocalDateTime.now(clock));
            reservationRepository.save(reservation);
            auditLogService.record(actor, "RESERVATION_READY", "Reservation", reservation.getId(),
                    "book=" + bookId + " patron=" + reservation.getPatronId());
            log.info("Reservation {} promoted to READY for book {}", reservation.getId(), bookId);
        });
        return next;
    }

    /** @return true if the book has anyone waiting (PENDING) in its queue. */
    public boolean hasPending(Long bookId) {
        return reservationRepository.findNextPending(bookId).isPresent();
    }

    /**
     * Expires READY holds that have not been collected within
     * {@code reservation.ready.expiry.days}, promoting the next patron for each.
     *
     * @return the number of reservations expired
     */
    public int expireStaleReady(String actor) {
        int expiryDays = settingsService.getInt("reservation.ready.expiry.days", 3);
        LocalDateTime cutoff = LocalDateTime.now(clock).minusDays(expiryDays);
        List<Reservation> stale = reservationRepository.findReadyExpired(cutoff);
        for (Reservation reservation : stale) {
            reservation.setStatus(ReservationStatus.EXPIRED);
            reservationRepository.save(reservation);
            auditLogService.record(actor, "RESERVATION_EXPIRED", "Reservation", reservation.getId(),
                    "book=" + reservation.getBookId());
            promoteNext(reservation.getBookId(), actor);
        }
        if (!stale.isEmpty()) {
            log.info("Expired {} stale READY reservation(s)", stale.size());
        }
        return stale.size();
    }

    /** Cancels a reservation (e.g. the patron no longer wants the hold). */
    public Reservation cancel(Long reservationId, String actor) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ValidationException("Reservation not found: " + reservationId));
        reservation.setStatus(ReservationStatus.CANCELLED);
        Reservation saved = reservationRepository.save(reservation);
        auditLogService.record(actor, "RESERVATION_CANCELLED", "Reservation", reservationId, null);
        return saved;
    }

    public List<Reservation> queueFor(Long bookId) {
        return reservationRepository.findQueueByBook(bookId);
    }

    public List<Reservation> listActive() {
        return reservationRepository.findAllActive();
    }
}
