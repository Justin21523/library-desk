package com.justin.libradesk.domain.model;

import com.justin.libradesk.domain.enumtype.Frequency;
import com.justin.libradesk.domain.enumtype.SubscriptionStatus;

import java.time.LocalDate;

/**
 * A serial subscription for a bib. {@code nextExpected} drives issue prediction.
 * {@code id} is {@code null} until persisted.
 */
public record Subscription(Long id, Long bookId, String label, Frequency frequency,
                           SubscriptionStatus status, LocalDate startDate, LocalDate nextExpected) {
}
