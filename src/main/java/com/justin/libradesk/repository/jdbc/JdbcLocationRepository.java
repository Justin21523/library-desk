package com.justin.libradesk.repository.jdbc;

import com.justin.libradesk.domain.model.ShelfLocation;
import com.justin.libradesk.infrastructure.database.DatabaseManager;
import com.justin.libradesk.repository.DataAccessException;
import com.justin.libradesk.repository.LocationRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcLocationRepository implements LocationRepository {

    private final DatabaseManager db;

    public JdbcLocationRepository(DatabaseManager db) {
        this.db = db;
    }

    @Override
    public ShelfLocation save(ShelfLocation location) {
        return location.id() == null ? insert(location) : update(location);
    }

    private ShelfLocation insert(ShelfLocation location) {
        String sql = "INSERT INTO locations (branch_id, name) VALUES (?, ?)";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, location.branchId());
            ps.setString(2, location.name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return new ShelfLocation(keys.getLong("id"), location.branchId(), location.name());
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to insert location: " + location.name(), e);
        }
    }

    private ShelfLocation update(ShelfLocation location) {
        String sql = "UPDATE locations SET branch_id = ?, name = ? WHERE id = ?";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, location.branchId());
            ps.setString(2, location.name());
            ps.setLong(3, location.id());
            ps.executeUpdate();
            return location;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update location id=" + location.id(), e);
        }
    }

    @Override
    public Optional<ShelfLocation> findById(Long id) {
        return queryList("SELECT * FROM locations WHERE id = ?", ps -> ps.setLong(1, id))
                .stream().findFirst();
    }

    @Override
    public List<ShelfLocation> findByBranch(Long branchId) {
        return queryList("SELECT * FROM locations WHERE branch_id = ? ORDER BY name",
                ps -> ps.setLong(1, branchId));
    }

    @Override
    public List<ShelfLocation> findAll() {
        return queryList("SELECT * FROM locations ORDER BY branch_id, name", ps -> { });
    }

    @Override
    public void deleteById(Long id) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM locations WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete location id=" + id, e);
        }
    }

    private List<ShelfLocation> queryList(String sql, StatementBinder binder) {
        List<ShelfLocation> locations = new ArrayList<>();
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    locations.add(new ShelfLocation(
                            rs.getLong("id"), rs.getLong("branch_id"), rs.getString("name")));
                }
            }
            return locations;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to list locations", e);
        }
    }

    @FunctionalInterface
    private interface StatementBinder {
        void bind(PreparedStatement ps) throws SQLException;
    }
}
