package com.justin.libradesk.repository.jdbc;

import com.justin.libradesk.domain.enumtype.CopyStatus;
import com.justin.libradesk.domain.enumtype.LoanStatus;
import com.justin.libradesk.domain.enumtype.PatronStatus;
import com.justin.libradesk.domain.enumtype.PatronType;
import com.justin.libradesk.domain.model.Book;
import com.justin.libradesk.domain.model.BookCopy;
import com.justin.libradesk.domain.model.Loan;
import com.justin.libradesk.domain.model.Patron;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcLoanRepositoryIT extends AbstractRepositoryIT {

    private JdbcLoanRepository loanRepository;
    private Long patronId;
    private Long copyId;

    @BeforeEach
    void setUp() {
        loanRepository = new JdbcLoanRepository(databaseManager);
        Long bookId = new JdbcBookRepository(databaseManager)
                .save(new Book(null, "isbn", "Book", null, null, null, FIXED)).getId();
        copyId = new JdbcBookCopyRepository(databaseManager)
                .save(new BookCopy(null, bookId, "BC", CopyStatus.ON_LOAN, "A1", FIXED)).getId();
        patronId = new JdbcPatronRepository(databaseManager)
                .save(new Patron(null, "M1", "Borrower", null, null,
                        PatronType.STUDENT, PatronStatus.ACTIVE, FIXED)).getId();
    }

    private Loan activeLoan(LocalDateTime dueAt) {
        return new Loan(null, copyId, patronId, FIXED, dueAt, null, LoanStatus.ACTIVE);
    }

    @Test
    void savesAndReadsBackIncludingNullReturnedAt() {
        Loan saved = loanRepository.save(activeLoan(FIXED.plusDays(14)));

        assertTrue(saved.getId() != null);
        Loan found = loanRepository.findById(saved.getId()).orElseThrow();
        assertEquals(LoanStatus.ACTIVE, found.getStatus());
        assertEquals(FIXED.plusDays(14), found.getDueAt());
        assertEquals(null, found.getReturnedAt());
    }

    @Test
    void activeFindersTrackOpenLoans() {
        loanRepository.save(activeLoan(FIXED.plusDays(14)));

        assertEquals(1, loanRepository.countActiveByPatron(patronId));
        assertEquals(1, loanRepository.findActiveByPatron(patronId).size());
        assertTrue(loanRepository.findActiveByCopy(copyId).isPresent());
    }

    @Test
    void returningLoanRemovesItFromActiveQueries() {
        Loan saved = loanRepository.save(activeLoan(FIXED.plusDays(14)));

        saved.setStatus(LoanStatus.RETURNED);
        saved.setReturnedAt(FIXED.plusDays(3));
        loanRepository.save(saved);

        assertEquals(0, loanRepository.countActiveByPatron(patronId));
        assertTrue(loanRepository.findActiveByCopy(copyId).isEmpty());
        assertEquals(FIXED.plusDays(3),
                loanRepository.findById(saved.getId()).orElseThrow().getReturnedAt());
    }

    @Test
    void findOverdueReturnsActiveLoansPastDueDate() {
        loanRepository.save(activeLoan(FIXED));                 // FIXED (2026-01-01) is in the past
        loanRepository.save(activeLoan(FIXED.plusYears(5)));    // far future, not overdue

        assertEquals(1, loanRepository.findOverdue().size());
    }

    @Test
    void renewalCountRoundTripsAndDefaultsToZero() {
        Loan saved = loanRepository.save(activeLoan(FIXED.plusDays(14)));
        assertEquals(0, loanRepository.findById(saved.getId()).orElseThrow().getRenewalCount());

        saved.setRenewalCount(2);
        loanRepository.save(saved);

        assertEquals(2, loanRepository.findById(saved.getId()).orElseThrow().getRenewalCount());
    }

    @Test
    void countByPatronAndStatusCountsMatchingLoans() {
        loanRepository.save(activeLoan(FIXED.plusDays(14)));
        Loan overdue = activeLoan(FIXED.plusDays(7));
        overdue.setStatus(LoanStatus.OVERDUE);
        loanRepository.save(overdue);

        assertEquals(1, loanRepository.countByPatronAndStatus(patronId, LoanStatus.OVERDUE));
        assertEquals(1, loanRepository.countByPatronAndStatus(patronId, LoanStatus.ACTIVE));
    }
}
