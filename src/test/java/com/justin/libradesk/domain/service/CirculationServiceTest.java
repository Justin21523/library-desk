package com.justin.libradesk.domain.service;

import com.justin.libradesk.domain.enumtype.CopyStatus;
import com.justin.libradesk.domain.enumtype.FeeType;
import com.justin.libradesk.domain.enumtype.LoanStatus;
import com.justin.libradesk.domain.enumtype.MaterialType;
import com.justin.libradesk.domain.enumtype.PatronStatus;
import com.justin.libradesk.domain.enumtype.PatronType;
import com.justin.libradesk.domain.enumtype.ReservationStatus;
import com.justin.libradesk.domain.model.Book;
import com.justin.libradesk.domain.model.BookCopy;
import com.justin.libradesk.domain.model.CircPolicy;
import com.justin.libradesk.domain.model.Loan;
import com.justin.libradesk.domain.model.Patron;
import com.justin.libradesk.domain.model.Reservation;
import com.justin.libradesk.dto.LoanResult;
import com.justin.libradesk.dto.ReturnResult;
import com.justin.libradesk.repository.BookCopyRepository;
import com.justin.libradesk.repository.BookRepository;
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
import java.time.LocalDate;
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
 * Tests the checkout/return/renew workflow's rule enforcement and side effects,
 * mocking the repositories and policy/calendar/account collaborators so no
 * database is required. Uses the real {@link BorrowingPolicy}.
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
    private BookRepository bookRepository;
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
    @Mock
    private CircPolicyService circPolicyService;
    @Mock
    private CalendarService calendarService;
    @Mock
    private PatronAccountService patronAccountService;

    private CirculationService circulationService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW.atZone(ZONE).toInstant(), ZONE);
        // Default collaborators: no blocks, a standard policy, an open calendar.
        lenient().when(patronAccountService.blocks(anyLong())).thenReturn(List.of());
        lenient().when(circPolicyService.policyFor(any(), any())).thenReturn(policy(14, 5, 2));
        lenient().when(calendarService.nextOpenDay(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(bookRepository.findById(anyLong())).thenReturn(Optional.of(book()));
        lenient().when(settingsService.getBigDecimal(anyString(), any()))
                .thenAnswer(i -> i.getArgument(1));
        lenient().when(settingsService.getInt(anyString(), anyInt())).thenAnswer(i -> i.getArgument(1));
        circulationService = new CirculationService(patronRepository, bookCopyRepository,
                bookRepository, loanRepository, auditLogService, reservationService, fineService,
                settingsService, circPolicyService, calendarService, patronAccountService,
                new BorrowingPolicy(), clock);
    }

    private static CircPolicy policy(int loanDays, int maxLoans, int renewalLimit) {
        return new CircPolicy(1L, PatronType.STUDENT, null, loanDays, maxLoans, renewalLimit, 5,
                new BigDecimal("0.50"), BigDecimal.ZERO, 0);
    }

    private static Book book() {
        Book book = new Book(5L, "isbn", "Book", null, null, null, NOW);
        book.setMaterialType(MaterialType.BOOK);
        return book;
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
    void checkoutRollsDueDateOffClosedDays() {
        when(patronRepository.findById(10L)).thenReturn(Optional.of(patron(PatronStatus.ACTIVE)));
        when(bookCopyRepository.findById(20L)).thenReturn(Optional.of(copy(CopyStatus.AVAILABLE)));
        when(loanRepository.countActiveByPatron(10L)).thenReturn(0);
        // The natural due date (NOW + 14 = Jan 15) is closed; roll to Jan 16.
        when(calendarService.nextOpenDay(LocalDate.of(2026, 1, 15)))
                .thenReturn(LocalDate.of(2026, 1, 16));
        stubLoanSaveReturningId(1L);

        LoanResult result = circulationService.checkout(10L, 20L, "admin");

        assertEquals(LocalDateTime.of(2026, 1, 16, 9, 0), result.dueAt());
    }

    @Test
    void checkoutRejectsSuspendedPatronViaBlock() {
        when(patronRepository.findById(10L)).thenReturn(Optional.of(patron(PatronStatus.SUSPENDED)));
        when(bookCopyRepository.findById(20L)).thenReturn(Optional.of(copy(CopyStatus.AVAILABLE)));
        when(patronAccountService.blocks(10L)).thenReturn(List.of("Account suspended"));

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
        when(patronRepository.findById(10L)).thenReturn(Optional.of(patron(PatronStatus.ACTIVE)));
        when(calendarService.openDaysBetween(any(), any())).thenReturn(1L);

        circulationService.returnByCopy(20L, "admin");

        ArgumentCaptor<BigDecimal> amount = ArgumentCaptor.forClass(BigDecimal.class);
        verify(fineService).charge(eq(10L), eq(77L), amount.capture(), eq(FeeType.OVERDUE), eq("admin"));
        assertEquals(0, amount.getValue().compareTo(new BigDecimal("0.50")));
    }

    @Test
    void returnByCopyCapsOverdueFine() {
        when(bookCopyRepository.findById(20L)).thenReturn(Optional.of(copy(CopyStatus.ON_LOAN)));
        when(loanRepository.findActiveByCopy(20L)).thenReturn(Optional.of(activeLoan(NOW.minusDays(100))));
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(patronRepository.findById(10L)).thenReturn(Optional.of(patron(PatronStatus.ACTIVE)));
        // Policy with a fine cap of 2.00; 100 open days × 0.50 = 50.00 should be capped.
        when(circPolicyService.policyFor(any(), any())).thenReturn(new CircPolicy(1L, PatronType.STUDENT,
                null, 14, 5, 2, 5, new BigDecimal("0.50"), new BigDecimal("2.00"), 0));
        when(calendarService.openDaysBetween(any(), any())).thenReturn(100L);

        circulationService.returnByCopy(20L, "admin");

        ArgumentCaptor<BigDecimal> amount = ArgumentCaptor.forClass(BigDecimal.class);
        verify(fineService).charge(eq(10L), eq(77L), amount.capture(), eq(FeeType.OVERDUE), eq("admin"));
        assertEquals(0, amount.getValue().compareTo(new BigDecimal("2.00")));
    }

    @Test
    void checkoutRejectedWhenBlocked() {
        when(patronRepository.findById(10L)).thenReturn(Optional.of(patron(PatronStatus.ACTIVE)));
        when(bookCopyRepository.findById(20L)).thenReturn(Optional.of(copy(CopyStatus.AVAILABLE)));
        when(patronAccountService.blocks(10L)).thenReturn(List.of("Outstanding fines exceed the allowed limit"));

        assertThrows(ValidationException.class, () -> circulationService.checkout(10L, 20L, "admin"));
        verify(loanRepository, never()).save(any());
    }

    @Test
    void renewExtendsDueDateWhenNoReservation() {
        when(bookCopyRepository.findById(20L)).thenReturn(Optional.of(copy(CopyStatus.ON_LOAN)));
        when(loanRepository.findActiveByCopy(20L)).thenReturn(Optional.of(activeLoan(NOW.plusDays(1))));
        when(reservationService.hasPending(5L)).thenReturn(false);
        when(patronRepository.findById(10L)).thenReturn(Optional.of(patron(PatronStatus.ACTIVE)));
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LocalDateTime newDue = circulationService.renew(20L, "admin");

        assertEquals(NOW.plusDays(14), newDue);
        ArgumentCaptor<Loan> captor = ArgumentCaptor.forClass(Loan.class);
        verify(loanRepository).save(captor.capture());
        assertEquals(LoanStatus.ACTIVE, captor.getValue().getStatus());
        assertEquals(1, captor.getValue().getRenewalCount());
    }

    @Test
    void renewRejectedWhenRenewalLimitReached() {
        Loan loan = activeLoan(NOW.plusDays(1));
        loan.setRenewalCount(2); // policy renewal limit is 2
        when(bookCopyRepository.findById(20L)).thenReturn(Optional.of(copy(CopyStatus.ON_LOAN)));
        when(loanRepository.findActiveByCopy(20L)).thenReturn(Optional.of(loan));
        when(reservationService.hasPending(5L)).thenReturn(false);
        when(patronRepository.findById(10L)).thenReturn(Optional.of(patron(PatronStatus.ACTIVE)));

        assertThrows(ValidationException.class, () -> circulationService.renew(20L, "admin"));
        verify(loanRepository, never()).save(any());
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
    void markLostClosesLoanRaisesFeesAndMarksCopyLost() {
        when(bookCopyRepository.findById(20L)).thenReturn(Optional.of(copy(CopyStatus.ON_LOAN)));
        when(loanRepository.findActiveByCopy(20L)).thenReturn(Optional.of(activeLoan(NOW.plusDays(1))));
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(settingsService.getBigDecimal(eq("fine.processing.fee"), any())).thenReturn(new BigDecimal("5.00"));

        circulationService.markLost(20L, new BigDecimal("25.00"), "admin");

        ArgumentCaptor<Loan> loanCaptor = ArgumentCaptor.forClass(Loan.class);
        verify(loanRepository).save(loanCaptor.capture());
        assertEquals(LoanStatus.LOST, loanCaptor.getValue().getStatus());

        ArgumentCaptor<BookCopy> copyCaptor = ArgumentCaptor.forClass(BookCopy.class);
        verify(bookCopyRepository).save(copyCaptor.capture());
        assertEquals(CopyStatus.LOST, copyCaptor.getValue().getStatus());

        verify(fineService).charge(eq(10L), eq(77L), eq(new BigDecimal("25.00")), eq(FeeType.LOST_ITEM), eq("admin"));
        verify(fineService).charge(eq(10L), eq(77L), eq(new BigDecimal("5.00")), eq(FeeType.PROCESSING), eq("admin"));
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
        when(loanRepository.countActiveByPatron(10L)).thenReturn(5); // policy max loans is 5

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
        assertFalse(result.heldForReservation());

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
        when(patronRepository.findById(10L)).thenReturn(Optional.of(patron(PatronStatus.ACTIVE)));
        when(calendarService.openDaysBetween(any(), any())).thenReturn(1L);

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
