package com.justin.libradesk.domain.service;

import com.justin.libradesk.domain.enumtype.LoanStatus;
import com.justin.libradesk.domain.enumtype.PatronStatus;
import com.justin.libradesk.domain.enumtype.PatronType;
import com.justin.libradesk.domain.model.Patron;
import com.justin.libradesk.repository.FineRepository;
import com.justin.libradesk.repository.LoanRepository;
import com.justin.libradesk.repository.PatronRepository;
import com.justin.libradesk.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatronAccountServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 1, 1, 9, 0);

    @Mock
    private PatronRepository patronRepository;
    @Mock
    private LoanRepository loanRepository;
    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private FineRepository fineRepository;
    @Mock
    private SettingsService settingsService;

    private PatronAccountService service;

    @BeforeEach
    void setUp() {
        service = new PatronAccountService(patronRepository, loanRepository, reservationRepository,
                fineRepository, settingsService);
        lenient().when(settingsService.getBigDecimal(anyString(), any())).thenAnswer(i -> i.getArgument(1));
        lenient().when(settingsService.getInt(anyString(), anyInt())).thenAnswer(i -> i.getArgument(1));
        lenient().when(fineRepository.unpaidTotal(1L)).thenReturn(BigDecimal.ZERO);
        lenient().when(loanRepository.countByPatronAndStatus(eq(1L), any())).thenReturn(0);
    }

    private Patron patron(PatronStatus status) {
        return new Patron(1L, "M1", "Pat", null, null, PatronType.STUDENT, status, NOW);
    }

    @Test
    void expiredMembershipIsBlocked() {
        when(patronRepository.findById(1L)).thenReturn(Optional.of(patron(PatronStatus.EXPIRED)));

        List<String> blocks = service.blocks(1L);

        assertTrue(blocks.stream().anyMatch(b -> b.contains("expired")));
    }

    @Test
    void balanceOverThresholdIsBlocked() {
        when(patronRepository.findById(1L)).thenReturn(Optional.of(patron(PatronStatus.ACTIVE)));
        when(fineRepository.unpaidTotal(1L)).thenReturn(new BigDecimal("10.00"));
        when(settingsService.getBigDecimal(eq("fine.block.threshold"), any())).thenReturn(new BigDecimal("5"));

        List<String> blocks = service.blocks(1L);

        assertTrue(blocks.stream().anyMatch(b -> b.contains("fines")));
    }

    @Test
    void tooManyOverdueIsBlocked() {
        when(patronRepository.findById(1L)).thenReturn(Optional.of(patron(PatronStatus.ACTIVE)));
        when(settingsService.getInt(eq("overdue.block.count"), anyInt())).thenReturn(3);
        when(loanRepository.countByPatronAndStatus(1L, LoanStatus.OVERDUE)).thenReturn(3);

        List<String> blocks = service.blocks(1L);

        assertTrue(blocks.stream().anyMatch(b -> b.contains("overdue")));
    }

    @Test
    void activePatronInGoodStandingHasNoBlocks() {
        when(patronRepository.findById(1L)).thenReturn(Optional.of(patron(PatronStatus.ACTIVE)));

        assertTrue(service.blocks(1L).isEmpty());
    }
}
