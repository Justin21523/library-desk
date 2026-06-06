package com.justin.libradesk.repository.jdbc;

import com.justin.libradesk.domain.enumtype.LoanStatus;
import com.justin.libradesk.domain.model.Loan;
import com.justin.libradesk.infrastructure.database.DatabaseManager;
import com.justin.libradesk.repository.DataAccessException;
import com.justin.libradesk.repository.LoanRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcLoanRepository implements LoanRepository {

    private final DatabaseManager db;

    public JdbcLoanRepository(DatabaseManager db) {
        this.db = db;
    }

    @Override
    public Loan save(Loan loan) {
        return loan.getId() == null ? insert(loan) : update(loan);
    }

    private Loan insert(Loan loan) {
        String sql = """
                INSERT INTO loans (copy_id, patron_id, loaned_at, due_at, returned_at, status)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, loan);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    loan.setId(keys.getLong("id"));
                }
            }
            return loan;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to insert loan for copy " + loan.getCopyId(), e);
        }
    }

    private Loan update(Loan loan) {
        String sql = """
                UPDATE loans
                   SET copy_id = ?, patron_id = ?, loaned_at = ?, due_at = ?,
                       returned_at = ?, status = ?
                 WHERE id = ?
                """;
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, loan);
            ps.setLong(7, loan.getId());
            ps.executeUpdate();
            return loan;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update loan id=" + loan.getId(), e);
        }
    }

    @Override
    public Optional<Loan> findById(Long id) {
        return queryOne("SELECT * FROM loans WHERE id = ?", ps -> ps.setLong(1, id));
    }

    // An OVERDUE loan is still outstanding: it counts against the patron's limit
    // and can still be returned, so the "active" queries include both statuses.
    private static final String OUTSTANDING = "status IN ('ACTIVE', 'OVERDUE')";

    @Override
    public int countActiveByPatron(Long patronId) {
        String sql = "SELECT COUNT(*) FROM loans WHERE patron_id = ? AND " + OUTSTANDING;
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, patronId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to count active loans for patron " + patronId, e);
        }
    }

    @Override
    public List<Loan> findActiveByPatron(Long patronId) {
        return queryList("SELECT * FROM loans WHERE patron_id = ? AND " + OUTSTANDING + " ORDER BY due_at",
                ps -> ps.setLong(1, patronId));
    }

    @Override
    public Optional<Loan> findActiveByCopy(Long copyId) {
        return queryOne("SELECT * FROM loans WHERE copy_id = ? AND " + OUTSTANDING,
                ps -> ps.setLong(1, copyId));
    }

    @Override
    public List<Loan> findOverdue() {
        return queryList("SELECT * FROM loans WHERE status = 'ACTIVE' AND due_at < now() ORDER BY due_at",
                ps -> { });
    }

    @Override
    public List<Loan> findAll() {
        return queryList("SELECT * FROM loans ORDER BY loaned_at DESC", ps -> { });
    }

    @Override
    public void deleteById(Long id) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM loans WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete loan id=" + id, e);
        }
    }

    private void bind(PreparedStatement ps, Loan loan) throws SQLException {
        ps.setLong(1, loan.getCopyId());
        ps.setLong(2, loan.getPatronId());
        ps.setTimestamp(3, Timestamp.valueOf(loan.getLoanedAt()));
        ps.setTimestamp(4, Timestamp.valueOf(loan.getDueAt()));
        if (loan.getReturnedAt() != null) {
            ps.setTimestamp(5, Timestamp.valueOf(loan.getReturnedAt()));
        } else {
            ps.setNull(5, Types.TIMESTAMP);
        }
        ps.setString(6, loan.getStatus().name());
    }

    private Optional<Loan> queryOne(String sql, StatementBinder binder) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query loan", e);
        }
    }

    private List<Loan> queryList(String sql, StatementBinder binder) {
        List<Loan> loans = new ArrayList<>();
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    loans.add(mapRow(rs));
                }
            }
            return loans;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to list loans", e);
        }
    }

    private Loan mapRow(ResultSet rs) throws SQLException {
        Timestamp returned = rs.getTimestamp("returned_at");
        LocalDateTime returnedAt = returned != null ? returned.toLocalDateTime() : null;
        return new Loan(
                rs.getLong("id"),
                rs.getLong("copy_id"),
                rs.getLong("patron_id"),
                rs.getTimestamp("loaned_at").toLocalDateTime(),
                rs.getTimestamp("due_at").toLocalDateTime(),
                returnedAt,
                LoanStatus.valueOf(rs.getString("status")));
    }

    @FunctionalInterface
    private interface StatementBinder {
        void bind(PreparedStatement ps) throws SQLException;
    }
}
