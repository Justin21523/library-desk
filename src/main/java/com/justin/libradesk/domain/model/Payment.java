package com.justin.libradesk.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A payment recorded against a {@link Fine}. Several partial payments may settle
 * one fine; a waive is recorded as a payment with method {@code WAIVE}.
 * {@code id} is {@code null} until persisted.
 */
public record Payment(Long id, Long fineId, BigDecimal amount, String method,
                      String note, LocalDateTime paidAt, String actor) {
}
