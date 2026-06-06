package com.justin.libradesk.domain.service;

import com.justin.libradesk.domain.enumtype.CopyStatus;
import com.justin.libradesk.domain.enumtype.LoanStatus;
import com.justin.libradesk.domain.enumtype.PatronStatus;
import com.justin.libradesk.domain.enumtype.PatronType;
import com.justin.libradesk.domain.model.BookCopy;
import com.justin.libradesk.domain.model.Loan;
import com.justin.libradesk.domain.enumtype.ReservationStatus;
import com.justin.libradesk.domain.model.Patron;
import com.justin.libradesk.domain.model.Reservation;
import com.justin.libradesk.dto.LoanResult;
import com.justin.libradesk.dto.ReturnResult;
import com.justin.libradesk.repository.BookCopyRepository;
import com.justin.libradesk.repository.LoanRepository;
import com.justin.libradesk.repository.PatronRepository;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the checkout workflow's rule enforcement and side effects, mocking the
 * repositories so no database is required. Uses the real {@link BorrowingPolicy}
 * and the bundled {@code application.properties} (student limit = 5).
 */
@ExtendWith(MockitoExtension.class)
class CirculationServiceTest {

    private static final ZoneId ZONE = ZoneId.of("UTC");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 1, 1, 9, 0);

    @Mock
    private PatronRepository patronRepository;
    @Mock
    private BookCopyRepository bookCopyRepository;
    @Mock
    private LoanRepository loanRepository;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private ReservationService reservationService;
    @Mock
    private FineService fineService;
    @Mock
    private SettingsService settingsService;

    private CirculationService circulationService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW.atZone(ZONE).toInstant(), ZONE);
        // Return the supplied fallback for any setting, matching the bundled defaults
        // (loan period 14, student limit 5, fine threshold 0 = no block).
        lenient().when(settingsService.getInt(anyString(), anyInt()))
                .thenAnswer(invocation -> invocation.getArgument(1));
        lenient().when(settingsService.getBigDecimal(anyString(), any()))
                .thenAnswer(invocation -> invocation.getArgument(1));
        lenient().when(fineService.unpaidTotal(anyLong())).thenReturn(BigDecimal.ZERO);
        circulationService = new CirculationService(patronRepository, bookCopyRepository,
                loanRepository, auditLogService, reservationService, fineService, settingsService,
                new BorrowingPolicy(), clock);
    }

    private Patron patron(PatronStatus status) {
        return new Patron(10L, "M010", "Bob", null, null, PatronType.STUDENT, status, NOW);
    }

    private BookCopy copy(CopyStatus status) {
        return new BookCopy(20L, 5L, "BARCODE-20", status, "A1", NOW);
    }

    private void stubLoanSaveReturningId(long id) {
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> {
            Loan loan = invocation.getArgument(0);
            loan.setId(id);
            return loan;
        });
    }

    @Test
    void checkoutSucceedsForActivePatronAndAvailableCopy() {
        when(patronRepository.findById(10L)).thenReturn(Optional.of(patron(PatronStatus.ACTIVE)));
        when(bookCopyRepository.findById(20L)).thenReturn(Optional.of(copy(CopyStatus.AVAILABLE)));
        when(loanRepository.countActiveByPatron(10L)).thenReturn(1);
        stubLoanSaveReturningId(99L);

        LoanResult result = circulationService.checkout(10L, 20L, "admin");

        assertEquals(99L, result.loanId());
        assertEquals(NOW.plusDays(14), result.dueAt());

        // The copy must be flipped to ON_LOAN and persisted.
        ArgumentCaptor<BookCopy> copyCaptor = ArgumentCaptor.forClass(BookCopy.class);
        verify(bookCopyRepository).save(copyCaptor.capture());
        assertEquals(CopyStatus.ON_LOAN, copyCaptor.getValue().getStatus());

        verify(auditLogService).record(eq("admin"), eq("LOAN_CREATED"), eq("Loan"), eq(99L), any());
    }

    @Test
    void createdLoanIsActiveWithCorrectDueDate() {
        when(patronRepository.findById(10L)).thenReturn(Optional.of(patron(PatronStatus.ACTIVE)));
        when(bookCopyRepository.findById(20L)).thenReturn(Optional.of(copy(CopyStatus.AVAILABLE)));
        when(loanRepository.countActiveByPatron(10L)).thenReturn(0);

        ArgumentCaptor<Loan> loanCaptor = ArgumentCaptor.forClass(Loan.class);
        when(loanRepository.save(loanCaptor.capture())).thenAnswer(invocation -> {
            Loan loan = invocation.getArgument(0);
            loan.setId(1L);
            return loan;
        });

        circulationService.checkout(10L, 20L, "admin");

        Loan saved = loanCaptor.getValue();
        assertEquals(LoanStatus.ACTIVE, saved.getStatus());
        assertEquals(NOW, saved.getLoanedAt());
        assertEquals(NOW.plusDays(14), saved.getDueAt());
    }

    @Test
    void checkoutRejectsSuspendedPatron() {
        when(patronRepository.findById(10L)).thenReturn(Optional.of(patron(PatronStatus.SUSPENDED)));
        when(bookCopyRepository.findById(20L)).thenReturn(Optional.of(copy(CopyStatus.AVAILABLE)));

        assertThrows(ValidationException.class, () -> circulationService.checkout(10L, 20L, "admin"));
        verify(loanRepository, never()).save(any());
    }

    @Test
    void checkoutRejectsUnavailableCopy() {
        when(patronRepository.findById(10L)).thenReturn(Optional.of(patron(PatronStatus.ACTIVE)));
        when(bookCopyRepository.findById(20L)).thenReturn(Optional.of(copy(CopyStatus.ON_LOAN)));

        assertThrows(ValidationException.class, () -> circulationService.checkout(10L, 20L, "admin"));
        verify(loanRepository, never()).save(any());
    }

    @Test
    void returnByCopyChargesFineWhenOverdue() {
        when(bookCopyRepository.findById(20L)).thenReturn(Optional.of(copy(CopyStatus.ON_LOAN)));
        when(loanRepository.findActiveByCopy(20L)).thenReturn(Optional.of(activeLoan(NOW.minusDays(1))));
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        circulationService.returnByCopy(20L, "admin");

        verify(fineService).chargeOverdue(eq(10L), eq(77L), eq(1L), eq("admin"));
    }

    @Test
    void checkoutBlockedWhenUnpaidFinesExceedThreshold() {
        when(patronRepository.findById(10L)).thenReturn(Optional.of(patron(PatronStatus.ACTIVE)));
        when(bookCopyRepository.findById(20L)).thenReturn(Optional.of(copy(CopyStatus.AVAILABLE)));
        when(settingsService.getBigDecimal(eq("fine.block.threshold"), any())).thenReturn(new BigDecimal("5"));
        when(fineService.unpaidTotal(10L)).thenReturn(new BigDecimal("10"));

        assertThrows(ValidationException.class, () -> circulationService.checkout(10L, 20L, "admin"));
        verify(loanRepository, never()).save(any());
    }

    @Test
    void renewExtendsDueDateWhenNoReservation() {
        when(bookCopyRepository.findById(20L)).thenReturn(Optional.of(copy(CopyStatus.ON_LOAN)));
        when(loanRepository.findActiveByCopy(20L)).thenReturn(Optional.of(activeLoan(NOW.plusDays(1))));
        when(reservationService.hasPending(5L)).thenReturn(false);
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LocalDateTime newDue = circulationService.renew(20L, "admin");

        assertEquals(NOW.plusDays(14), newDue);
        ArgumentCaptor<Loan> captor = ArgumentCaptor.forClass(Loan.class);
        verify(loanRepository).save(captor.capture());
        assertEquals(LoanStatus.ACTIVE, captor.getValue().getStatus());
    }

    @Test
    void renewRejectedWhenBookIsReserved() {
        when(bookCopyRepository.findById(20L)).thenReturn(Optional.of(copy(CopyStatus.ON_LOAN)));
        when(loanRepository.findActiveByCopy(20L)).thenReturn(Optional.of(activeLoan(NOW.plusDays(1))));
        when(reservationService.hasPending(5L)).thenReturn(true);

        assertThrows(ValidationException.class, () -> circulationService.renew(20L, "admin"));
        verify(loanRepository, never()).save(any());
    }

    @Test
    void markOverdueLoansFlipsActiveLoansToOverdue() {
        Loan overdue = new Loan(1L, 20L, 10L, NOW.minusDays(20), NOW.minusDays(1), null, LoanStatus.ACTIVE);
        when(loanRepository.findOverdue()).thenReturn(List.of(overdue));
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        int count = circulationService.markOverdueLoans();

        assertEquals(1, count);
        ArgumentCaptor<Loan> captor = ArgumentCaptor.forClass(Loan.class);
        verify(loanRepository).save(captor.capture());
        assertEquals(LoanStatus.OVERDUE, captor.getValue().getStatus());
        verify(auditLogService).record(eq("system"), eq("LOAN_OVERDUE"), eq("Loan"), eq(1L), any());
    }

    @Test
    void checkoutRejectsWhenBorrowingLimitReached() {
        when(patronRepository.findById(10L)).thenReturn(Optional.of(patron(PatronStatus.ACTIVE)));
        when(bookCopyRepository.findById(20L)).thenReturn(Optional.of(copy(CopyStatus.AVAILABLE)));
        when(loanRepository.countActiveByPatron(10L)).thenReturn(5); // student limit is 5

        assertThrows(ValidationException.class, () -> circulationService.checkout(10L, 20L, "admin"));
        verify(loanRepository, never()).save(any());
    }

    private Loan activeLoan(LocalDateTime dueAt) {
        return new Loan(77L, 20L, 10L, NOW.minusDays(7), dueAt, null, LoanStatus.ACTIVE);
    }

    @Test
    void returnByCopyClosesLoanAndFreesCopy() {
        when(bookCopyRepository.findById(20L)).thenReturn(Optional.of(copy(CopyStatus.ON_LOAN)));
        when(loanRepository.findActiveByCopy(20L)).thenReturn(Optional.of(activeLoan(NOW.plusDays(5))));
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReturnResult result = circulationService.returnByCopy(20L, "admin");

        assertEquals(77L, result.loanId());
        assertEquals(NOW, result.returnedAt());
        assertFalse(result.wasOverdue());
        assertFalse(result.heldForReservation()); // no reservation waiting (promoteNext returns empty)

        ArgumentCaptor<Loan> loanCaptor = ArgumentCaptor.forClass(Loan.class);
        verify(loanRepository).save(loanCaptor.capture());
        assertEquals(LoanStatus.RETURNED, loanCaptor.getValue().getStatus());
        assertEquals(NOW, loanCaptor.getValue().getReturnedAt());

        ArgumentCaptor<BookCopy> copyCaptor = ArgumentCaptor.forClass(BookCopy.class);
        verify(bookCopyRepository).save(copyCaptor.capture());
        assertEquals(CopyStatus.AVAILABLE, copyCaptor.getValue().getStatus());

        verify(auditLogService).record(eq("admin"), eq("LOAN_RETURNED"), eq("Loan"), eq(77L), any());
    }

    @Test
    void returnByCopyHoldsCopyForWaitingReservation() {
        when(bookCopyRepository.findById(20L)).thenReturn(Optional.of(copy(CopyStatus.ON_LOAN)));
        when(loanRepository.findActiveByCopy(20L)).thenReturn(Optional.of(activeLoan(NOW.plusDays(5))));
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> invocation.getArgument(0));
        // copy(...) belongs to book 5; a patron is waiting in that book's queue.
        when(reservationService.promoteNext(5L, "admin")).thenReturn(
                Optional.of(new Reservation(1L, 5L, 99L, NOW, 1, ReservationStatus.READY)));

        ReturnResult result = circulationService.returnByCopy(20L, "admin");

        assertTrue(result.heldForReservation());
        ArgumentCaptor<BookCopy> copyCaptor = ArgumentCaptor.forClass(BookCopy.class);
        verify(bookCopyRepository).save(copyCaptor.capture());
        assertEquals(CopyStatus.RESERVED, copyCaptor.getValue().getStatus());
    }

    @Test
    void returnByCopyFlagsOverdueLoan() {
        when(bookCopyRepository.findById(20L)).thenReturn(Optional.of(copy(CopyStatus.ON_LOAN)));
        when(loanRepository.findActiveByCopy(20L)).thenReturn(Optional.of(activeLoan(NOW.minusDays(1))));
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReturnResult result = circulationService.returnByCopy(20L, "admin");

        assertTrue(result.wasOverdue());
    }

    @Test
    void returnByCopyRejectsWhenNoActiveLoan() {
        when(bookCopyRepository.findById(20L)).thenReturn(Optional.of(copy(CopyStatus.AVAILABLE)));
        when(loanRepository.findActiveByCopy(20L)).thenReturn(Optional.empty());

        assertThrows(ValidationException.class, () -> circulationService.returnByCopy(20L, "admin"));
        verify(loanRepository, never()).save(any());
    }
}
