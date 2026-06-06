package com.justin.libradesk.dto;

import java.time.LocalDateTime;

/**
 * Lightweight result returned to the UI after a successful checkout, so the
 * controller does not need to touch the domain {@code Loan} entity directly.
 */
public record LoanResult(Long loanId, Long copyId, Long patronId, LocalDateTime dueAt) {
}
