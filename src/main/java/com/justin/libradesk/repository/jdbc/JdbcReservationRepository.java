package com.justin.libradesk.repository.jdbc;

import com.justin.libradesk.domain.enumtype.ReservationStatus;
import com.justin.libradesk.domain.model.Reservation;
import com.justin.libradesk.infrastructure.database.DatabaseManager;
import com.justin.libradesk.repository.DataAccessException;
import com.justin.libradesk.repository.ReservationRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcReservationRepository implements ReservationRepository {

    private final DatabaseManager db;

    public JdbcReservationRepository(DatabaseManager db) {
        this.db = db;
    }

    @Override
    public Reservation save(Reservation reservation) {
        return reservation.getId() == null ? insert(reservation) : update(reservation);
    }

    private Reservation insert(Reservation reservation) {
        String sql = """
                INSERT INTO reservations (book_id, patron_id, reserved_at, queue_position, status)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, reservation.getBookId());
            ps.setLong(2, reservation.getPatronId());
            ps.setTimestamp(3, Timestamp.valueOf(reservation.getReservedAt()));
            ps.setInt(4, reservation.getQueuePosition());
            ps.setString(5, reservation.getStatus().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    reservation.setId(keys.getLong("id"));
                }
            }
            return reservation;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to insert reservation for book " + reservation.getBookId(), e);
        }
    }

    private Reservation update(Reservation reservation) {
        String sql = """
                UPDATE reservations
                   SET book_id = ?, patron_id = ?, reserved_at = ?, queue_position = ?, status = ?
                 WHERE id = ?
                """;
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, reservation.getBookId());
            ps.setLong(2, reservation.getPatronId());
            ps.setTimestamp(3, Timestamp.valueOf(reservation.getReservedAt()));
            ps.setInt(4, reservation.getQueuePosition());
            ps.setString(5, reservation.getStatus().name());
            ps.setLong(6, reservation.getId());
            ps.executeUpdate();
            return reservation;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update reservation id=" + reservation.getId(), e);
        }
    }

    @Override
    public Optional<Reservation> findById(Long id) {
        return queryOne("SELECT * FROM reservations WHERE id = ?", ps -> ps.setLong(1, id));
    }

    @Override
    public List<Reservation> findQueueByBook(Long bookId) {
        return queryList(
                "SELECT * FROM reservations WHERE book_id = ? AND status = 'PENDING' ORDER BY queue_position",
                ps -> ps.setLong(1, bookId));
    }

    @Override
    public Optional<Reservation> findNextPending(Long bookId) {
        return queryOne(
                "SELECT * FROM reservations WHERE book_id = ? AND status = 'PENDING' "
                        + "ORDER BY queue_position LIMIT 1",
                ps -> ps.setLong(1, bookId));
    }

    @Override
    public Optional<Reservation> findActiveByBookAndPatron(Long bookId, Long patronId) {
        return queryOne(
                "SELECT * FROM reservations WHERE book_id = ? AND patron_id = ? "
                        + "AND status IN ('PENDING', 'READY') LIMIT 1",
                ps -> {
                    ps.setLong(1, bookId);
                    ps.setLong(2, patronId);
                });
    }

    @Override
    public List<Reservation> findAllActive() {
        return queryList(
                "SELECT * FROM reservations WHERE status IN ('PENDING', 'READY') "
                        + "ORDER BY book_id, queue_position",
                ps -> { });
    }

    @Override
    public int maxQueuePosition(Long bookId) {
        String sql = "SELECT COALESCE(MAX(queue_position), 0) FROM reservations "
                + "WHERE book_id = ? AND status IN ('PENDING', 'READY')";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to read max queue position for book " + bookId, e);
        }
    }

    @Override
    public List<Reservation> findAll() {
        return queryList("SELECT * FROM reservations ORDER BY reserved_at DESC", ps -> { });
    }

    @Override
    public void deleteById(Long id) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM reservations WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete reservation id=" + id, e);
        }
    }

    private Optional<Reservation> queryOne(String sql, StatementBinder binder) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query reservation", e);
        }
    }

    private List<Reservation> queryList(String sql, StatementBinder binder) {
        List<Reservation> reservations = new ArrayList<>();
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapRow(rs));
                }
            }
            return reservations;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to list reservations", e);
        }
    }

    private Reservation mapRow(ResultSet rs) throws SQLException {
        return new Reservation(
                rs.getLong("id"),
                rs.getLong("book_id"),
                rs.getLong("patron_id"),
                rs.getTimestamp("reserved_at").toLocalDateTime(),
                rs.getInt("queue_position"),
                ReservationStatus.valueOf(rs.getString("status")));
    }

    @FunctionalInterface
    private interface StatementBinder {
        void bind(PreparedStatement ps) throws SQLException;
    }
}
