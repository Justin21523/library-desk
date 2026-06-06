package com.justin.libradesk.repository.jdbc;

import com.justin.libradesk.domain.enumtype.PatronStatus;
import com.justin.libradesk.domain.enumtype.PatronType;
import com.justin.libradesk.domain.model.Patron;
import com.justin.libradesk.infrastructure.database.DatabaseManager;
import com.justin.libradesk.repository.DataAccessException;
import com.justin.libradesk.repository.PatronRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcPatronRepository implements PatronRepository {

    private final DatabaseManager db;

    public JdbcPatronRepository(DatabaseManager db) {
        this.db = db;
    }

    @Override
    public Patron save(Patron patron) {
        return patron.getId() == null ? insert(patron) : update(patron);
    }

    private Patron insert(Patron patron) {
        String sql = """
                INSERT INTO patrons (membership_no, full_name, email, phone, patron_type, status, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, patron);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    patron.setId(keys.getLong("id"));
                }
            }
            return patron;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to insert patron: " + patron.getMembershipNo(), e);
        }
    }

    private Patron update(Patron patron) {
        String sql = """
                UPDATE patrons
                   SET membership_no = ?, full_name = ?, email = ?, phone = ?,
                       patron_type = ?, status = ?
                 WHERE id = ?
                """;
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, patron.getMembershipNo());
            ps.setString(2, patron.getFullName());
            ps.setString(3, patron.getEmail());
            ps.setString(4, patron.getPhone());
            ps.setString(5, patron.getPatronType().name());
            ps.setString(6, patron.getStatus().name());
            ps.setLong(7, patron.getId());
            ps.executeUpdate();
            return patron;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update patron id=" + patron.getId(), e);
        }
    }

    @Override
    public Optional<Patron> findById(Long id) {
        return queryOne("SELECT * FROM patrons WHERE id = ?", ps -> ps.setLong(1, id));
    }

    @Override
    public Optional<Patron> findByMembershipNo(String membershipNo) {
        return queryOne("SELECT * FROM patrons WHERE membership_no = ?",
                ps -> ps.setString(1, membershipNo));
    }

    @Override
    public List<Patron> findAll() {
        List<Patron> patrons = new ArrayList<>();
        String sql = "SELECT * FROM patrons ORDER BY full_name";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                patrons.add(mapRow(rs));
            }
            return patrons;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to list patrons", e);
        }
    }

    @Override
    public void deleteById(Long id) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM patrons WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete patron id=" + id, e);
        }
    }

    private void bind(PreparedStatement ps, Patron patron) throws SQLException {
        ps.setString(1, patron.getMembershipNo());
        ps.setString(2, patron.getFullName());
        ps.setString(3, patron.getEmail());
        ps.setString(4, patron.getPhone());
        ps.setString(5, patron.getPatronType().name());
        ps.setString(6, patron.getStatus().name());
        ps.setTimestamp(7, Timestamp.valueOf(patron.getCreatedAt()));
    }

    private Optional<Patron> queryOne(String sql, StatementBinder binder) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query patron", e);
        }
    }

    private Patron mapRow(ResultSet rs) throws SQLException {
        return new Patron(
                rs.getLong("id"),
                rs.getString("membership_no"),
                rs.getString("full_name"),
                rs.getString("email"),
                rs.getString("phone"),
                PatronType.valueOf(rs.getString("patron_type")),
                PatronStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("created_at").toLocalDateTime());
    }

    @FunctionalInterface
    private interface StatementBinder {
        void bind(PreparedStatement ps) throws SQLException;
    }
}
