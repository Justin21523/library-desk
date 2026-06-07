package com.justin.libradesk.domain.service;

import com.justin.libradesk.domain.enumtype.Frequency;
import com.justin.libradesk.domain.enumtype.IssueStatus;
import com.justin.libradesk.domain.enumtype.SubscriptionStatus;
import com.justin.libradesk.domain.model.SerialIssue;
import com.justin.libradesk.domain.model.Subscription;
import com.justin.libradesk.repository.SerialIssueRepository;
import com.justin.libradesk.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SerialsServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 1, 1);

    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private SerialIssueRepository serialIssueRepository;
    @Mock
    private AuditLogService auditLogService;

    private SerialsService serialsService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(TODAY.atStartOfDay(ZoneId.of("UTC")).toInstant(), ZoneId.of("UTC"));
        serialsService = new SerialsService(subscriptionRepository, serialIssueRepository,
                auditLogService, clock);
    }

    @Test
    void subscribeCreatesActiveSubscription() {
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(i -> i.getArgument(0));

        Subscription sub = serialsService.subscribe(1L, "Journal", Frequency.MONTHLY, TODAY, "admin");

        assertEquals(SubscriptionStatus.ACTIVE, sub.status());
        assertEquals(TODAY, sub.nextExpected());
    }

    @Test
    void expectNextCreatesIssueAndAdvancesPrediction() {
        Subscription sub = new Subscription(3L, 1L, "Journal", Frequency.MONTHLY,
                SubscriptionStatus.ACTIVE, TODAY, TODAY);
        when(subscriptionRepository.findById(3L)).thenReturn(Optional.of(sub));
        when(serialIssueRepository.save(any(SerialIssue.class))).thenAnswer(i -> i.getArgument(0));

        SerialIssue issue = serialsService.expectNext(3L, "Vol 1", "admin");

        assertEquals(IssueStatus.EXPECTED, issue.status());
        assertEquals(TODAY, issue.expectedDate());
        ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionRepository).save(captor.capture());
        assertEquals(TODAY.plusDays(30), captor.getValue().nextExpected());
    }

    @Test
    void checkInMarksIssueReceived() {
        SerialIssue issue = new SerialIssue(9L, 3L, "Vol 1", TODAY, null, IssueStatus.EXPECTED);
        when(serialIssueRepository.findById(9L)).thenReturn(Optional.of(issue));
        when(serialIssueRepository.save(any(SerialIssue.class))).thenAnswer(i -> i.getArgument(0));

        SerialIssue received = serialsService.checkIn(9L, "admin");

        assertEquals(IssueStatus.RECEIVED, received.status());
        assertEquals(TODAY, received.receivedDate());
    }

    @Test
    void claimLateClaimsOverdueExpectedIssues() {
        SerialIssue late = new SerialIssue(9L, 3L, "Vol 1", TODAY.minusDays(40), null, IssueStatus.EXPECTED);
        when(serialIssueRepository.findExpectedBefore(TODAY)).thenReturn(List.of(late));
        when(serialIssueRepository.save(any(SerialIssue.class))).thenAnswer(i -> i.getArgument(0));

        int claimed = serialsService.claimLate("system");

        assertEquals(1, claimed);
        ArgumentCaptor<SerialIssue> captor = ArgumentCaptor.forClass(SerialIssue.class);
        verify(serialIssueRepository).save(captor.capture());
        assertEquals(IssueStatus.CLAIMED, captor.getValue().status());
    }
}
