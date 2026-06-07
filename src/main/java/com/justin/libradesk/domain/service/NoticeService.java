package com.justin.libradesk.domain.service;

import com.justin.libradesk.domain.enumtype.LoanStatus;
import com.justin.libradesk.domain.enumtype.ReservationStatus;
import com.justin.libradesk.domain.model.Loan;
import com.justin.libradesk.domain.model.Patron;
import com.justin.libradesk.domain.model.Reservation;
import com.justin.libradesk.infrastructure.notify.Mailer;
import com.justin.libradesk.repository.BookCopyRepository;
import com.justin.libradesk.repository.BookRepository;
import com.justin.libradesk.repository.LoanRepository;
import com.justin.libradesk.repository.PatronRepository;
import com.justin.libradesk.repository.ReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Generates and delivers patron notices via the {@link Mailer} seam: due-soon
 * reminders, overdue notices, and hold-ready notices. Notices are skipped for
 * patrons without an email address. Driven on a schedule by
 * {@code MaintenanceScheduler}.
 */
public class NoticeService {

    private static final Logger log = LoggerFactory.getLogger(NoticeService.class);

    private final LoanRepository loanRepository;
    private final ReservationRepository reservationRepository;
    private final PatronRepository patronRepository;
    private final BookCopyRepository bookCopyRepository;
    private final BookRepository bookRepository;
    private final SettingsService settingsService;
    private final AuditLogService auditLogService;
    private final Mailer mailer;
    private final Clock clock;

    public NoticeService(LoanRepository loanRepository,
                         ReservationRepository reservationRepository,
                         PatronRepository patronRepository,
                         BookCopyRepository bookCopyRepository,
                         BookRepository bookRepository,
                         SettingsService settingsService,
                         AuditLogService auditLogService,
                         Mailer mailer,
                         Clock clock) {
        this.loanRepository = loanRepository;
        this.reservationRepository = reservationRepository;
        this.patronRepository = patronRepository;
        this.bookCopyRepository = bookCopyRepository;
        this.bookRepository = bookRepository;
        this.settingsService = settingsService;
        this.auditLogService = auditLogService;
        this.mailer = mailer;
        this.clock = clock;
    }

    /** Reminds patrons of loans due within {@code notice.due.soon.days}. */
    public int sendDueSoon() {
        int days = settingsService.getInt("notice.due.soon.days", 2);
        LocalDateTime now = LocalDateTime.now(clock);
        List<Loan> due = loanRepository.findActiveDueBetween(now, now.plusDays(days));
        int sent = 0;
        for (Loan loan : due) {
            sent += notifyLoan(loan, "Item due soon",
                    "is due on " + loan.getDueAt().toLocalDate());
        }
        return audited("DUE_SOON", sent);
    }

    /** Notifies patrons of items now overdue. */
    public int sendOverdue() {
        int sent = 0;
        for (Loan loan : loanRepository.findByStatus(LoanStatus.OVERDUE)) {
            sent += notifyLoan(loan, "Item overdue",
                    "was due on " + loan.getDueAt().toLocalDate() + " and is now overdue");
        }
        return audited("OVERDUE", sent);
    }

    /** Notifies patrons whose held item is ready for pickup. */
    public int sendHoldReady() {
        int sent = 0;
        for (Reservation r : reservationRepository.findAllActive()) {
            if (r.getStatus() != ReservationStatus.READY) {
                continue;
            }
            Patron patron = patronRepository.findById(r.getPatronId()).orElse(null);
            if (patron == null || isBlank(patron.getEmail())) {
                continue;
            }
            String title = bookRepository.findById(r.getBookId()).map(b -> b.getTitle()).orElse("your hold");
            mailer.send(patron.getEmail(), "Hold ready for pickup",
                    patron.getFullName() + ", \"" + title + "\" is ready for pickup.");
            sent++;
        }
        return audited("HOLD_READY", sent);
    }

    private int notifyLoan(Loan loan, String subject, String detail) {
        Patron patron = patronRepository.findById(loan.getPatronId()).orElse(null);
        if (patron == null || isBlank(patron.getEmail())) {
            return 0;
        }
        String title = bookCopyRepository.findById(loan.getCopyId())
                .flatMap(copy -> bookRepository.findById(copy.getBookId()))
                .map(b -> b.getTitle())
                .orElse("your item");
        mailer.send(patron.getEmail(), subject,
                patron.getFullName() + ", \"" + title + "\" " + detail + ".");
        return 1;
    }

    private int audited(String kind, int sent) {
        if (sent > 0) {
            auditLogService.record("system", "NOTICE_SENT", "Notice", null, kind + " x" + sent);
            log.info("Sent {} {} notice(s)", sent, kind);
        }
        return sent;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
