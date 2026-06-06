package com.justin.libradesk.dto;

import java.time.LocalDateTime;

/**
 * Result returned to the UI after a successful check-in. {@code wasOverdue}
 * lets the UI flag late returns (fine handling is a later phase).
 */
public record ReturnResult(Long loanId, Long copyId, LocalDateTime returnedAt, boolean wasOverdue) {
}
