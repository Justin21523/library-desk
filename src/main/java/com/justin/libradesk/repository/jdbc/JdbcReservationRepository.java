package com.justin.libradesk.repository.jdbc;

import com.justin.libradesk.domain.model.Reservation;
import com.justin.libradesk.infrastructure.database.DatabaseManager;
import com.justin.libradesk.repository.ReservationRepository;

import java.util.List;
import java.util.Optional;

/**
 * Phase 1 skeleton.
 *
 * TODO(phase2): implement CRUD plus the queue helpers used by the reservation
 * workflow.
 */
public class JdbcReservationRepository implements ReservationRepository {

    private final DatabaseManager db;

    public JdbcReservationRepository(DatabaseManager db) {
        this.db = db;
    }

    @Override
    public Reservation save(Reservation reservation) {
        throw new UnsupportedOperationException("JdbcReservationRepository.save not implemented in Phase 1");
    }

    @Override
    public Optional<Reservation> findById(Long id) {
        return Optional.empty();
    }

    @Override
    public List<Reservation> findQueueByBook(Long bookId) {
        return List.of();
    }

    @Override
    public int maxQueuePosition(Long bookId) {
        return 0;
    }

    @Override
    public List<Reservation> findAll() {
        return List.of();
    }

    @Override
    public void deleteById(Long id) {
        throw new UnsupportedOperationException("JdbcReservationRepository.deleteById not implemented in Phase 1");
    }
}
