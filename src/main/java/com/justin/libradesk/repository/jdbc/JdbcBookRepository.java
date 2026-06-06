package com.justin.libradesk.repository.jdbc;

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
     * Persists the book and its author links atomically: the book row and the
     * {@code book_authors} junction are written inside a single transaction.
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
                syncAuthors(c, book);
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
                INSERT INTO books (isbn, title, publisher_id, category_id, published_year, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, book.getIsbn());
            ps.setString(2, book.getTitle());
            setNullableLong(ps, 3, book.getPublisherId());
            setNullableLong(ps, 4, book.getCategoryId());
            setNullableInt(ps, 5, book.getPublishedYear());
            ps.setTimestamp(6, Timestamp.valueOf(book.getCreatedAt()));
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
                   SET isbn = ?, title = ?, publisher_id = ?, category_id = ?, published_year = ?
                 WHERE id = ?
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, book.getIsbn());
            ps.setString(2, book.getTitle());
            setNullableLong(ps, 3, book.getPublisherId());
            setNullableLong(ps, 4, book.getCategoryId());
            setNullableInt(ps, 5, book.getPublishedYear());
            ps.setLong(6, book.getId());
            ps.executeUpdate();
        }
    }

    /** Replaces the book's author links with the current {@code authorIds}. */
    private void syncAuthors(Connection c, Book book) throws SQLException {
        try (PreparedStatement delete = c.prepareStatement("DELETE FROM book_authors WHERE book_id = ?")) {
            delete.setLong(1, book.getId());
            delete.executeUpdate();
        }
        if (book.getAuthorIds().isEmpty()) {
            return;
        }
        try (PreparedStatement insert = c.prepareStatement(
                "INSERT INTO book_authors (book_id, author_id) VALUES (?, ?)")) {
            for (Long authorId : book.getAuthorIds()) {
                insert.setLong(1, book.getId());
                insert.setLong(2, authorId);
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
        // book_authors and book_copies cascade via ON DELETE CASCADE.
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
                book.getAuthorIds().addAll(loadAuthorIds(c, book.getId()));
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
                book.getAuthorIds().addAll(loadAuthorIds(c, book.getId()));
            }
            return books;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to list books", e);
        }
    }

    private List<Long> loadAuthorIds(Connection c, long bookId) throws SQLException {
        List<Long> authorIds = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT author_id FROM book_authors WHERE book_id = ? ORDER BY author_id")) {
            ps.setLong(1, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    authorIds.add(rs.getLong("author_id"));
                }
            }
        }
        return authorIds;
    }

    private Book mapRow(ResultSet rs) throws SQLException {
        return new Book(
                rs.getLong("id"),
                rs.getString("isbn"),
                rs.getString("title"),
                rs.getObject("publisher_id", Long.class),
                rs.getObject("category_id", Long.class),
                rs.getObject("published_year", Integer.class),
                rs.getTimestamp("created_at").toLocalDateTime());
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
