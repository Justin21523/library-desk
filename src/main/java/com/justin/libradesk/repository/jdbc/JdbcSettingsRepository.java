package com.justin.libradesk.repository.jdbc;

import com.justin.libradesk.infrastructure.database.DatabaseManager;
import com.justin.libradesk.repository.DataAccessException;
import com.justin.libradesk.repository.SettingsRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class JdbcSettingsRepository implements SettingsRepository {

    private final DatabaseManager db;

    public JdbcSettingsRepository(DatabaseManager db) {
        this.db = db;
    }

    @Override
    public Optional<String> find(String key) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT value FROM settings WHERE key = ?")) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(rs.getString("value")) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to read setting: " + key, e);
        }
    }

    @Override
    public void put(String key, String value) {
        String sql = """
                INSERT INTO settings (key, value) VALUES (?, ?)
                ON CONFLICT (key) DO UPDATE SET value = excluded.value
                """;
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to write setting: " + key, e);
        }
    }

    @Override
    public Map<String, String> findAll() {
        Map<String, String> settings = new LinkedHashMap<>();
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT key, value FROM settings ORDER BY key");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                settings.put(rs.getString("key"), rs.getString("value"));
            }
            return settings;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to list settings", e);
        }
    }
}
