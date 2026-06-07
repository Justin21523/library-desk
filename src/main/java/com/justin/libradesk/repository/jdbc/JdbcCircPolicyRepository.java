package com.justin.libradesk.repository.jdbc;

import com.justin.libradesk.domain.enumtype.MaterialType;
import com.justin.libradesk.domain.enumtype.PatronType;
import com.justin.libradesk.domain.model.CircPolicy;
import com.justin.libradesk.infrastructure.database.DatabaseManager;
import com.justin.libradesk.repository.CircPolicyRepository;
import com.justin.libradesk.repository.DataAccessException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcCircPolicyRepository implements CircPolicyRepository {

    private final DatabaseManager db;

    public JdbcCircPolicyRepository(DatabaseManager db) {
        this.db = db;
    }

    @Override
    public CircPolicy save(CircPolicy p) {
        return p.id() == null ? insert(p) : update(p);
    }

    private CircPolicy insert(CircPolicy p) {
        String sql = """
                INSERT INTO circ_policies
                    (patron_type, material_type, loan_days, max_loans, renewal_limit,
                     max_holds, fine_per_day, fine_cap, grace_days)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, p);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return withId(keys.getLong("id"), p);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to insert circ policy for " + p.patronType(), e);
        }
    }

    private CircPolicy update(CircPolicy p) {
        String sql = """
                UPDATE circ_policies
                   SET patron_type = ?, material_type = ?, loan_days = ?, max_loans = ?,
                       renewal_limit = ?, max_holds = ?, fine_per_day = ?, fine_cap = ?, grace_days = ?
                 WHERE id = ?
                """;
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, p);
            ps.setLong(10, p.id());
            ps.executeUpdate();
            return p;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update circ policy id=" + p.id(), e);
        }
    }

    @Override
    public Optional<CircPolicy> findFor(PatronType patronType, MaterialType materialType) {
        // Prefer an exact (patron, material) row; fall back to the patron-type default.
        Optional<CircPolicy> exact = queryList(
                "SELECT * FROM circ_policies WHERE patron_type = ? AND material_type = ?",
                ps -> {
                    ps.setString(1, patronType.name());
                    ps.setString(2, materialType == null ? null : materialType.name());
                }).stream().findFirst();
        if (exact.isPresent()) {
            return exact;
        }
        return queryList(
                "SELECT * FROM circ_policies WHERE patron_type = ? AND material_type IS NULL",
                ps -> ps.setString(1, patronType.name())).stream().findFirst();
    }

    @Override
    public Optional<CircPolicy> findById(Long id) {
        return queryList("SELECT * FROM circ_policies WHERE id = ?", ps -> ps.setLong(1, id))
                .stream().findFirst();
    }

    @Override
    public List<CircPolicy> findAll() {
        return queryList("SELECT * FROM circ_policies ORDER BY patron_type, material_type NULLS FIRST",
                ps -> { });
    }

    @Override
    public void deleteById(Long id) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM circ_policies WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete circ policy id=" + id, e);
        }
    }

    private void bind(PreparedStatement ps, CircPolicy p) throws SQLException {
        ps.setString(1, p.patronType().name());
        ps.setString(2, p.materialType() == null ? null : p.materialType().name());
        ps.setInt(3, p.loanDays());
        ps.setInt(4, p.maxLoans());
        ps.setInt(5, p.renewalLimit());
        ps.setInt(6, p.maxHolds());
        ps.setBigDecimal(7, p.finePerDay());
        ps.setBigDecimal(8, p.fineCap());
        ps.setInt(9, p.graceDays());
    }

    private static CircPolicy withId(Long id, CircPolicy p) {
        return new CircPolicy(id, p.patronType(), p.materialType(), p.loanDays(), p.maxLoans(),
                p.renewalLimit(), p.maxHolds(), p.finePerDay(), p.fineCap(), p.graceDays());
    }

    private List<CircPolicy> queryList(String sql, StatementBinder binder) {
        List<CircPolicy> policies = new ArrayList<>();
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    policies.add(mapRow(rs));
                }
            }
            return policies;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to list circ policies", e);
        }
    }

    private CircPolicy mapRow(ResultSet rs) throws SQLException {
        String material = rs.getString("material_type");
        return new CircPolicy(
                rs.getLong("id"),
                PatronType.valueOf(rs.getString("patron_type")),
                material == null ? null : MaterialType.valueOf(material),
                rs.getInt("loan_days"),
                rs.getInt("max_loans"),
                rs.getInt("renewal_limit"),
                rs.getInt("max_holds"),
                rs.getBigDecimal("fine_per_day"),
                rs.getBigDecimal("fine_cap"),
                rs.getInt("grace_days"));
    }

    @FunctionalInterface
    private interface StatementBinder {
        void bind(PreparedStatement ps) throws SQLException;
    }
}
