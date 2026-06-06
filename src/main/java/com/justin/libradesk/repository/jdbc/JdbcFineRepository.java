package com.justin.libradesk.repository.jdbc;

import com.justin.libradesk.domain.enumtype.FineStatus;
import com.justin.libradesk.domain.model.Fine;
import com.justin.libradesk.infrastructure.database.DatabaseManager;
import com.justin.libradesk.repository.DataAccessException;
import com.justin.libradesk.repository.FineRepository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcFineRepository implements FineRepository {

    private final DatabaseManager db;

    public JdbcFineRepository(DatabaseManager db) {
        this.db = db;
    }

    @Override
    public Fine save(Fine fine) {
        return fine.getId() == null ? insert(fine) : update(fine);
    }

    private Fine insert(Fine fine) {
        String sql = """
                INSERT INTO fines (patron_id, loan_id, amount, status, created_at, settled_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, fine);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    fine.setId(keys.getLong("id"));
                }
            }
            return fine;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to insert fine for patron " + fine.getPatronId(), e);
        }
    }

    private Fine update(Fine fine) {
        String sql = """
                UPDATE fines
                   SET patron_id = ?, loan_id = ?, amount = ?, status = ?, created_at = ?, settled_at = ?
                 WHERE id = ?
                """;
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, fine);
            ps.setLong(7, fine.getId());
            ps.executeUpdate();
            return fine;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update fine id=" + fine.getId(), e);
        }
    }

    @Override
    public Optional<Fine> findById(Long id) {
        return queryOne("SELECT * FROM fines WHERE id = ?", ps -> ps.setLong(1, id));
    }

    @Override
    public List<Fine> findByPatron(Long patronId) {
        return queryList("SELECT * FROM fines WHERE patron_id = ? ORDER BY created_at DESC",
                ps -> ps.setLong(1, patronId));
    }

    @Override
    public List<Fine> findUnpaid() {
        return queryList("SELECT * FROM fines WHERE status = 'UNPAID' ORDER BY created_at DESC", ps -> { });
    }

    @Override
    public BigDecimal unpaidTotal(Long patronId) {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM fines WHERE patron_id = ? AND status = 'UNPAID'";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, patronId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to total fines for patron " + patronId, e);
        }
    }

    @Override
    public List<Fine> findAll() {
        return queryList("SELECT * FROM fines ORDER BY created_at DESC", ps -> { });
    }

    @Override
    public void deleteById(Long id) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM fines WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete fine id=" + id, e);
        }
    }

    private void bind(PreparedStatement ps, Fine fine) throws SQLException {
        ps.setLong(1, fine.getPatronId());
        if (fine.getLoanId() != null) {
            ps.setLong(2, fine.getLoanId());
        } else {
            ps.setNull(2, Types.BIGINT);
        }
        ps.setBigDecimal(3, fine.getAmount());
        ps.setString(4, fine.getStatus().name());
        ps.setTimestamp(5, Timestamp.valueOf(fine.getCreatedAt()));
        if (fine.getSettledAt() != null) {
            ps.setTimestamp(6, Timestamp.valueOf(fine.getSettledAt()));
        } else {
            ps.setNull(6, Types.TIMESTAMP);
        }
    }

    private Optional<Fine> queryOne(String sql, StatementBinder binder) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query fine", e);
        }
    }

    private List<Fine> queryList(String sql, StatementBinder binder) {
        List<Fine> fines = new ArrayList<>();
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    fines.add(mapRow(rs));
                }
            }
            return fines;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to list fines", e);
        }
    }

    private Fine mapRow(ResultSet rs) throws SQLException {
        long loanId = rs.getLong("loan_id");
        Long loan = rs.wasNull() ? null : loanId;
        Timestamp settled = rs.getTimestamp("settled_at");
        return new Fine(
                rs.getLong("id"),
                rs.getLong("patron_id"),
                loan,
                rs.getBigDecimal("amount"),
                FineStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("created_at").toLocalDateTime(),
                settled != null ? settled.toLocalDateTime() : null);
    }

    @FunctionalInterface
    private interface StatementBinder {
        void bind(PreparedStatement ps) throws SQLException;
    }
}
