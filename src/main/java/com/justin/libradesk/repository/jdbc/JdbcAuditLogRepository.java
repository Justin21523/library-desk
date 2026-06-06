package com.justin.libradesk.repository.jdbc;

import com.justin.libradesk.domain.model.AuditLog;
import com.justin.libradesk.infrastructure.database.DatabaseManager;
import com.justin.libradesk.repository.AuditLogRepository;
import com.justin.libradesk.repository.DataAccessException;

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

public class JdbcAuditLogRepository implements AuditLogRepository {

    private final DatabaseManager db;

    public JdbcAuditLogRepository(DatabaseManager db) {
        this.db = db;
    }

    @Override
    public AuditLog save(AuditLog entry) {
        String sql = """
                INSERT INTO audit_logs (actor, action, entity_type, entity_id, detail, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, entry.getActor());
            ps.setString(2, entry.getAction());
            ps.setString(3, entry.getEntityType());
            if (entry.getEntityId() != null) {
                ps.setLong(4, entry.getEntityId());
            } else {
                ps.setNull(4, Types.BIGINT);
            }
            ps.setString(5, entry.getDetail());
            ps.setTimestamp(6, Timestamp.valueOf(entry.getCreatedAt()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    entry.setId(keys.getLong("id"));
                }
            }
            return entry;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to write audit log: " + entry.getAction(), e);
        }
    }

    @Override
    public Optional<AuditLog> findById(Long id) {
        String sql = "SELECT * FROM audit_logs WHERE id = ?";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query audit log id=" + id, e);
        }
    }

    @Override
    public List<AuditLog> findAll() {
        List<AuditLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM audit_logs ORDER BY created_at DESC";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                logs.add(mapRow(rs));
            }
            return logs;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to list audit logs", e);
        }
    }

    @Override
    public void deleteById(Long id) {
        throw new UnsupportedOperationException("Audit logs are append-only and cannot be deleted");
    }

    private AuditLog mapRow(ResultSet rs) throws SQLException {
        long rawEntityId = rs.getLong("entity_id");
        Long entityId = rs.wasNull() ? null : rawEntityId; // capture wasNull() before reading more columns
        return new AuditLog(
                rs.getLong("id"),
                rs.getString("actor"),
                rs.getString("action"),
                rs.getString("entity_type"),
                entityId,
                rs.getString("detail"),
                rs.getTimestamp("created_at").toLocalDateTime());
    }
}
