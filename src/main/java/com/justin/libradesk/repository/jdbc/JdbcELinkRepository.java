package com.justin.libradesk.repository.jdbc;

import com.justin.libradesk.domain.model.ELink;
import com.justin.libradesk.infrastructure.database.DatabaseManager;
import com.justin.libradesk.repository.DataAccessException;
import com.justin.libradesk.repository.ELinkRepository;

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

public class JdbcELinkRepository implements ELinkRepository {

    private final DatabaseManager db;

    public JdbcELinkRepository(DatabaseManager db) {
        this.db = db;
    }

    @Override
    public ELink save(ELink link) {
        return link.id() == null ? insert(link) : update(link);
    }

    private ELink insert(ELink l) {
        String sql = """
                INSERT INTO e_links (book_id, url, label, last_status, last_checked)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, l);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return new ELink(keys.getLong("id"), l.bookId(), l.url(), l.label(),
                        l.lastStatus(), l.lastChecked());
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to insert e-link for book " + l.bookId(), e);
        }
    }

    private ELink update(ELink l) {
        String sql = """
                UPDATE e_links SET book_id = ?, url = ?, label = ?, last_status = ?, last_checked = ?
                 WHERE id = ?
                """;
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, l);
            ps.setLong(6, l.id());
            ps.executeUpdate();
            return l;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update e-link id=" + l.id(), e);
        }
    }

    private void bind(PreparedStatement ps, ELink l) throws SQLException {
        ps.setLong(1, l.bookId());
        ps.setString(2, l.url());
        ps.setString(3, l.label());
        if (l.lastStatus() != null) {
            ps.setInt(4, l.lastStatus());
        } else {
            ps.setNull(4, Types.INTEGER);
        }
        if (l.lastChecked() != null) {
            ps.setTimestamp(5, Timestamp.valueOf(l.lastChecked()));
        } else {
            ps.setNull(5, Types.TIMESTAMP);
        }
    }

    @Override
    public List<ELink> findByBook(Long bookId) {
        return query("SELECT * FROM e_links WHERE book_id = ? ORDER BY id", ps -> ps.setLong(1, bookId));
    }

    @Override
    public Optional<ELink> findById(Long id) {
        return query("SELECT * FROM e_links WHERE id = ?", ps -> ps.setLong(1, id)).stream().findFirst();
    }

    @Override
    public List<ELink> findAll() {
        return query("SELECT * FROM e_links ORDER BY id", ps -> { });
    }

    @Override
    public void deleteById(Long id) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM e_links WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete e-link id=" + id, e);
        }
    }

    private List<ELink> query(String sql, StatementBinder binder) {
        List<ELink> links = new ArrayList<>();
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp checked = rs.getTimestamp("last_checked");
                    links.add(new ELink(
                            rs.getLong("id"),
                            rs.getLong("book_id"),
                            rs.getString("url"),
                            rs.getString("label"),
                            rs.getObject("last_status", Integer.class),
                            checked != null ? checked.toLocalDateTime() : null));
                }
            }
            return links;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query e-links", e);
        }
    }

    @FunctionalInterface
    private interface StatementBinder {
        void bind(PreparedStatement ps) throws SQLException;
    }
}
