package com.justin.libradesk.domain.model;

import com.justin.libradesk.domain.enumtype.IssueStatus;

import java.time.LocalDate;

/**
 * One issue of a {@link Subscription}: its enumeration, expected/received dates,
 * and {@link IssueStatus}. {@code id} is {@code null} until persisted.
 */
public record SerialIssue(Long id, Long subscriptionId, String enumeration,
                          LocalDate expectedDate, LocalDate receivedDate, IssueStatus status) {
}
