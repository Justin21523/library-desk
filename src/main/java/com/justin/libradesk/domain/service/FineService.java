package com.justin.libradesk.domain.service;

import com.justin.libradesk.domain.enumtype.FeeType;
import com.justin.libradesk.domain.enumtype.FineStatus;
import com.justin.libradesk.domain.model.Fine;
import com.justin.libradesk.domain.model.Payment;
import com.justin.libradesk.repository.FineRepository;
import com.justin.libradesk.repository.PaymentRepository;
import com.justin.libradesk.validation.ValidationException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Billing service: raises charges (overdue, lost-item, processing, damage),
 * records full or partial payments and waivers against them, and reports a
 * patron's outstanding balance. The legacy per-day rate ({@code fine.per.day})
 * is still used by {@link #chargeOverdue}; policy-driven, capped overdue amounts
 * are computed by the circulation service and raised through {@link #charge}.
 */
public class FineService {

    private static final BigDecimal DEFAULT_PER_DAY = new BigDecimal("0.50");

    private final FineRepository fineRepository;
    private final PaymentRepository paymentRepository;
    private final SettingsService settingsService;
    private final AuditLogService auditLogService;
    private final Clock clock;

    public FineService(FineRepository fineRepository, PaymentRepository paymentRepository,
                       SettingsService settingsService, AuditLogService auditLogService, Clock clock) {
        this.fineRepository = fineRepository;
        this.paymentRepository = paymentRepository;
        this.settingsService = settingsService;
        this.auditLogService = auditLogService;
        this.clock = clock;
    }

    /**
     * Raises an UNPAID charge of a given type.
     *
     * @return the created fine, or {@code null} when {@code amount <= 0}
     */
    public Fine charge(Long patronId, Long loanId, BigDecimal amount, FeeType feeType, String actor) {
        if (amount == null || amount.signum() <= 0) {
            return null;
        }
        Fine fine = new Fine(null, patronId, loanId, amount, FineStatus.UNPAID,
                LocalDateTime.now(clock), null);
        fine.setFeeType(feeType);
        Fine saved = fineRepository.save(fine);
        auditLogService.record(actor, "FINE_CHARGED", "Fine", saved.getId(),
                feeType + " patron=" + patronId + " amount=" + amount);
        return saved;
    }

    /** Raises an overdue charge of {@code overdueDays × fine.per.day} (legacy flat rate). */
    public Fine chargeOverdue(Long patronId, Long loanId, long overdueDays, String actor) {
        if (overdueDays <= 0) {
            return null;
        }
        BigDecimal perDay = settingsService.getBigDecimal("fine.per.day", DEFAULT_PER_DAY);
        return charge(patronId, loanId, perDay.multiply(BigDecimal.valueOf(overdueDays)),
                FeeType.OVERDUE, actor);
    }

    public BigDecimal unpaidTotal(Long patronId) {
        return fineRepository.unpaidTotal(patronId);
    }

    /** @return a patron's outstanding balance (sum of unpaid fine balances). */
    public BigDecimal balanceForPatron(Long patronId) {
        return fineRepository.unpaidTotal(patronId);
    }

    public List<Fine> listUnpaid() {
        return fineRepository.findUnpaid();
    }

    public List<Fine> listByPatron(Long patronId) {
        return fineRepository.findByPatron(patronId);
    }

    public List<Payment> payments(Long fineId) {
        return paymentRepository.findByFine(fineId);
    }

    /** Pays the full outstanding balance of a fine (cash). */
    public Fine pay(Long fineId, String actor) {
        Fine fine = require(fineId);
        return pay(fineId, fine.balance(), "CASH", actor);
    }

    /**
     * Records a (possibly partial) payment against a fine. When the cumulative
     * paid amount reaches the fine total the fine becomes PAID; otherwise it
     * stays UNPAID with a reduced balance.
     */
    public Fine pay(Long fineId, BigDecimal amount, String method, String actor) {
        Fine fine = require(fineId);
        if (amount == null || amount.signum() <= 0) {
            throw new ValidationException("Payment amount must be positive");
        }
        if (amount.compareTo(fine.balance()) > 0) {
            throw new ValidationException("Payment exceeds the outstanding balance of " + fine.balance());
        }
        LocalDateTime now = LocalDateTime.now(clock);
        paymentRepository.save(new Payment(null, fineId, amount, method, null, now, actor));
        fine.setPaidAmount(fine.getPaidAmount().add(amount));
        if (fine.balance().signum() <= 0) {
            fine.setStatus(FineStatus.PAID);
            fine.setSettledAt(now);
        }
        Fine saved = fineRepository.save(fine);
        auditLogService.record(actor, "FINE_PAYMENT", "Fine", fineId, "amount=" + amount);
        return saved;
    }

    /** Waives the remaining balance of a fine, recording the reason. */
    public Fine waive(Long fineId, String reason, String actor) {
        Fine fine = require(fineId);
        LocalDateTime now = LocalDateTime.now(clock);
        BigDecimal balance = fine.balance();
        if (balance.signum() > 0) {
            paymentRepository.save(new Payment(null, fineId, balance, "WAIVE", reason, now, actor));
            fine.setPaidAmount(fine.getPaidAmount().add(balance));
        }
        fine.setStatus(FineStatus.WAIVED);
        fine.setSettledAt(now);
        fine.setNote(reason);
        Fine saved = fineRepository.save(fine);
        auditLogService.record(actor, "FINE_WAIVED", "Fine", fineId, reason);
        return saved;
    }

    /** Waives a fine without a recorded reason (convenience). */
    public Fine waive(Long fineId, String actor) {
        return waive(fineId, null, actor);
    }

    private Fine require(Long fineId) {
        Fine fine = fineRepository.findById(fineId)
                .orElseThrow(() -> new ValidationException("Fine not found: " + fineId));
        if (fine.getStatus() != FineStatus.UNPAID) {
            throw new ValidationException("Fine is already settled (" + fine.getStatus() + ")");
        }
        return fine;
    }
}
