package com.justin.libradesk.repository.jdbc;

import com.justin.libradesk.domain.enumtype.IssueStatus;
import com.justin.libradesk.domain.model.SerialIssue;
import com.justin.libradesk.infrastructure.database.DatabaseManager;
import com.justin.libradesk.repository.DataAccessException;
import com.justin.libradesk.repository.SerialIssueRepository;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcSerialIssueRepository implements SerialIssueRepository {

    private final DatabaseManager db;

    public JdbcSerialIssueRepository(DatabaseManager db) {
        this.db = db;
    }

    @Override
    public SerialIssue save(SerialIssue i) {
        return i.id() == null ? insert(i) : update(i);
    }

    private SerialIssue insert(SerialIssue i) {
        String sql = """
                INSERT INTO serial_issues (subscription_id, enumeration, expected_date, received_date, status)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, i);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return new SerialIssue(keys.getLong("id"), i.subscriptionId(), i.enumeration(),
                        i.expectedDate(), i.receivedDate(), i.status());
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to insert issue for subscription " + i.subscriptionId(), e);
        }
    }

    private SerialIssue update(SerialIssue i) {
        String sql = """
                UPDATE serial_issues SET subscription_id = ?, enumeration = ?, expected_date = ?,
                       received_date = ?, status = ?
                 WHERE id = ?
                """;
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, i);
            ps.setLong(6, i.id());
            ps.executeUpdate();
            return i;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update issue id=" + i.id(), e);
        }
    }

    private void bind(PreparedStatement ps, SerialIssue i) throws SQLException {
        ps.setLong(1, i.subscriptionId());
        ps.setString(2, i.enumeration());
        setDate(ps, 3, i.expectedDate());
        setDate(ps, 4, i.receivedDate());
        ps.setString(5, i.status().name());
    }

    private static void setDate(PreparedStatement ps, int index, LocalDate value) throws SQLException {
        if (value != null) {
            ps.setDate(index, Date.valueOf(value));
        } else {
            ps.setNull(index, Types.DATE);
        }
    }

    @Override
    public List<SerialIssue> findBySubscription(Long subscriptionId) {
        return query("SELECT * FROM serial_issues WHERE subscription_id = ? ORDER BY id",
                ps -> ps.setLong(1, subscriptionId));
    }

    @Override
    public List<SerialIssue> findExpectedBefore(LocalDate cutoff) {
        return query("SELECT * FROM serial_issues WHERE status = 'EXPECTED' AND expected_date < ? "
                + "ORDER BY expected_date", ps -> ps.setDate(1, Date.valueOf(cutoff)));
    }

    @Override
    public Optional<SerialIssue> findById(Long id) {
        return query("SELECT * FROM serial_issues WHERE id = ?", ps -> ps.setLong(1, id))
                .stream().findFirst();
    }

    @Override
    public List<SerialIssue> findAll() {
        return query("SELECT * FROM serial_issues ORDER BY id", ps -> { });
    }

    @Override
    public void deleteById(Long id) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM serial_issues WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete issue id=" + id, e);
        }
    }

    private List<SerialIssue> query(String sql, StatementBinder binder) {
        List<SerialIssue> issues = new ArrayList<>();
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Date expected = rs.getDate("expected_date");
                    Date received = rs.getDate("received_date");
                    issues.add(new SerialIssue(
                            rs.getLong("id"),
                            rs.getLong("subscription_id"),
                            rs.getString("enumeration"),
                            expected != null ? expected.toLocalDate() : null,
                            received != null ? received.toLocalDate() : null,
                            IssueStatus.valueOf(rs.getString("status"))));
                }
            }
            return issues;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query serial issues", e);
        }
    }

    @FunctionalInterface
    private interface StatementBinder {
        void bind(PreparedStatement ps) throws SQLException;
    }
}
