package com.justin.libradesk.repository.jdbc;

import com.justin.libradesk.domain.enumtype.Frequency;
import com.justin.libradesk.domain.enumtype.IssueStatus;
import com.justin.libradesk.domain.enumtype.SubscriptionStatus;
import com.justin.libradesk.domain.model.Book;
import com.justin.libradesk.domain.model.SerialIssue;
import com.justin.libradesk.domain.model.Subscription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcSerialsRepositoryIT extends AbstractRepositoryIT {

    private static final LocalDate START = LocalDate.of(2026, 1, 1);

    private JdbcSubscriptionRepository subscriptions;
    private JdbcSerialIssueRepository issues;
    private Long bookId;

    @BeforeEach
    void setUp() {
        subscriptions = new JdbcSubscriptionRepository(databaseManager);
        issues = new JdbcSerialIssueRepository(databaseManager);
        bookId = new JdbcBookRepository(databaseManager)
                .save(new Book(null, "isbn", "Serial", null, null, null, FIXED)).getId();
    }

    private Long subscription() {
        return subscriptions.save(new Subscription(null, bookId, "Journal", Frequency.MONTHLY,
                SubscriptionStatus.ACTIVE, START, START)).id();
    }

    @Test
    void subscriptionRoundTripsAndFindsActive() {
        subscription();
        assertEquals(1, subscriptions.findActive().size());
        assertEquals(Frequency.MONTHLY, subscriptions.findAll().get(0).frequency());
    }

    @Test
    void issuesRoundTripAndExpectedBeforeFiltersByDate() {
        Long subId = subscription();
        issues.save(new SerialIssue(null, subId, "Vol 1", START.minusDays(40), null, IssueStatus.EXPECTED));
        issues.save(new SerialIssue(null, subId, "Vol 2", START.plusDays(40), null, IssueStatus.EXPECTED));

        assertEquals(2, issues.findBySubscription(subId).size());
        assertEquals(1, issues.findExpectedBefore(START).size());
    }

    @Test
    void receivedDateRoundTrips() {
        Long subId = subscription();
        SerialIssue saved = issues.save(
                new SerialIssue(null, subId, "Vol 1", START, START.plusDays(2), IssueStatus.RECEIVED));
        SerialIssue read = issues.findById(saved.id()).orElseThrow();
        assertEquals(IssueStatus.RECEIVED, read.status());
        assertEquals(START.plusDays(2), read.receivedDate());
        assertTrue(read.id() != null);
    }
}
