package com.justin.libradesk.repository;

import com.justin.libradesk.domain.model.Loan;

import java.util.List;

public interface LoanRepository extends Repository<Loan, Long> {

    /** @return the number of currently active (unreturned) loans for a patron. */
    int countActiveByPatron(Long patronId);

    List<Loan> findActiveByPatron(Long patronId);

    /** @return all active loans whose due date is before {@code now}. */
    List<Loan> findOverdue();
}
