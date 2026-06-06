package com.justin.libradesk.repository.jdbc;

import com.justin.libradesk.domain.model.Book;
import com.justin.libradesk.infrastructure.database.DatabaseManager;
import com.justin.libradesk.repository.BookRepository;

import java.util.List;
import java.util.Optional;

/**
 * Phase 1 skeleton. Read methods return empty results so the catalog UI can be
 * wired without crashing; writes are intentionally unimplemented until Phase 2.
 *
 * TODO(phase2): implement full CRUD plus book_authors join handling, mirroring
 * the pattern in {@link JdbcUserRepository} / {@link JdbcPatronRepository}.
 */
public class JdbcBookRepository implements BookRepository {

    private final DatabaseManager db;

    public JdbcBookRepository(DatabaseManager db) {
        this.db = db;
    }

    @Override
    public Book save(Book book) {
        throw new UnsupportedOperationException("JdbcBookRepository.save not implemented in Phase 1");
    }

    @Override
    public Optional<Book> findById(Long id) {
        return Optional.empty();
    }

    @Override
    public Optional<Book> findByIsbn(String isbn) {
        return Optional.empty();
    }

    @Override
    public List<Book> searchByTitle(String fragment) {
        return List.of();
    }

    @Override
    public List<Book> findAll() {
        return List.of();
    }

    @Override
    public void deleteById(Long id) {
        throw new UnsupportedOperationException("JdbcBookRepository.deleteById not implemented in Phase 1");
    }
}
