package com.justin.libradesk.domain.service;

import com.justin.libradesk.domain.enumtype.CopyStatus;
import com.justin.libradesk.domain.enumtype.LoanStatus;
import com.justin.libradesk.domain.enumtype.PatronStatus;
import com.justin.libradesk.domain.enumtype.PatronType;
import com.justin.libradesk.domain.enumtype.ReservationStatus;
import com.justin.libradesk.domain.model.Book;
import com.justin.libradesk.domain.model.BookCopy;
import com.justin.libradesk.domain.model.Loan;
import com.justin.libradesk.domain.model.Patron;
import com.justin.libradesk.domain.model.Reservation;
import com.justin.libradesk.infrastructure.notify.Mailer;
import com.justin.libradesk.repository.BookCopyRepository;
import com.justin.libradesk.repository.BookRepository;
import com.justin.libradesk.repository.LoanRepository;
import com.justin.libradesk.repository.PatronRepository;
import com.justin.libradesk.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NoticeServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 1, 1, 9, 0);

    @Mock
    private LoanRepository loanRepository;
    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private PatronRepository patronRepository;
    @Mock
    private BookCopyRepository bookCopyRepository;
    @Mock
    private BookRepository bookRepository;
    @Mock
    private SettingsService settingsService;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private Mailer mailer;

    private NoticeService noticeService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW.atZone(ZoneId.of("UTC")).toInstant(), ZoneId.of("UTC"));
        noticeService = new NoticeService(loanRepository, reservationRepository, patronRepository,
                bookCopyRepository, bookRepository, settingsService, auditLogService, mailer, clock);
        lenient().when(settingsService.getInt(anyString(), anyInt())).thenAnswer(i -> i.getArgument(1));
        lenient().when(bookCopyRepository.findById(any())).thenReturn(Optional.of(
                new BookCopy(20L, 5L, "BC", CopyStatus.ON_LOAN, "A1", NOW)));
        lenient().when(bookRepository.findById(any())).thenReturn(Optional.of(
                new Book(5L, "isbn", "A Title", null, null, null, NOW)));
    }

    private Patron patron(String email) {
        return new Patron(10L, "M1", "Pat", email, null, PatronType.STUDENT, PatronStatus.ACTIVE, NOW);
    }

    private Loan loan(LoanStatus status) {
        return new Loan(1L, 20L, 10L, NOW.minusDays(7), NOW.plusDays(1), null, status);
    }

    @Test
    void sendDueSoonMailsPatronWithEmail() {
        when(loanRepository.findActiveDueBetween(any(), any())).thenReturn(List.of(loan(LoanStatus.ACTIVE)));
        when(patronRepository.findById(10L)).thenReturn(Optional.of(patron("pat@example.com")));

        assertEquals(1, noticeService.sendDueSoon());
        verify(mailer).send(eq("pat@example.com"), anyString(), anyString());
    }

    @Test
    void sendDueSoonSkipsPatronWithoutEmail() {
        when(loanRepository.findActiveDueBetween(any(), any())).thenReturn(List.of(loan(LoanStatus.ACTIVE)));
        when(patronRepository.findById(10L)).thenReturn(Optional.of(patron(null)));

        assertEquals(0, noticeService.sendDueSoon());
        verify(mailer, never()).send(any(), any(), any());
    }

    @Test
    void sendOverdueMailsForOverdueLoans() {
        when(loanRepository.findByStatus(LoanStatus.OVERDUE)).thenReturn(List.of(loan(LoanStatus.OVERDUE)));
        when(patronRepository.findById(10L)).thenReturn(Optional.of(patron("pat@example.com")));

        assertEquals(1, noticeService.sendOverdue());
        verify(mailer, times(1)).send(eq("pat@example.com"), anyString(), anyString());
    }

    @Test
    void sendHoldReadyMailsForReadyHolds() {
        when(reservationRepository.findAllActive()).thenReturn(List.of(
                new Reservation(7L, 5L, 10L, NOW, 1, ReservationStatus.READY)));
        when(patronRepository.findById(10L)).thenReturn(Optional.of(patron("pat@example.com")));

        assertEquals(1, noticeService.sendHoldReady());
        verify(mailer).send(eq("pat@example.com"), anyString(), anyString());
    }
}
