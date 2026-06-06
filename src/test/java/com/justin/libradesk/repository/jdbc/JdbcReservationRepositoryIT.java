package com.justin.libradesk.repository.jdbc;

import com.justin.libradesk.domain.enumtype.PatronStatus;
import com.justin.libradesk.domain.enumtype.PatronType;
import com.justin.libradesk.domain.enumtype.ReservationStatus;
import com.justin.libradesk.domain.model.Book;
import com.justin.libradesk.domain.model.Patron;
import com.justin.libradesk.domain.model.Reservation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcReservationRepositoryIT extends AbstractRepositoryIT {

    private JdbcReservationRepository repository;
    private Long bookId;
    private Long patronA;
    private Long patronB;

    @BeforeEach
    void setUp() {
        repository = new JdbcReservationRepository(databaseManager);
        bookId = new JdbcBookRepository(databaseManager)
                .save(new Book(null, "isbn", "Wanted Book", null, null, null, FIXED)).getId();
        JdbcPatronRepository patrons = new JdbcPatronRepository(databaseManager);
        patronA = patrons.save(new Patron(null, "A", "Alice", null, null,
                PatronType.STUDENT, PatronStatus.ACTIVE, FIXED)).getId();
        patronB = patrons.save(new Patron(null, "B", "Bob", null, null,
                PatronType.STUDENT, PatronStatus.ACTIVE, FIXED)).getId();
    }

    private Reservation pending(Long patronId, int position) {
        return new Reservation(null, bookId, patronId, FIXED, position, ReservationStatus.PENDING);
    }

    @Test
    void queueIsOrderedByPositionAndNextPendingIsHead() {
        repository.save(pending(patronB, 2));
        repository.save(pending(patronA, 1));

        var queue = repository.findQueueByBook(bookId);
        assertEquals(2, queue.size());
        assertEquals(patronA, queue.get(0).getPatronId());

        assertEquals(patronA, repository.findNextPending(bookId).orElseThrow().getPatronId());
    }

    @Test
    void maxQueuePositionConsidersActiveOnly() {
        repository.save(pending(patronA, 1));
        repository.save(pending(patronB, 2));

        assertEquals(2, repository.maxQueuePosition(bookId));
    }

    @Test
    void findActiveByBookAndPatronMatchesPendingAndReady() {
        repository.save(pending(patronA, 1));

        assertTrue(repository.findActiveByBookAndPatron(bookId, patronA).isPresent());
        assertTrue(repository.findActiveByBookAndPatron(bookId, patronB).isEmpty());
    }

    @Test
    void cancelledReservationsAreExcludedFromActiveQueries() {
        Reservation saved = repository.save(pending(patronA, 1));
        saved.setStatus(ReservationStatus.CANCELLED);
        repository.save(saved);

        assertEquals(0, repository.maxQueuePosition(bookId));
        assertTrue(repository.findNextPending(bookId).isEmpty());
        assertTrue(repository.findAllActive().isEmpty());
    }
}
