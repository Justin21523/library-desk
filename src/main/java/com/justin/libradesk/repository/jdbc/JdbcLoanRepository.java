package com.justin.libradesk.repository.jdbc;

import com.justin.libradesk.domain.model.Loan;
import com.justin.libradesk.infrastructure.database.DatabaseManager;
import com.justin.libradesk.repository.LoanRepository;

import java.util.List;
import java.util.Optional;

/**
 * Phase 1 skeleton.
 *
 * TODO(phase2): implement CRUD plus countActiveByPatron/findOverdue used by the
 * circulation and overdue workflows.
 */
public class JdbcLoanRepository implements LoanRepository {

    private final DatabaseManager db;

    public JdbcLoanRepository(DatabaseManager db) {
        this.db = db;
    }

    @Override
    public Loan save(Loan loan) {
        throw new UnsupportedOperationException("JdbcLoanRepository.save not implemented in Phase 1");
    }

    @Override
    public Optional<Loan> findById(Long id) {
        return Optional.empty();
    }

    @Override
    public int countActiveByPatron(Long patronId) {
        return 0;
    }

    @Override
    public List<Loan> findActiveByPatron(Long patronId) {
        return List.of();
    }

    @Override
    public List<Loan> findOverdue() {
        return List.of();
    }

    @Override
    public List<Loan> findAll() {
        return List.of();
    }

    @Override
    public void deleteById(Long id) {
        throw new UnsupportedOperationException("JdbcLoanRepository.deleteById not implemented in Phase 1");
    }
}
