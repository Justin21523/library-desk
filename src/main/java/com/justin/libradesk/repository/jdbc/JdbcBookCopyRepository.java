package com.justin.libradesk.repository.jdbc;

import com.justin.libradesk.domain.enumtype.CopyStatus;
import com.justin.libradesk.domain.model.BookCopy;
import com.justin.libradesk.infrastructure.database.DatabaseManager;
import com.justin.libradesk.repository.BookCopyRepository;
import com.justin.libradesk.repository.DataAccessException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcBookCopyRepository implements BookCopyRepository {

    private final DatabaseManager db;

    public JdbcBookCopyRepository(DatabaseManager db) {
        this.db = db;
    }

    @Override
    public BookCopy save(BookCopy copy) {
        return copy.getId() == null ? insert(copy) : update(copy);
    }

    private BookCopy insert(BookCopy copy) {
        String sql = """
                INSERT INTO book_copies (book_id, barcode, status, shelf_location, location_id,
                                         holding_id, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, copy.getBookId());
            ps.setString(2, copy.getBarcode());
            ps.setString(3, copy.getStatus().name());
            ps.setString(4, copy.getShelfLocation());
            setNullableLong(ps, 5, copy.getLocationId());
            setNullableLong(ps, 6, copy.getHoldingId());
            ps.setTimestamp(7, Timestamp.valueOf(copy.getCreatedAt()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    copy.setId(keys.getLong("id"));
                }
            }
            return copy;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to insert book copy: " + copy.getBarcode(), e);
        }
    }

    private BookCopy update(BookCopy copy) {
        String sql = """
                UPDATE book_copies
                   SET book_id = ?, barcode = ?, status = ?, shelf_location = ?, location_id = ?,
                       holding_id = ?
                 WHERE id = ?
                """;
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, copy.getBookId());
            ps.setString(2, copy.getBarcode());
            ps.setString(3, copy.getStatus().name());
            ps.setString(4, copy.getShelfLocation());
            setNullableLong(ps, 5, copy.getLocationId());
            setNullableLong(ps, 6, copy.getHoldingId());
            ps.setLong(7, copy.getId());
            ps.executeUpdate();
            return copy;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update book copy id=" + copy.getId(), e);
        }
    }

    @Override
    public Optional<BookCopy> findById(Long id) {
        return queryOne("SELECT * FROM book_copies WHERE id = ?", ps -> ps.setLong(1, id));
    }

    @Override
    public Optional<BookCopy> findByBarcode(String barcode) {
        return queryOne("SELECT * FROM book_copies WHERE barcode = ?", ps -> ps.setString(1, barcode));
    }

    @Override
    public List<BookCopy> findByBookId(Long bookId) {
        return queryList("SELECT * FROM book_copies WHERE book_id = ? ORDER BY barcode",
                ps -> ps.setLong(1, bookId));
    }

    @Override
    public List<BookCopy> findAvailableByBookId(Long bookId) {
        return queryList(
                "SELECT * FROM book_copies WHERE book_id = ? AND status = 'AVAILABLE' ORDER BY barcode",
                ps -> ps.setLong(1, bookId));
    }

    @Override
    public List<BookCopy> findAll() {
        return queryList("SELECT * FROM book_copies ORDER BY barcode", ps -> { });
    }

    @Override
    public void deleteById(Long id) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM book_copies WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete book copy id=" + id, e);
        }
    }

    private Optional<BookCopy> queryOne(String sql, StatementBinder binder) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query book copy", e);
        }
    }

    private List<BookCopy> queryList(String sql, StatementBinder binder) {
        List<BookCopy> copies = new ArrayList<>();
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    copies.add(mapRow(rs));
                }
            }
            return copies;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to list book copies", e);
        }
    }

    private BookCopy mapRow(ResultSet rs) throws SQLException {
        BookCopy copy = new BookCopy(
                rs.getLong("id"),
                rs.getLong("book_id"),
                rs.getString("barcode"),
                CopyStatus.valueOf(rs.getString("status")),
                rs.getString("shelf_location"),
                rs.getTimestamp("created_at").toLocalDateTime());
        long locationId = rs.getLong("location_id");
        copy.setLocationId(rs.wasNull() ? null : locationId);
        long holdingId = rs.getLong("holding_id");
        copy.setHoldingId(rs.wasNull() ? null : holdingId);
        return copy;
    }

    private static void setNullableLong(PreparedStatement ps, int index, Long value) throws SQLException {
        if (value != null) {
            ps.setLong(index, value);
        } else {
            ps.setNull(index, java.sql.Types.BIGINT);
        }
    }

    @FunctionalInterface
    private interface StatementBinder {
        void bind(PreparedStatement ps) throws SQLException;
    }
}
