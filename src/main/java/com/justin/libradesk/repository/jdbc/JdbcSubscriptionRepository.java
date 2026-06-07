package com.justin.libradesk.repository.jdbc;

import com.justin.libradesk.domain.enumtype.Frequency;
import com.justin.libradesk.domain.enumtype.SubscriptionStatus;
import com.justin.libradesk.domain.model.Subscription;
import com.justin.libradesk.infrastructure.database.DatabaseManager;
import com.justin.libradesk.repository.DataAccessException;
import com.justin.libradesk.repository.SubscriptionRepository;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcSubscriptionRepository implements SubscriptionRepository {

    private final DatabaseManager db;

    public JdbcSubscriptionRepository(DatabaseManager db) {
        this.db = db;
    }

    @Override
    public Subscription save(Subscription s) {
        return s.id() == null ? insert(s) : update(s);
    }

    private Subscription insert(Subscription s) {
        String sql = """
                INSERT INTO subscriptions (book_id, label, frequency, status, start_date, next_expected)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, s);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return new Subscription(keys.getLong("id"), s.bookId(), s.label(), s.frequency(),
                        s.status(), s.startDate(), s.nextExpected());
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to insert subscription: " + s.label(), e);
        }
    }

    private Subscription update(Subscription s) {
        String sql = """
                UPDATE subscriptions SET book_id = ?, label = ?, frequency = ?, status = ?,
                       start_date = ?, next_expected = ?
                 WHERE id = ?
                """;
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, s);
            ps.setLong(7, s.id());
            ps.executeUpdate();
            return s;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update subscription id=" + s.id(), e);
        }
    }

    private void bind(PreparedStatement ps, Subscription s) throws SQLException {
        ps.setLong(1, s.bookId());
        ps.setString(2, s.label());
        ps.setString(3, s.frequency().name());
        ps.setString(4, s.status().name());
        ps.setDate(5, Date.valueOf(s.startDate()));
        if (s.nextExpected() != null) {
            ps.setDate(6, Date.valueOf(s.nextExpected()));
        } else {
            ps.setNull(6, Types.DATE);
        }
    }

    @Override
    public List<Subscription> findActive() {
        return query("SELECT * FROM subscriptions WHERE status = 'ACTIVE' ORDER BY label", ps -> { });
    }

    @Override
    public Optional<Subscription> findById(Long id) {
        return query("SELECT * FROM subscriptions WHERE id = ?", ps -> ps.setLong(1, id))
                .stream().findFirst();
    }

    @Override
    public List<Subscription> findAll() {
        return query("SELECT * FROM subscriptions ORDER BY label", ps -> { });
    }

    @Override
    public void deleteById(Long id) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM subscriptions WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete subscription id=" + id, e);
        }
    }

    private List<Subscription> query(String sql, StatementBinder binder) {
        List<Subscription> subs = new ArrayList<>();
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Date next = rs.getDate("next_expected");
                    subs.add(new Subscription(
                            rs.getLong("id"),
                            rs.getLong("book_id"),
                            rs.getString("label"),
                            Frequency.valueOf(rs.getString("frequency")),
                            SubscriptionStatus.valueOf(rs.getString("status")),
                            rs.getDate("start_date").toLocalDate(),
                            next != null ? next.toLocalDate() : null));
                }
            }
            return subs;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query subscriptions", e);
        }
    }

    @FunctionalInterface
    private interface StatementBinder {
        void bind(PreparedStatement ps) throws SQLException;
    }
}
