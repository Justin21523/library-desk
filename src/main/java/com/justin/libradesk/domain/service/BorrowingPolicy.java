package com.justin.libradesk.domain.service;

import com.justin.libradesk.domain.enumtype.PatronStatus;
import com.justin.libradesk.domain.model.Patron;

/**
 * Pure business rule deciding whether a patron may borrow another item.
 *
 * <p>Intentionally free of database, configuration, and framework dependencies
 * so it can be unit-tested directly (see {@code BorrowingPolicyTest}). The
 * caller supplies the current active-loan count and the patron's borrowing
 * limit; this class only encodes the decision.
 */
public final class BorrowingPolicy {

    /** Outcome of a borrowing check. When {@code allowed} is false, {@code reason} explains why. */
    public record Decision(boolean allowed, String reason) {

        public static Decision allow() {
            return new Decision(true, null);
        }

        public static Decision deny(String reason) {
            return new Decision(false, reason);
        }
    }

    /**
     * @param patron          the borrower
     * @param activeLoanCount number of items the patron currently has on loan
     * @param borrowLimit     maximum simultaneous loans allowed for this patron
     * @return an allow/deny decision
     */
    public Decision evaluate(Patron patron, int activeLoanCount, int borrowLimit) {
        if (patron == null) {
            return Decision.deny("Patron does not exist");
        }
        if (patron.getStatus() != PatronStatus.ACTIVE) {
            return Decision.deny("Patron is not active (status: " + patron.getStatus() + ")");
        }
        if (activeLoanCount >= borrowLimit) {
            return Decision.deny("Borrowing limit reached (" + activeLoanCount + "/" + borrowLimit + ")");
        }
        return Decision.allow();
    }
}
