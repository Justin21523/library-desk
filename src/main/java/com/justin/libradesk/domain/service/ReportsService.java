package com.justin.libradesk.domain.service;

import com.justin.libradesk.domain.model.Loan;
import com.justin.libradesk.repository.LoanRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Read-only queries for the Reports screen and CSV exports.
 */
public class ReportsService {

    private final LoanRepository loanRepository;
    private final Clock clock;

    public ReportsService(LoanRepository loanRepository, Clock clock) {
        this.loanRepository = loanRepository;
        this.clock = clock;
    }

    /**
     * @return every unreturned loan whose due date has passed, regardless of
     *         whether the overdue sweep has already flipped its status. Computed
     *         from {@link Loan#isOverdue} so it is correct between sweeps.
     */
    public List<Loan> overdueLoans() {
        LocalDateTime now = LocalDateTime.now(clock);
        return loanRepository.findAll().stream()
                .filter(loan -> loan.isOverdue(now))
                .toList();
    }

    /** @return every unreturned loan (active or overdue). */
    public List<Loan> outstandingLoans() {
        return loanRepository.findAll().stream()
                .filter(loan -> loan.getReturnedAt() == null)
                .toList();
    }
}
