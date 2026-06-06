package com.justin.libradesk.dto;

import java.time.LocalDateTime;

/**
 * Result returned to the UI after a successful check-in.
 *
 * @param wasOverdue         the loan was returned past its due date
 * @param heldForReservation the copy was held (status RESERVED) for the next
 *                           patron in the book's reservation queue, rather than
 *                           returned to AVAILABLE
 */
public record ReturnResult(Long loanId, Long copyId, LocalDateTime returnedAt,
                           boolean wasOverdue, boolean heldForReservation) {
}
