package com.justin.libradesk.domain.service;

import com.justin.libradesk.domain.enumtype.CopyStatus;
import com.justin.libradesk.domain.enumtype.FeeType;
import com.justin.libradesk.domain.enumtype.LoanStatus;
import com.justin.libradesk.domain.enumtype.MaterialType;
import com.justin.libradesk.domain.model.BookCopy;
import com.justin.libradesk.domain.model.CircPolicy;
import com.justin.libradesk.domain.model.Loan;
import com.justin.libradesk.domain.model.Patron;
import com.justin.libradesk.domain.model.Reservation;
import com.justin.libradesk.dto.LoanResult;
import com.justin.libradesk.dto.ReturnResult;
import com.justin.libradesk.repository.BookCopyRepository;
import com.justin.libradesk.repository.BookRepository;
import com.justin.libradesk.repository.LoanRepository;
import com.justin.libradesk.repository.PatronRepository;
import com.justin.libradesk.validation.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Coordinates the loan/return workflow and enforces the borrowing rules before
 * any state changes are written:
 * <ul>
 *   <li>only {@link CopyStatus#AVAILABLE} copies can be loaned;</li>
 *   <li>a patron with any {@link PatronAccountService} block cannot borrow;</li>
 *   <li>loan period, max loans, renewal limit, fine rate/cap and grace come from
 *       the resolved {@link CircPolicy} (patron type × material type);</li>
 *   <li>due dates roll off closed days and overdue fines accrue only on open
 *       days ({@link CalendarService}).</li>
 * </ul>
 * The allow/deny limit check is delegated to {@link BorrowingPolicy}; this
 * service gathers inputs, applies side effects, and records the audit trail.
 */
public class CirculationService {

    private static final Logger log = LoggerFactory.getLogger(CirculationService.class);

    private final PatronRepository patronRepository;
    private final BookCopyRepository bookCopyRepository;
    private final BookRepository bookRepository;
    private final LoanRepository loanRepository;
    private final AuditLogService auditLogService;
    private final ReservationService reservationService;
    private final FineService fineService;
    private final SettingsService settingsService;
    private final CircPolicyService circPolicyService;
    private final CalendarService calendarService;
    private final PatronAccountService patronAccountService;
    private final BorrowingPolicy borrowingPolicy;
    private final Clock clock;

    public CirculationService(PatronRepository patronRepository,
                              BookCopyRepository bookCopyRepository,
                              BookRepository bookRepository,
                              LoanRepository loanRepository,
                              AuditLogService auditLogService,
                              ReservationService reservationService,
                              FineService fineService,
                              SettingsService settingsService,
                              CircPolicyService circPolicyService,
                              CalendarService calendarService,
                              PatronAccountService patronAccountService,
                              BorrowingPolicy borrowingPolicy,
                              Clock clock) {
        this.patronRepository = patronRepository;
        this.bookCopyRepository = bookCopyRepository;
        this.bookRepository = bookRepository;
        this.loanRepository = loanRepository;
        this.auditLogService = auditLogService;
        this.reservationService = reservationService;
        this.fineService = fineService;
        this.settingsService = settingsService;
        this.circPolicyService = circPolicyService;
        this.calendarService = calendarService;
        this.patronAccountService = patronAccountService;
        this.borrowingPolicy = borrowingPolicy;
        this.clock = clock;
    }

    /**
     * Issues a copy to a patron.
     *
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

        List<String> blocks = patronAccountService.blocks(patronId);
        if (!blocks.isEmpty()) {
            throw new ValidationException("Patron is blocked: " + String.join("; ", blocks));
        }

        CircPolicy policy = policyFor(patron, copy);
        int activeLoans = loanRepository.countActiveByPatron(patronId);
        BorrowingPolicy.Decision decision = borrowingPolicy.evaluate(patron, activeLoans, policy.maxLoans());
        if (!decision.allowed()) {
            throw new ValidationException(decision.reason());
        }

        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime dueAt = dueDate(now, policy.loanDays());

        Loan savedLoan = loanRepository.save(
                new Loan(null, copyId, patronId, now, dueAt, null, LoanStatus.ACTIVE));

        copy.setStatus(CopyStatus.ON_LOAN);
        bookCopyRepository.save(copy);

        auditLogService.record(actor, "LOAN_CREATED", "Loan", savedLoan.getId(),
                "patron=" + patronId + " copy=" + copyId);
        log.info("Loan {} created: patron={} copy={} due={}", savedLoan.getId(), patronId, copyId, dueAt);

        return new LoanResult(savedLoan.getId(), copyId, patronId, dueAt);
    }

    /**
     * Checks a copy back in: closes its open loan and makes the copy loanable
     * again. Overdue fines accrue only on open days, minus the policy grace, and
     * are capped by the policy.
     *
     * @throws ValidationException if the copy is missing or has no active loan
     */
    public ReturnResult returnByCopy(Long copyId, String actor) {
        BookCopy copy = bookCopyRepository.findById(copyId)
                .orElseThrow(() -> new ValidationException("Book copy not found: " + copyId));
        Loan loan = loanRepository.findActiveByCopy(copyId)
                .orElseThrow(() -> new ValidationException("No active loan for copy: " + copyId));

        LocalDateTime now = LocalDateTime.now(clock);
        boolean wasOverdue = loan.isOverdue(now);

        loan.setReturnedAt(now);
        loan.setStatus(LoanStatus.RETURNED);
        loanRepository.save(loan);

        if (wasOverdue) {
            chargeOverdue(loan, copy, now, actor);
        }

        // If someone is waiting for this book, hold the copy for the next in line;
        // otherwise it returns to the shelf.
        Optional<Reservation> promoted = reservationService.promoteNext(copy.getBookId(), actor);
        boolean heldForReservation = promoted.isPresent();
        copy.setStatus(heldForReservation ? CopyStatus.RESERVED : CopyStatus.AVAILABLE);
        bookCopyRepository.save(copy);

        auditLogService.record(actor, "LOAN_RETURNED", "Loan", loan.getId(),
                "copy=" + copyId + (wasOverdue ? " (overdue)" : "") + (heldForReservation ? " (held)" : ""));
        log.info("Loan {} returned: copy={} overdue={} held={}", loan.getId(), copyId, wasOverdue,
                heldForReservation);

        return new ReturnResult(loan.getId(), copyId, now, wasOverdue, heldForReservation);
    }

    /**
     * Marks every active loan that is past its due date as OVERDUE. Idempotent
     * and safe to run on a schedule.
     *
     * @return the number of loans newly marked overdue
     */
    public int markOverdueLoans() {
        List<Loan> overdue = loanRepository.findOverdue();
        for (Loan loan : overdue) {
            loan.setStatus(LoanStatus.OVERDUE);
            loanRepository.save(loan);
            auditLogService.record("system", "LOAN_OVERDUE", "Loan", loan.getId(), null);
        }
        if (!overdue.isEmpty()) {
            log.info("Marked {} loan(s) overdue", overdue.size());
        }
        return overdue.size();
    }

    /**
     * Renews a loan one more loan period, provided the renewal limit is not
     * reached and no other patron is waiting for the book.
     *
     * @return the new due date
     * @throws ValidationException if there is no active loan, the limit is hit, or the book is reserved
     */
    public LocalDateTime renew(Long copyId, String actor) {
        BookCopy copy = bookCopyRepository.findById(copyId)
                .orElseThrow(() -> new ValidationException("Book copy not found: " + copyId));
        Loan loan = loanRepository.findActiveByCopy(copyId)
                .orElseThrow(() -> new ValidationException("No active loan for copy: " + copyId));
        if (reservationService.hasPending(copy.getBookId())) {
            throw new ValidationException("Cannot renew: another patron has reserved this book");
        }
        Patron patron = patronRepository.findById(loan.getPatronId())
                .orElseThrow(() -> new ValidationException("Patron not found: " + loan.getPatronId()));
        CircPolicy policy = policyFor(patron, copy);
        if (loan.getRenewalCount() >= policy.renewalLimit()) {
            throw new ValidationException("Renewal limit reached (" + loan.getRenewalCount()
                    + "/" + policy.renewalLimit() + ")");
        }

        LocalDateTime newDueAt = dueDate(LocalDateTime.now(clock), policy.loanDays());
        loan.setDueAt(newDueAt);
        loan.setRenewalCount(loan.getRenewalCount() + 1);
        loan.setStatus(LoanStatus.ACTIVE); // clears OVERDUE if it had been flagged
        loanRepository.save(loan);

        auditLogService.record(actor, "LOAN_RENEWED", "Loan", loan.getId(), "newDue=" + newDueAt);
        log.info("Loan {} renewed: copy={} newDue={}", loan.getId(), copyId, newDueAt);
        return newDueAt;
    }

    /**
     * Declares a copy lost: closes any active loan, marks the copy LOST, and
     * raises a replacement charge plus the configured processing fee.
     */
    public void markLost(Long copyId, BigDecimal replacementCost, String actor) {
        closeForLossOrDamage(copyId, CopyStatus.LOST, LoanStatus.LOST, FeeType.LOST_ITEM,
                replacementCost, true, "COPY_LOST", actor);
    }

    /**
     * Marks a copy damaged: closes any active loan, marks the copy DAMAGED, and
     * raises a damage charge.
     */
    public void markDamaged(Long copyId, BigDecimal damageFee, String actor) {
        closeForLossOrDamage(copyId, CopyStatus.DAMAGED, LoanStatus.RETURNED, FeeType.DAMAGE,
                damageFee, false, "COPY_DAMAGED", actor);
    }

    private void closeForLossOrDamage(Long copyId, CopyStatus copyStatus, LoanStatus loanStatus,
                                      FeeType feeType, BigDecimal fee, boolean addProcessing,
                                      String action, String actor) {
        BookCopy copy = bookCopyRepository.findById(copyId)
                .orElseThrow(() -> new ValidationException("Book copy not found: " + copyId));
        Optional<Loan> active = loanRepository.findActiveByCopy(copyId);
        active.ifPresent(loan -> {
            loan.setReturnedAt(LocalDateTime.now(clock));
            loan.setStatus(loanStatus);
            loanRepository.save(loan);
            fineService.charge(loan.getPatronId(), loan.getId(), fee, feeType, actor);
            if (addProcessing) {
                BigDecimal processing = settingsService.getBigDecimal("fine.processing.fee", BigDecimal.ZERO);
                fineService.charge(loan.getPatronId(), loan.getId(), processing, FeeType.PROCESSING, actor);
            }
        });
        copy.setStatus(copyStatus);
        bookCopyRepository.save(copy);
        auditLogService.record(actor, action, "BookCopy", copyId, "fee=" + fee);
        log.info("Copy {} marked {}", copyId, copyStatus);
    }

    private void chargeOverdue(Loan loan, BookCopy copy, LocalDateTime now, String actor) {
        Patron patron = patronRepository.findById(loan.getPatronId()).orElse(null);
        if (patron == null) {
            return;
        }
        CircPolicy policy = policyFor(patron, copy);
        long openDays = calendarService.openDaysBetween(loan.getDueAt().toLocalDate(), now.toLocalDate());
        long chargeable = Math.max(0, openDays - policy.graceDays());
        if (chargeable <= 0) {
            return;
        }
        BigDecimal amount = policy.finePerDay().multiply(BigDecimal.valueOf(chargeable));
        if (policy.fineCap().signum() > 0 && amount.compareTo(policy.fineCap()) > 0) {
            amount = policy.fineCap();
        }
        fineService.charge(loan.getPatronId(), loan.getId(), amount, FeeType.OVERDUE, actor);
    }

    private CircPolicy policyFor(Patron patron, BookCopy copy) {
        MaterialType material = bookRepository.findById(copy.getBookId())
                .map(b -> b.getMaterialType())
                .orElse(MaterialType.BOOK);
        return circPolicyService.policyFor(patron.getPatronType(), material);
    }

    /** Due date = now + loanDays, rolled forward off any closed day, keeping the time of day. */
    private LocalDateTime dueDate(LocalDateTime now, int loanDays) {
        LocalDate due = calendarService.nextOpenDay(now.toLocalDate().plusDays(loanDays));
        return LocalDateTime.of(due, now.toLocalTime());
    }
}
