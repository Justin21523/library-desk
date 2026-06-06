package com.justin.libradesk.repository.jdbc;

import com.justin.libradesk.domain.model.Publisher;
import com.justin.libradesk.infrastructure.database.DatabaseManager;
import com.justin.libradesk.repository.DataAccessException;
import com.justin.libradesk.repository.PublisherRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcPublisherRepository implements PublisherRepository {

    private final DatabaseManager db;

    public JdbcPublisherRepository(DatabaseManager db) {
        this.db = db;
    }

    @Override
    public Publisher save(Publisher publisher) {
        return publisher.id() == null ? insert(publisher) : update(publisher);
    }

    private Publisher insert(Publisher publisher) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO publishers (name) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, publisher.name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return new Publisher(keys.getLong("id"), publisher.name());
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to insert publisher: " + publisher.name(), e);
        }
    }

    private Publisher update(Publisher publisher) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE publishers SET name = ? WHERE id = ?")) {
            ps.setString(1, publisher.name());
            ps.setLong(2, publisher.id());
            ps.executeUpdate();
            return publisher;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update publisher id=" + publisher.id(), e);
        }
    }

    @Override
    public Optional<Publisher> findById(Long id) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT id, name FROM publishers WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(new Publisher(rs.getLong("id"), rs.getString("name")))
                        : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query publisher id=" + id, e);
        }
    }

    @Override
    public List<Publisher> findAll() {
        List<Publisher> publishers = new ArrayList<>();
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT id, name FROM publishers ORDER BY name");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                publishers.add(new Publisher(rs.getLong("id"), rs.getString("name")));
            }
            return publishers;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to list publishers", e);
        }
    }

    @Override
    public void deleteById(Long id) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM publishers WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete publisher id=" + id, e);
        }
    }
}
