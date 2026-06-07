package com.justin.libradesk.repository.jdbc;

import com.justin.libradesk.domain.model.Branch;
import com.justin.libradesk.infrastructure.database.DatabaseManager;
import com.justin.libradesk.repository.BranchRepository;
import com.justin.libradesk.repository.DataAccessException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcBranchRepository implements BranchRepository {

    private final DatabaseManager db;

    public JdbcBranchRepository(DatabaseManager db) {
        this.db = db;
    }

    @Override
    public Branch save(Branch branch) {
        return branch.id() == null ? insert(branch) : update(branch);
    }

    private Branch insert(Branch branch) {
        String sql = "INSERT INTO branches (code, name) VALUES (?, ?)";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, branch.code());
            ps.setString(2, branch.name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return new Branch(keys.getLong("id"), branch.code(), branch.name());
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to insert branch: " + branch.code(), e);
        }
    }

    private Branch update(Branch branch) {
        String sql = "UPDATE branches SET code = ?, name = ? WHERE id = ?";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, branch.code());
            ps.setString(2, branch.name());
            ps.setLong(3, branch.id());
            ps.executeUpdate();
            return branch;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update branch id=" + branch.id(), e);
        }
    }

    @Override
    public Optional<Branch> findById(Long id) {
        String sql = "SELECT * FROM branches WHERE id = ?";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query branch id=" + id, e);
        }
    }

    @Override
    public List<Branch> findAll() {
        List<Branch> branches = new ArrayList<>();
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM branches ORDER BY code");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                branches.add(mapRow(rs));
            }
            return branches;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to list branches", e);
        }
    }

    @Override
    public void deleteById(Long id) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM branches WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete branch id=" + id, e);
        }
    }

    private Branch mapRow(ResultSet rs) throws SQLException {
        return new Branch(rs.getLong("id"), rs.getString("code"), rs.getString("name"));
    }
}
