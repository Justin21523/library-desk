package com.justin.libradesk.repository;

import com.justin.libradesk.domain.model.Loan;

import java.util.List;
import java.util.Optional;

public interface LoanRepository extends Repository<Loan, Long> {

    /** @return the number of currently active (unreturned) loans for a patron. */
    int countActiveByPatron(Long patronId);

    List<Loan> findActiveByPatron(Long patronId);

    /** @return the open (ACTIVE) loan for a copy, if one exists (used by check-in). */
    Optional<Loan> findActiveByCopy(Long copyId);

    /** @return all active loans whose due date is before now. */
    List<Loan> findOverdue();
}
