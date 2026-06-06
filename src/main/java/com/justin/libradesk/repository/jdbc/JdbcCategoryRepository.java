package com.justin.libradesk.repository.jdbc;

import com.justin.libradesk.domain.model.Category;
import com.justin.libradesk.infrastructure.database.DatabaseManager;
import com.justin.libradesk.repository.CategoryRepository;
import com.justin.libradesk.repository.DataAccessException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcCategoryRepository implements CategoryRepository {

    private final DatabaseManager db;

    public JdbcCategoryRepository(DatabaseManager db) {
        this.db = db;
    }

    @Override
    public Category save(Category category) {
        return category.id() == null ? insert(category) : update(category);
    }

    private Category insert(Category category) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO categories (name) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, category.name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return new Category(keys.getLong("id"), category.name());
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to insert category: " + category.name(), e);
        }
    }

    private Category update(Category category) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE categories SET name = ? WHERE id = ?")) {
            ps.setString(1, category.name());
            ps.setLong(2, category.id());
            ps.executeUpdate();
            return category;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update category id=" + category.id(), e);
        }
    }

    @Override
    public Optional<Category> findById(Long id) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT id, name FROM categories WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(new Category(rs.getLong("id"), rs.getString("name")))
                        : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query category id=" + id, e);
        }
    }

    @Override
    public List<Category> findAll() {
        List<Category> categories = new ArrayList<>();
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT id, name FROM categories ORDER BY name");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                categories.add(new Category(rs.getLong("id"), rs.getString("name")));
            }
            return categories;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to list categories", e);
        }
    }

    @Override
    public void deleteById(Long id) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM categories WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete category id=" + id, e);
        }
    }
}
