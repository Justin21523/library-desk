package com.justin.libradesk.repository.jdbc;

import com.justin.libradesk.domain.enumtype.ClassificationScheme;
import com.justin.libradesk.domain.enumtype.MaterialType;
import com.justin.libradesk.domain.model.Book;
import com.justin.libradesk.infrastructure.database.DatabaseManager;
import com.justin.libradesk.repository.BookRepository;
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

public class JdbcBookRepository implements BookRepository {

    private final DatabaseManager db;

    public JdbcBookRepository(DatabaseManager db) {
        this.db = db;
    }

    /**
     * Persists the book and its author/subject links atomically: the book row and
     * the {@code book_authors}/{@code book_subjects} junctions are written in one
     * transaction.
     */
    @Override
    public Book save(Book book) {
        try (Connection c = db.getConnection()) {
            boolean previousAutoCommit = c.getAutoCommit();
            c.setAutoCommit(false);
            try {
                if (book.getId() == null) {
                    insertBook(c, book);
                } else {
                    updateBook(c, book);
                }
                syncLinks(c, "book_authors", "author_id", book.getId(), book.getAuthorIds());
                syncLinks(c, "book_subjects", "subject_id", book.getId(), book.getSubjectIds());
                c.commit();
                return book;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(previousAutoCommit);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to save book: " + book.getTitle(), e);
        }
    }

    private void insertBook(Connection c, Book book) throws SQLException {
        String sql = """
                INSERT INTO books (isbn, title, publisher_id, category_id, published_year,
                                   edition, pub_place, extent, series, language, material_type,
                                   control_number, summary, marc_xml, call_number,
                                   classification_scheme, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindBook(ps, book);
            ps.setTimestamp(17, Timestamp.valueOf(book.getCreatedAt()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    book.setId(keys.getLong("id"));
                }
            }
        }
    }

    private void updateBook(Connection c, Book book) throws SQLException {
        String sql = """
                UPDATE books
                   SET isbn = ?, title = ?, publisher_id = ?, category_id = ?, published_year = ?,
                       edition = ?, pub_place = ?, extent = ?, series = ?, language = ?,
                       material_type = ?, control_number = ?, summary = ?, marc_xml = ?,
                       call_number = ?, classification_scheme = ?
                 WHERE id = ?
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bindBook(ps, book);
            ps.setLong(17, book.getId());
            ps.executeUpdate();
        }
    }

    /** Binds the 16 shared book columns (1..16); callers set the trailing column (17). */
    private void bindBook(PreparedStatement ps, Book book) throws SQLException {
        ps.setString(1, book.getIsbn());
        ps.setString(2, book.getTitle());
        setNullableLong(ps, 3, book.getPublisherId());
        setNullableLong(ps, 4, book.getCategoryId());
        setNullableInt(ps, 5, book.getPublishedYear());
        ps.setString(6, book.getEdition());
        ps.setString(7, book.getPubPlace());
        ps.setString(8, book.getExtent());
        ps.setString(9, book.getSeries());
        ps.setString(10, book.getLanguage());
        ps.setString(11, book.getMaterialType() == null ? null : book.getMaterialType().name());
        ps.setString(12, book.getControlNumber());
        ps.setString(13, book.getSummary());
        ps.setString(14, book.getMarcXml());
        ps.setString(15, book.getCallNumber());
        ps.setString(16, book.getClassificationScheme() == null
                ? null : book.getClassificationScheme().name());
    }

    /** Replaces a book's link rows in a junction table with the given ids. */
    private void syncLinks(Connection c, String table, String column, long bookId, List<Long> ids)
            throws SQLException {
        try (PreparedStatement delete = c.prepareStatement(
                "DELETE FROM " + table + " WHERE book_id = ?")) {
            delete.setLong(1, bookId);
            delete.executeUpdate();
        }
        if (ids.isEmpty()) {
            return;
        }
        try (PreparedStatement insert = c.prepareStatement(
                "INSERT INTO " + table + " (book_id, " + column + ") VALUES (?, ?)")) {
            for (Long id : ids) {
                insert.setLong(1, bookId);
                insert.setLong(2, id);
                insert.addBatch();
            }
            insert.executeBatch();
        }
    }

