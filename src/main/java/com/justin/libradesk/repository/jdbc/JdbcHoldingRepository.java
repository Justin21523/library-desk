package com.justin.libradesk.repository.jdbc;

import com.justin.libradesk.domain.model.Holding;
import com.justin.libradesk.infrastructure.database.DatabaseManager;
import com.justin.libradesk.repository.DataAccessException;
import com.justin.libradesk.repository.HoldingRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcHoldingRepository implements HoldingRepository {

    private final DatabaseManager db;

    public JdbcHoldingRepository(DatabaseManager db) {
        this.db = db;
    }

    @Override
    public Holding save(Holding holding) {
        return holding.id() == null ? insert(holding) : update(holding);
    }

    private Holding insert(Holding h) {
        String sql = """
                INSERT INTO holdings (book_id, location_id, call_number, summary, note)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, h);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return new Holding(keys.getLong("id"), h.bookId(), h.locationId(),
                        h.callNumber(), h.summary(), h.note());
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to insert holding for book " + h.bookId(), e);
        }
    }

    private Holding update(Holding h) {
        String sql = """
                UPDATE holdings SET book_id = ?, location_id = ?, call_number = ?, summary = ?, note = ?
                 WHERE id = ?
                """;
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, h);
            ps.setLong(6, h.id());
            ps.executeUpdate();
            return h;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update holding id=" + h.id(), e);
        }
    }

    private void bind(PreparedStatement ps, Holding h) throws SQLException {
        ps.setLong(1, h.bookId());
        if (h.locationId() != null) {
            ps.setLong(2, h.locationId());
        } else {
            ps.setNull(2, Types.BIGINT);
        }
        ps.setString(3, h.callNumber());
        ps.setString(4, h.summary());
        ps.setString(5, h.note());
    }

    @Override
    public List<Holding> findByBook(Long bookId) {
        return query("SELECT * FROM holdings WHERE book_id = ? ORDER BY id", ps -> ps.setLong(1, bookId));
    }

    @Override
    public Optional<Holding> findById(Long id) {
        return query("SELECT * FROM holdings WHERE id = ?", ps -> ps.setLong(1, id)).stream().findFirst();
    }

    @Override
    public List<Holding> findAll() {
        return query("SELECT * FROM holdings ORDER BY id", ps -> { });
    }

    @Override
    public void deleteById(Long id) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM holdings WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete holding id=" + id, e);
        }
    }

    private List<Holding> query(String sql, StatementBinder binder) {
        List<Holding> holdings = new ArrayList<>();
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    holdings.add(new Holding(
                            rs.getLong("id"),
                            rs.getLong("book_id"),
                            rs.getObject("location_id", Long.class),
                            rs.getString("call_number"),
                            rs.getString("summary"),
                            rs.getString("note")));
                }
            }
            return holdings;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query holdings", e);
        }
    }

    @FunctionalInterface
    private interface StatementBinder {
        void bind(PreparedStatement ps) throws SQLException;
    }
}
