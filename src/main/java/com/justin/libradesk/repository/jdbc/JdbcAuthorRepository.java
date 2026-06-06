package com.justin.libradesk.repository.jdbc;

import com.justin.libradesk.domain.model.Author;
import com.justin.libradesk.infrastructure.database.DatabaseManager;
import com.justin.libradesk.repository.AuthorRepository;
import com.justin.libradesk.repository.DataAccessException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcAuthorRepository implements AuthorRepository {

    private final DatabaseManager db;

    public JdbcAuthorRepository(DatabaseManager db) {
        this.db = db;
    }

    @Override
    public Author save(Author author) {
        return author.id() == null ? insert(author) : update(author);
    }

    private Author insert(Author author) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO authors (name) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, author.name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return new Author(keys.getLong("id"), author.name());
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to insert author: " + author.name(), e);
        }
    }

    private Author update(Author author) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE authors SET name = ? WHERE id = ?")) {
            ps.setString(1, author.name());
            ps.setLong(2, author.id());
            ps.executeUpdate();
            return author;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update author id=" + author.id(), e);
        }
    }

    @Override
    public Optional<Author> findById(Long id) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT id, name FROM authors WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(new Author(rs.getLong("id"), rs.getString("name")))
                        : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query author id=" + id, e);
        }
    }

    @Override
    public List<Author> findAll() {
        List<Author> authors = new ArrayList<>();
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT id, name FROM authors ORDER BY name");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                authors.add(new Author(rs.getLong("id"), rs.getString("name")));
            }
            return authors;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to list authors", e);
        }
    }

    @Override
    public void deleteById(Long id) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM authors WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete author id=" + id, e);
        }
    }
}
