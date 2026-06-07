package com.justin.libradesk.domain.service;

import com.justin.libradesk.domain.enumtype.MaterialType;
import com.justin.libradesk.domain.enumtype.PatronType;
import com.justin.libradesk.domain.model.CircPolicy;
import com.justin.libradesk.repository.CircPolicyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CircPolicyServiceTest {

    @Mock
    private CircPolicyRepository circPolicyRepository;
    @Mock
    private SettingsService settingsService;
    @Mock
    private AuditLogService auditLogService;

    private CircPolicyService circPolicyService;

    @BeforeEach
    void setUp() {
        circPolicyService = new CircPolicyService(circPolicyRepository, settingsService, auditLogService);
    }

    @Test
    void policyForReturnsMatrixRowWhenPresent() {
        CircPolicy row = new CircPolicy(1L, PatronType.STAFF, MaterialType.BOOK, 28, 10, 3, 10,
                new BigDecimal("0.25"), new BigDecimal("30.00"), 2);
        when(circPolicyRepository.findFor(PatronType.STAFF, MaterialType.BOOK)).thenReturn(Optional.of(row));

        assertEquals(row, circPolicyService.policyFor(PatronType.STAFF, MaterialType.BOOK));
    }

    @Test
    void policyForFallsBackToSettingsWhenNoRow() {
        when(circPolicyRepository.findFor(any(), any())).thenReturn(Optional.empty());
        when(settingsService.getInt(eq("loan.period.days"), eq(14))).thenReturn(21);
        when(settingsService.getInt(eq("borrow.limit.student"), eq(5))).thenReturn(7);
        when(settingsService.getBigDecimal(eq("fine.per.day"), any())).thenReturn(new BigDecimal("0.50"));

        CircPolicy policy = circPolicyService.policyFor(PatronType.STUDENT, MaterialType.BOOK);

        assertEquals(21, policy.loanDays());
        assertEquals(7, policy.maxLoans());
        assertEquals(PatronType.STUDENT, policy.patronType());
        assertEquals(0, policy.finePerDay().compareTo(new BigDecimal("0.50")));
    }
}
