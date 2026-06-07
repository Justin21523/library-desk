package com.justin.libradesk.domain.service;

import com.justin.libradesk.domain.enumtype.Frequency;
import com.justin.libradesk.domain.enumtype.IssueStatus;
import com.justin.libradesk.domain.enumtype.SubscriptionStatus;
import com.justin.libradesk.domain.model.SerialIssue;
import com.justin.libradesk.domain.model.Subscription;
import com.justin.libradesk.repository.SerialIssueRepository;
import com.justin.libradesk.repository.SubscriptionRepository;
import com.justin.libradesk.validation.ValidationException;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * Serials control: subscriptions, predicted issues, issue check-in, and claiming
 * of late issues. The predicted date of the next issue advances by the
 * subscription's {@link Frequency}.
 */
public class SerialsService {

    private final SubscriptionRepository subscriptionRepository;
    private final SerialIssueRepository serialIssueRepository;
    private final AuditLogService auditLogService;
    private final Clock clock;

    public SerialsService(SubscriptionRepository subscriptionRepository,
                          SerialIssueRepository serialIssueRepository,
                          AuditLogService auditLogService, Clock clock) {
        this.subscriptionRepository = subscriptionRepository;
        this.serialIssueRepository = serialIssueRepository;
        this.auditLogService = auditLogService;
        this.clock = clock;
    }

    public Subscription subscribe(Long bookId, String label, Frequency frequency,
                                  LocalDate startDate, String actor) {
        if (label == null || label.isBlank()) {
            throw new ValidationException("Subscription label is required");
        }
        Subscription saved = subscriptionRepository.save(new Subscription(null, bookId, label.trim(),
                frequency, SubscriptionStatus.ACTIVE, startDate, startDate));
        auditLogService.record(actor, "SUBSCRIPTION_CREATED", "Subscription", saved.id(), label);
        return saved;
    }

    /** Records the next expected issue and advances the subscription's predicted date. */
    public SerialIssue expectNext(Long subscriptionId, String enumeration, String actor) {
        Subscription sub = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ValidationException("Subscription not found: " + subscriptionId));
        LocalDate expected = sub.nextExpected() != null ? sub.nextExpected() : LocalDate.now(clock);
        SerialIssue issue = serialIssueRepository.save(new SerialIssue(null, subscriptionId,
                enumeration, expected, null, IssueStatus.EXPECTED));
        LocalDate advanced = expected.plusDays(sub.frequency().days());
        subscriptionRepository.save(new Subscription(sub.id(), sub.bookId(), sub.label(),
                sub.frequency(), sub.status(), sub.startDate(), advanced));
        auditLogService.record(actor, "ISSUE_EXPECTED", "SerialIssue", issue.id(), enumeration);
        return issue;
    }

    /** Marks an issue received. */
    public SerialIssue checkIn(Long issueId, String actor) {
        SerialIssue issue = serialIssueRepository.findById(issueId)
                .orElseThrow(() -> new ValidationException("Issue not found: " + issueId));
        if (issue.status() == IssueStatus.RECEIVED) {
            throw new ValidationException("Issue is already received");
        }
        SerialIssue received = new SerialIssue(issue.id(), issue.subscriptionId(), issue.enumeration(),
                issue.expectedDate(), LocalDate.now(clock), IssueStatus.RECEIVED);
        serialIssueRepository.save(received);
        auditLogService.record(actor, "ISSUE_RECEIVED", "SerialIssue", issueId, issue.enumeration());
        return received;
    }

    /**
     * Flags EXPECTED issues whose expected date has passed as CLAIMED.
     *
     * @return the number of issues claimed
     */
    public int claimLate(String actor) {
        List<SerialIssue> late = serialIssueRepository.findExpectedBefore(LocalDate.now(clock));
        for (SerialIssue issue : late) {
            serialIssueRepository.save(new SerialIssue(issue.id(), issue.subscriptionId(),
                    issue.enumeration(), issue.expectedDate(), null, IssueStatus.CLAIMED));
            auditLogService.record(actor, "ISSUE_CLAIMED", "SerialIssue", issue.id(), issue.enumeration());
        }
        return late.size();
    }

    public List<Subscription> listSubscriptions() {
        return subscriptionRepository.findAll();
    }

    public List<SerialIssue> issues(Long subscriptionId) {
        return serialIssueRepository.findBySubscription(subscriptionId);
    }
}
