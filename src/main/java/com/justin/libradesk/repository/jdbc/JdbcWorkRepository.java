package com.justin.libradesk.repository.jdbc;

import com.justin.libradesk.domain.model.Work;
import com.justin.libradesk.infrastructure.database.DatabaseManager;
import com.justin.libradesk.repository.DataAccessException;
import com.justin.libradesk.repository.WorkRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcWorkRepository implements WorkRepository {

    private final DatabaseManager db;

    public JdbcWorkRepository(DatabaseManager db) {
        this.db = db;
    }

    @Override
    public Work save(Work work) {
        return work.id() == null ? insert(work) : update(work);
    }

    private Work insert(Work work) {
        String sql = "INSERT INTO works (work_key, title, author) VALUES (?, ?, ?)";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, work.workKey());
            ps.setString(2, work.title());
            ps.setString(3, work.author());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return new Work(keys.getLong("id"), work.workKey(), work.title(), work.author());
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to insert work: " + work.workKey(), e);
        }
    }

    private Work update(Work work) {
        String sql = "UPDATE works SET work_key = ?, title = ?, author = ? WHERE id = ?";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, work.workKey());
            ps.setString(2, work.title());
            ps.setString(3, work.author());
            ps.setLong(4, work.id());
            ps.executeUpdate();
            return work;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update work id=" + work.id(), e);
        }
    }

    @Override
    public Optional<Work> findByKey(String workKey) {
        return query("SELECT * FROM works WHERE work_key = ?", ps -> ps.setString(1, workKey))
                .stream().findFirst();
    }

    @Override
    public Optional<Work> findById(Long id) {
        return query("SELECT * FROM works WHERE id = ?", ps -> ps.setLong(1, id)).stream().findFirst();
    }

    @Override
    public List<Work> findAll() {
        return query("SELECT * FROM works ORDER BY title", ps -> { });
    }

    @Override
    public void deleteById(Long id) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM works WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete work id=" + id, e);
        }
    }

    private List<Work> query(String sql, StatementBinder binder) {
        List<Work> works = new ArrayList<>();
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    works.add(new Work(rs.getLong("id"), rs.getString("work_key"),
                            rs.getString("title"), rs.getString("author")));
                }
            }
            return works;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query works", e);
        }
    }

    @FunctionalInterface
    private interface StatementBinder {
        void bind(PreparedStatement ps) throws SQLException;
    }
}
