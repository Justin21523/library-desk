package com.justin.libradesk.domain.service;

import com.justin.libradesk.domain.enumtype.FineStatus;
import com.justin.libradesk.domain.enumtype.LoanStatus;
import com.justin.libradesk.domain.enumtype.PatronStatus;
import com.justin.libradesk.domain.model.Fine;
import com.justin.libradesk.domain.model.Loan;
import com.justin.libradesk.domain.model.Patron;
import com.justin.libradesk.domain.model.Reservation;
import com.justin.libradesk.dto.PatronAccount;
import com.justin.libradesk.repository.FineRepository;
import com.justin.libradesk.repository.LoanRepository;
import com.justin.libradesk.repository.PatronRepository;
import com.justin.libradesk.repository.ReservationRepository;
import com.justin.libradesk.validation.ValidationException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Aggregates a patron's circulation standing and computes the {@code blocks}
 * that prevent borrowing. Blocks are derived (no stored block table): an expired
 * or suspended membership, an outstanding balance over
 * {@code fine.block.threshold}, or too many overdue items
 * ({@code overdue.block.count}).
 */
public class PatronAccountService {

    private final PatronRepository patronRepository;
    private final LoanRepository loanRepository;
    private final ReservationRepository reservationRepository;
    private final FineRepository fineRepository;
    private final SettingsService settingsService;

    public PatronAccountService(PatronRepository patronRepository,
                                LoanRepository loanRepository,
                                ReservationRepository reservationRepository,
                                FineRepository fineRepository,
                                SettingsService settingsService) {
        this.patronRepository = patronRepository;
        this.loanRepository = loanRepository;
        this.reservationRepository = reservationRepository;
        this.fineRepository = fineRepository;
        this.settingsService = settingsService;
    }

    /** @return the full account snapshot for a patron. */
    public PatronAccount accountFor(Long patronId) {
        Patron patron = patronRepository.findById(patronId)
                .orElseThrow(() -> new ValidationException("Patron not found: " + patronId));
        List<Loan> loans = loanRepository.findActiveByPatron(patronId);
        int overdueCount = loanRepository.countByPatronAndStatus(patronId, LoanStatus.OVERDUE);
        List<Reservation> holds = reservationRepository.findActiveByPatron(patronId);
        List<Fine> fines = fineRepository.findByPatron(patronId).stream()
                .filter(f -> f.getStatus() == FineStatus.UNPAID)
                .toList();
        BigDecimal balance = fineRepository.unpaidTotal(patronId);
        return new PatronAccount(patron, loans, overdueCount, holds, fines, balance,
                blocks(patron, balance, overdueCount));
    }

    /** @return the block reasons preventing this patron from borrowing (empty = none). */
    public List<String> blocks(Long patronId) {
        Patron patron = patronRepository.findById(patronId)
                .orElseThrow(() -> new ValidationException("Patron not found: " + patronId));
        BigDecimal balance = fineRepository.unpaidTotal(patronId);
        int overdueCount = loanRepository.countByPatronAndStatus(patronId, LoanStatus.OVERDUE);
        return blocks(patron, balance, overdueCount);
    }

    private List<String> blocks(Patron patron, BigDecimal balance, int overdueCount) {
        List<String> blocks = new ArrayList<>();
        if (patron.getStatus() == PatronStatus.EXPIRED) {
            blocks.add("Membership expired");
        } else if (patron.getStatus() == PatronStatus.SUSPENDED) {
            blocks.add("Account suspended");
        }
        BigDecimal fineThreshold = settingsService.getBigDecimal("fine.block.threshold", BigDecimal.ZERO);
        if (fineThreshold.signum() > 0 && balance.compareTo(fineThreshold) > 0) {
            blocks.add("Outstanding fines exceed the allowed limit (" + balance + ")");
        }
        int overdueLimit = settingsService.getInt("overdue.block.count", 0);
        if (overdueLimit > 0 && overdueCount >= overdueLimit) {
            blocks.add("Too many overdue items (" + overdueCount + ")");
        }
        return blocks;
    }
}
