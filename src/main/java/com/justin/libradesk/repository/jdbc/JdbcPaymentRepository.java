package com.justin.libradesk.repository.jdbc;

import com.justin.libradesk.domain.model.Payment;
import com.justin.libradesk.infrastructure.database.DatabaseManager;
import com.justin.libradesk.repository.DataAccessException;
import com.justin.libradesk.repository.PaymentRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class JdbcPaymentRepository implements PaymentRepository {

    private final DatabaseManager db;

    public JdbcPaymentRepository(DatabaseManager db) {
        this.db = db;
    }

    @Override
    public Payment save(Payment p) {
        String sql = """
                INSERT INTO payments (fine_id, amount, method, note, paid_at, actor)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, p.fineId());
            ps.setBigDecimal(2, p.amount());
            ps.setString(3, p.method());
            ps.setString(4, p.note());
            ps.setTimestamp(5, Timestamp.valueOf(p.paidAt()));
            ps.setString(6, p.actor());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return new Payment(keys.getLong("id"), p.fineId(), p.amount(), p.method(),
                        p.note(), p.paidAt(), p.actor());
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to insert payment for fine " + p.fineId(), e);
        }
    }

    @Override
    public List<Payment> findByFine(Long fineId) {
        return queryList("SELECT * FROM payments WHERE fine_id = ? ORDER BY paid_at",
                ps -> ps.setLong(1, fineId));
    }

    @Override
    public List<Payment> findByPatron(Long patronId) {
        String sql = """
                SELECT p.* FROM payments p
                  JOIN fines f ON f.id = p.fine_id
                 WHERE f.patron_id = ?
                 ORDER BY p.paid_at DESC
                """;
        return queryList(sql, ps -> ps.setLong(1, patronId));
    }

    private List<Payment> queryList(String sql, StatementBinder binder) {
        List<Payment> payments = new ArrayList<>();
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    payments.add(mapRow(rs));
                }
            }
            return payments;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to list payments", e);
        }
    }

    private Payment mapRow(ResultSet rs) throws SQLException {
        return new Payment(
                rs.getLong("id"),
                rs.getLong("fine_id"),
                rs.getBigDecimal("amount"),
                rs.getString("method"),
                rs.getString("note"),
                rs.getTimestamp("paid_at").toLocalDateTime(),
                rs.getString("actor"));
    }

    @FunctionalInterface
    private interface StatementBinder {
        void bind(PreparedStatement ps) throws SQLException;
    }
}
