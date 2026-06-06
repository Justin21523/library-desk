package com.justin.libradesk.domain.service;

import com.justin.libradesk.domain.enumtype.FineStatus;
import com.justin.libradesk.domain.model.Fine;
import com.justin.libradesk.repository.FineRepository;
import com.justin.libradesk.validation.ValidationException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Manages overdue fines: raising them on late returns, listing them, and
 * settling them (pay or waive). The per-day rate comes from {@link SettingsService}
 * ({@code fine.per.day}).
 */
public class FineService {

    private static final BigDecimal DEFAULT_PER_DAY = new BigDecimal("0.50");

    private final FineRepository fineRepository;
    private final SettingsService settingsService;
    private final AuditLogService auditLogService;
    private final Clock clock;

    public FineService(FineRepository fineRepository, SettingsService settingsService,
                       AuditLogService auditLogService, Clock clock) {
        this.fineRepository = fineRepository;
        this.settingsService = settingsService;
        this.auditLogService = auditLogService;
        this.clock = clock;
    }

    /**
     * Raises an UNPAID fine of {@code overdueDays × fine.per.day}.
     *
     * @return the created fine, or {@code null} if there is nothing to charge
     */
    public Fine chargeOverdue(Long patronId, Long loanId, long overdueDays, String actor) {
        if (overdueDays <= 0) {
            return null;
        }
        BigDecimal perDay = settingsService.getBigDecimal("fine.per.day", DEFAULT_PER_DAY);
        BigDecimal amount = perDay.multiply(BigDecimal.valueOf(overdueDays));
        Fine fine = new Fine(null, patronId, loanId, amount, FineStatus.UNPAID,
                LocalDateTime.now(clock), null);
        Fine saved = fineRepository.save(fine);
        auditLogService.record(actor, "FINE_CHARGED", "Fine", saved.getId(),
                "patron=" + patronId + " amount=" + amount);
        return saved;
    }

    public BigDecimal unpaidTotal(Long patronId) {
        return fineRepository.unpaidTotal(patronId);
    }

    public List<Fine> listUnpaid() {
        return fineRepository.findUnpaid();
    }

    public List<Fine> listByPatron(Long patronId) {
        return fineRepository.findByPatron(patronId);
    }

    public Fine pay(Long fineId, String actor) {
        return settle(fineId, FineStatus.PAID, "FINE_PAID", actor);
    }

    public Fine waive(Long fineId, String actor) {
        return settle(fineId, FineStatus.WAIVED, "FINE_WAIVED", actor);
    }

    private Fine settle(Long fineId, FineStatus status, String action, String actor) {
        Fine fine = fineRepository.findById(fineId)
                .orElseThrow(() -> new ValidationException("Fine not found: " + fineId));
        if (fine.getStatus() != FineStatus.UNPAID) {
            throw new ValidationException("Fine is already settled (" + fine.getStatus() + ")");
        }
        fine.setStatus(status);
        fine.setSettledAt(LocalDateTime.now(clock));
        Fine saved = fineRepository.save(fine);
        auditLogService.record(actor, action, "Fine", fineId, null);
        return saved;
    }
}
