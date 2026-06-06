package com.justin.libradesk.repository.jdbc;

import com.justin.libradesk.domain.model.BookCopy;
import com.justin.libradesk.infrastructure.database.DatabaseManager;
import com.justin.libradesk.repository.BookCopyRepository;

import java.util.List;
import java.util.Optional;

/**
 * Phase 1 skeleton.
 *
 * TODO(phase2): implement CRUD and the status-aware finders used by circulation.
 */
public class JdbcBookCopyRepository implements BookCopyRepository {

    private final DatabaseManager db;

    public JdbcBookCopyRepository(DatabaseManager db) {
        this.db = db;
    }

    @Override
    public BookCopy save(BookCopy copy) {
        throw new UnsupportedOperationException("JdbcBookCopyRepository.save not implemented in Phase 1");
    }

    @Override
    public Optional<BookCopy> findById(Long id) {
        return Optional.empty();
    }

    @Override
    public Optional<BookCopy> findByBarcode(String barcode) {
        return Optional.empty();
    }

    @Override
    public List<BookCopy> findByBookId(Long bookId) {
        return List.of();
    }

    @Override
    public List<BookCopy> findAvailableByBookId(Long bookId) {
        return List.of();
    }

    @Override
    public List<BookCopy> findAll() {
        return List.of();
    }

    @Override
    public void deleteById(Long id) {
        throw new UnsupportedOperationException("JdbcBookCopyRepository.deleteById not implemented in Phase 1");
    }
}