    @Override
    public Optional<Book> findById(Long id) {
        return findOne("SELECT * FROM books WHERE id = ?", ps -> ps.setLong(1, id));
    }

    @Override
    public Optional<Book> findByIsbn(String isbn) {
        return findOne("SELECT * FROM books WHERE isbn = ?", ps -> ps.setString(1, isbn));
    }

    @Override
    public List<Book> searchByTitle(String fragment) {
        return findMany("SELECT * FROM books WHERE title ILIKE '%' || ? || '%' ORDER BY title",
                ps -> ps.setString(1, fragment));
    }

    @Override
    public List<Book> findAll() {
        return findMany("SELECT * FROM books ORDER BY title", ps -> { });
    }

    @Override
    public void deleteById(Long id) {
        // book_authors, book_subjects and book_copies cascade via ON DELETE CASCADE.
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM books WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete book id=" + id, e);
        }
    }

    private Optional<Book> findOne(String sql, StatementBinder binder) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                Book book = mapRow(rs);
                loadLinks(c, book);
                return Optional.of(book);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query book", e);
        }
    }

    private List<Book> findMany(String sql, StatementBinder binder) {
        List<Book> books = new ArrayList<>();
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    books.add(mapRow(rs));
                }
            }
            for (Book book : books) {
                loadLinks(c, book);
            }
            return books;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to list books", e);
        }
    }

    private void loadLinks(Connection c, Book book) throws SQLException {
        book.getAuthorIds().addAll(loadIds(c, "book_authors", "author_id", book.getId()));
        book.getSubjectIds().addAll(loadIds(c, "book_subjects", "subject_id", book.getId()));
    }

    private List<Long> loadIds(Connection c, String table, String column, long bookId) throws SQLException {
        List<Long> ids = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT " + column + " FROM " + table + " WHERE book_id = ? ORDER BY " + column)) {
            ps.setLong(1, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getLong(column));
                }
            }
        }
        return ids;
    }

    private Book mapRow(ResultSet rs) throws SQLException {
        Book book = new Book(
                rs.getLong("id"),
                rs.getString("isbn"),
                rs.getString("title"),
                rs.getObject("publisher_id", Long.class),
                rs.getObject("category_id", Long.class),
                rs.getObject("published_year", Integer.class),
                rs.getTimestamp("created_at").toLocalDateTime());
        book.setEdition(rs.getString("edition"));
        book.setPubPlace(rs.getString("pub_place"));
        book.setExtent(rs.getString("extent"));
        book.setSeries(rs.getString("series"));
        book.setLanguage(rs.getString("language"));
        String materialType = rs.getString("material_type");
        book.setMaterialType(materialType != null ? MaterialType.valueOf(materialType) : null);
        book.setControlNumber(rs.getString("control_number"));
        book.setSummary(rs.getString("summary"));
        book.setMarcXml(rs.getString("marc_xml"));
        book.setCallNumber(rs.getString("call_number"));
        String scheme = rs.getString("classification_scheme");
        book.setClassificationScheme(scheme != null ? ClassificationScheme.valueOf(scheme) : null);
        return book;
    }

    private void setNullableLong(PreparedStatement ps, int index, Long value) throws SQLException {
        if (value != null) {
            ps.setLong(index, value);
        } else {
            ps.setNull(index, Types.BIGINT);
        }
    }

    private void setNullableInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value != null) {
            ps.setInt(index, value);
        } else {
            ps.setNull(index, Types.INTEGER);
        }
    }

    @FunctionalInterface
    private interface StatementBinder {
        void bind(PreparedStatement ps) throws SQLException;
    }
}
