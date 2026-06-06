package com.justin.libradesk.domain.service;

import com.justin.libradesk.domain.enumtype.PatronStatus;
import com.justin.libradesk.domain.enumtype.PatronType;
import com.justin.libradesk.domain.model.Patron;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the core borrowing rule. {@link BorrowingPolicy} is pure, so
 * these tests need no database or mocks.
 */
class BorrowingPolicyTest {

    private final BorrowingPolicy policy = new BorrowingPolicy();

    private Patron patron(PatronStatus status) {
        return new Patron(1L, "M001", "Alice", "alice@example.com", null,
                PatronType.STUDENT, status, LocalDateTime.now());
    }

    @Test
    void allowsActivePatronUnderLimit() {
        BorrowingPolicy.Decision decision = policy.evaluate(patron(PatronStatus.ACTIVE), 2, 5);

        assertTrue(decision.allowed());
    }

    @Test
    void deniesSuspendedPatron() {
        BorrowingPolicy.Decision decision = policy.evaluate(patron(PatronStatus.SUSPENDED), 0, 5);

        assertFalse(decision.allowed());
    }

    @Test
    void deniesExpiredPatron() {
        BorrowingPolicy.Decision decision = policy.evaluate(patron(PatronStatus.EXPIRED), 0, 5);

        assertFalse(decision.allowed());
    }

    @Test
    void deniesWhenAtLimit() {
        BorrowingPolicy.Decision decision = policy.evaluate(patron(PatronStatus.ACTIVE), 5, 5);

        assertFalse(decision.allowed());
    }

    @Test
    void deniesNullPatron() {
        BorrowingPolicy.Decision decision = policy.evaluate(null, 0, 5);

        assertFalse(decision.allowed());
    }
}
