package com.justin.libradesk.domain.service;

import com.justin.libradesk.domain.enumtype.FineStatus;
import com.justin.libradesk.domain.model.Fine;
import com.justin.libradesk.repository.FineRepository;
import com.justin.libradesk.validation.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FineServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 1, 1, 9, 0);

    @Mock
    private FineRepository fineRepository;
    @Mock
    private SettingsService settingsService;
    @Mock
    private AuditLogService auditLogService;

    private FineService fineService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW.atZone(ZoneId.of("UTC")).toInstant(), ZoneId.of("UTC"));
        fineService = new FineService(fineRepository, settingsService, auditLogService, clock);
    }

    @Test
    void chargeOverdueComputesAmountFromRateAndDays() {
        when(settingsService.getBigDecimal(eq("fine.per.day"), any())).thenReturn(new BigDecimal("0.50"));
        when(fineRepository.save(any(Fine.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Fine fine = fineService.chargeOverdue(1L, 2L, 4, "admin");

        assertEquals(0, fine.getAmount().compareTo(new BigDecimal("2.00")));
        assertEquals(FineStatus.UNPAID, fine.getStatus());
    }

    @Test
    void chargeOverdueDoesNothingForZeroDays() {
        assertNull(fineService.chargeOverdue(1L, 2L, 0, "admin"));
        verify(fineRepository, never()).save(any());
    }

    @Test
    void payMarksFinePaidWithSettledTimestamp() {
        Fine unpaid = new Fine(9L, 1L, 2L, new BigDecimal("3.00"), FineStatus.UNPAID, NOW, null);
        when(fineRepository.findById(9L)).thenReturn(Optional.of(unpaid));
        when(fineRepository.save(any(Fine.class))).thenAnswer(invocation -> invocation.getArgument(0));

        fineService.pay(9L, "admin");

        ArgumentCaptor<Fine> captor = ArgumentCaptor.forClass(Fine.class);
        verify(fineRepository).save(captor.capture());
        assertEquals(FineStatus.PAID, captor.getValue().getStatus());
        assertEquals(NOW, captor.getValue().getSettledAt());
    }

    @Test
    void settlingAnAlreadySettledFineIsRejected() {
        Fine paid = new Fine(9L, 1L, 2L, new BigDecimal("3.00"), FineStatus.PAID, NOW, NOW);
        when(fineRepository.findById(9L)).thenReturn(Optional.of(paid));

        assertThrows(ValidationException.class, () -> fineService.waive(9L, "admin"));
        verify(fineRepository, never()).save(any());
    }
}
