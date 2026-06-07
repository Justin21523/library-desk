package com.justin.libradesk.dto;

import com.justin.libradesk.domain.model.Fine;
import com.justin.libradesk.domain.model.Loan;
import com.justin.libradesk.domain.model.Patron;
import com.justin.libradesk.domain.model.Reservation;

import java.math.BigDecimal;
import java.util.List;

/**
 * A snapshot of a patron's circulation standing: their outstanding loans, active
 * holds, unpaid fines and balance, plus any {@code blocks} (reasons the patron
 * may not borrow). An empty {@code blocks} list means the patron is in good
 * standing.
 */
public record PatronAccount(Patron patron,
                            List<Loan> loans,
                            int overdueCount,
                            List<Reservation> holds,
                            List<Fine> fines,
                            BigDecimal balance,
                            List<String> blocks) {

    public boolean blocked() {
        return !blocks.isEmpty();
    }
}
