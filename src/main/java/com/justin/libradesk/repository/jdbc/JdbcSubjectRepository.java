package com.justin.libradesk.repository.jdbc;

import com.justin.libradesk.domain.model.Subject;
import com.justin.libradesk.infrastructure.database.DatabaseManager;
import com.justin.libradesk.repository.DataAccessException;
import com.justin.libradesk.repository.SubjectRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcSubjectRepository implements SubjectRepository {

    private final DatabaseManager db;

    public JdbcSubjectRepository(DatabaseManager db) {
        this.db = db;
    }

    @Override
    public Subject save(Subject subject) {
        return subject.id() == null ? insert(subject) : update(subject);
    }

    private Subject insert(Subject subject) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO subjects (term) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, subject.term());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return new Subject(keys.getLong("id"), subject.term());
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to insert subject: " + subject.term(), e);
        }
    }

    private Subject update(Subject subject) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE subjects SET term = ? WHERE id = ?")) {
            ps.setString(1, subject.term());
            ps.setLong(2, subject.id());
            ps.executeUpdate();
            return subject;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update subject id=" + subject.id(), e);
        }
    }

    @Override
    public Optional<Subject> findById(Long id) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT id, term FROM subjects WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(new Subject(rs.getLong("id"), rs.getString("term")))
                        : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query subject id=" + id, e);
        }
    }

    @Override
    public List<Subject> findAll() {
        List<Subject> subjects = new ArrayList<>();
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT id, term FROM subjects ORDER BY term");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                subjects.add(new Subject(rs.getLong("id"), rs.getString("term")));
            }
            return subjects;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to list subjects", e);
        }
    }

    @Override
    public void deleteById(Long id) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM subjects WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete subject id=" + id, e);
        }
    }
}
