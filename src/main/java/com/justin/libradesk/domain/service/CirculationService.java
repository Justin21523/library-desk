package com.justin.libradesk.domain.service;

import com.justin.libradesk.config.AppConfig;
import com.justin.libradesk.domain.enumtype.CopyStatus;
import com.justin.libradesk.domain.enumtype.LoanStatus;
import com.justin.libradesk.domain.enumtype.PatronType;
import com.justin.libradesk.domain.model.BookCopy;
import com.justin.libradesk.domain.model.Loan;
import com.justin.libradesk.domain.model.Patron;
import com.justin.libradesk.dto.LoanResult;
import com.justin.libradesk.repository.BookCopyRepository;
import com.justin.libradesk.repository.LoanRepository;
import com.justin.libradesk.repository.PatronRepository;
import com.justin.libradesk.validation.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Coordinates the loan/return workflow. This is where the borrowing rules are
 * enforced before any state changes are written:
 * <ul>
 *   <li>only {@link CopyStatus#AVAILABLE} copies can be loaned;</li>
 *   <li>a non-active (e.g. suspended) patron cannot borrow;</li>
 *   <li>a patron cannot exceed the borrowing limit for their type.</li>
 * </ul>
 * The decision itself is delegated to {@link BorrowingPolicy}; this service
 * gathers the inputs, applies side effects, and records the audit trail.
 */
public class CirculationService {

    private static final Logger log = LoggerFactory.getLogger(CirculationService.class);

    private final PatronRepository patronRepository;
    private final BookCopyRepository bookCopyRepository;
    private final LoanRepository loanRepository;
    private final AuditLogService auditLogService;
    private final BorrowingPolicy borrowingPolicy;
    private final AppConfig config;
    private final Clock clock;

    public CirculationService(PatronRepository patronRepository,
                              BookCopyRepository bookCopyRepository,
                              LoanRepository loanRepository,
                              AuditLogService auditLogService,
                              BorrowingPolicy borrowingPolicy,
                              AppConfig config,
                              Clock clock) {
        this.patronRepository = patronRepository;
        this.bookCopyRepository = bookCopyRepository;
        this.loanRepository = loanRepository;
        this.auditLogService = auditLogService;
        this.borrowingPolicy = borrowingPolicy;
        this.config = config;
        this.clock = clock;
    }

    /**
     * Issues a copy to a patron.
     *
     * @param patronId the borrower's id
     * @param copyId   the physical copy to lend
     * @param actor    username of the staff member performing the checkout
     * @return a summary of the created loan
     * @throws ValidationException if the patron/copy is missing or a rule is violated
     */
    public LoanResult checkout(Long patronId, Long copyId, String actor) {
        Patron patron = patronRepository.findById(patronId)
                .orElseThrow(() -> new ValidationException("Patron not found: " + patronId));
        BookCopy copy = bookCopyRepository.findById(copyId)
                .orElseThrow(() -> new ValidationException("Book copy not found: " + copyId));

        if (!copy.isAvailable()) {
            throw new ValidationException("Copy is not available (status: " + copy.getStatus() + ")");
        }

        int activeLoans = loanRepository.countActiveByPatron(patronId);
        int limit = borrowLimitFor(patron.getPatronType());

        BorrowingPolicy.Decision decision = borrowingPolicy.evaluate(patron, activeLoans, limit);
        if (!decision.allowed()) {
            throw new ValidationException(decision.reason());
        }

        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime dueAt = now.plusDays(config.getInt("loan.period.days", 14));

        Loan loan = new Loan(null, copyId, patronId, now, dueAt, null, LoanStatus.ACTIVE);
        Loan savedLoan = loanRepository.save(loan);

        copy.setStatus(CopyStatus.ON_LOAN);
        bookCopyRepository.save(copy);

        auditLogService.record(actor, "LOAN_CREATED", "Loan", savedLoan.getId(),
                "patron=" + patronId + " copy=" + copyId);
        log.info("Loan {} created: patron={} copy={} due={}", savedLoan.getId(), patronId, copyId, dueAt);

        return new LoanResult(savedLoan.getId(), copyId, patronId, dueAt);
    }

    /** Resolves the configured borrowing limit for a patron type. */
    private int borrowLimitFor(PatronType type) {
        String key = "borrow.limit." + type.name().toLowerCase();
        return config.getInt(key);
    }

    // TODO(phase2): return workflow (mark loan RETURNED, copy AVAILABLE, trigger
    // next reservation) and overdue detection sweep.
}
