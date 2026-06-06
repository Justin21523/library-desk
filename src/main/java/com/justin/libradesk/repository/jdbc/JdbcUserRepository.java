package com.justin.libradesk.repository.jdbc;

import com.justin.libradesk.domain.enumtype.UserRole;
import com.justin.libradesk.domain.model.User;
import com.justin.libradesk.infrastructure.database.DatabaseManager;
import com.justin.libradesk.repository.DataAccessException;
import com.justin.libradesk.repository.UserRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcUserRepository implements UserRepository {

    private final DatabaseManager db;

    public JdbcUserRepository(DatabaseManager db) {
        this.db = db;
    }

    @Override
    public User save(User user) {
        return user.getId() == null ? insert(user) : update(user);
    }

    private User insert(User user) {
        String sql = """
                INSERT INTO users (username, password_hash, full_name, role, active, must_change_password, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPasswordHash());
            ps.setString(3, user.getFullName());
            ps.setString(4, user.getRole().name());
            ps.setBoolean(5, user.isActive());
            ps.setBoolean(6, user.isMustChangePassword());
            ps.setTimestamp(7, Timestamp.valueOf(user.getCreatedAt()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    user.setId(keys.getLong("id"));
                }
            }
            return user;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to insert user: " + user.getUsername(), e);
        }
    }

    private User update(User user) {
        String sql = """
                UPDATE users
                   SET username = ?, password_hash = ?, full_name = ?, role = ?, active = ?,
                       must_change_password = ?
                 WHERE id = ?
                """;
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPasswordHash());
            ps.setString(3, user.getFullName());
            ps.setString(4, user.getRole().name());
            ps.setBoolean(5, user.isActive());
            ps.setBoolean(6, user.isMustChangePassword());
            ps.setLong(7, user.getId());
            ps.executeUpdate();
            return user;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update user id=" + user.getId(), e);
        }
    }

    @Override
    public Optional<User> findById(Long id) {
        return queryOne("SELECT * FROM users WHERE id = ?", ps -> ps.setLong(1, id));
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return queryOne("SELECT * FROM users WHERE username = ?", ps -> ps.setString(1, username));
    }

    @Override
    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY username";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                users.add(mapRow(rs));
            }
            return users;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to list users", e);
        }
    }

    @Override
    public void deleteById(Long id) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM users WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete user id=" + id, e);
        }
    }

    private Optional<User> queryOne(String sql, StatementBinder binder) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query user", e);
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        User user = new User(
                rs.getLong("id"),
                rs.getString("username"),
                rs.getString("password_hash"),
                rs.getString("full_name"),
                UserRole.valueOf(rs.getString("role")),
                rs.getBoolean("active"),
                rs.getTimestamp("created_at").toLocalDateTime());
        user.setMustChangePassword(rs.getBoolean("must_change_password"));
        return user;
    }

    @FunctionalInterface
    private interface StatementBinder {
        void bind(PreparedStatement ps) throws SQLException;
    }
}
