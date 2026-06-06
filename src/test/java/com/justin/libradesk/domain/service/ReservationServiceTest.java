package com.justin.libradesk.domain.service;

import com.justin.libradesk.domain.enumtype.PatronStatus;
import com.justin.libradesk.domain.enumtype.PatronType;
import com.justin.libradesk.domain.enumtype.ReservationStatus;
import com.justin.libradesk.domain.model.Book;
import com.justin.libradesk.domain.model.Patron;
import com.justin.libradesk.domain.model.Reservation;
import com.justin.libradesk.repository.BookRepository;
import com.justin.libradesk.repository.PatronRepository;
import com.justin.libradesk.repository.ReservationRepository;
import com.justin.libradesk.validation.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 1, 1, 9, 0);

    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private PatronRepository patronRepository;
    @Mock
    private BookRepository bookRepository;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private SettingsService settingsService;

    private ReservationService reservationService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW.atZone(ZoneId.of("UTC")).toInstant(), ZoneId.of("UTC"));
        reservationService = new ReservationService(reservationRepository, patronRepository,
                bookRepository, auditLogService, settingsService, clock);
    }

    private Book book() {
        return new Book(5L, "isbn", "Book", null, null, null, NOW);
    }

    private Patron patron(PatronStatus status) {
        return new Patron(10L, "M1", "Pat", null, null, PatronType.STUDENT, status, NOW);
    }

    @Test
    void reserveAppendsToQueue() {
        when(bookRepository.findById(5L)).thenReturn(Optional.of(book()));
        when(patronRepository.findById(10L)).thenReturn(Optional.of(patron(PatronStatus.ACTIVE)));
        when(reservationRepository.findActiveByBookAndPatron(5L, 10L)).thenReturn(Optional.empty());
        when(reservationRepository.maxQueuePosition(5L)).thenReturn(2);
        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Reservation result = reservationService.reserve(5L, 10L, "admin");

        assertEquals(3, result.getQueuePosition());
        assertEquals(ReservationStatus.PENDING, result.getStatus());
    }

    @Test
    void reserveRejectsSuspendedPatron() {
        when(bookRepository.findById(5L)).thenReturn(Optional.of(book()));
        when(patronRepository.findById(10L)).thenReturn(Optional.of(patron(PatronStatus.SUSPENDED)));

        assertThrows(ValidationException.class, () -> reservationService.reserve(5L, 10L, "admin"));
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void reserveRejectsDuplicateActiveReservation() {
        when(bookRepository.findById(5L)).thenReturn(Optional.of(book()));
        when(patronRepository.findById(10L)).thenReturn(Optional.of(patron(PatronStatus.ACTIVE)));
        when(reservationRepository.findActiveByBookAndPatron(5L, 10L)).thenReturn(
                Optional.of(new Reservation(1L, 5L, 10L, NOW, 1, ReservationStatus.PENDING)));

        assertThrows(ValidationException.class, () -> reservationService.reserve(5L, 10L, "admin"));
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void promoteNextMarksHeadReady() {
        Reservation head = new Reservation(1L, 5L, 10L, NOW, 1, ReservationStatus.PENDING);
        when(reservationRepository.findNextPending(5L)).thenReturn(Optional.of(head));
        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Optional<Reservation> promoted = reservationService.promoteNext(5L, "admin");

        assertTrue(promoted.isPresent());
        ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository).save(captor.capture());
        assertEquals(ReservationStatus.READY, captor.getValue().getStatus());
    }

    @Test
    void promoteNextOnEmptyQueueDoesNothing() {
        when(reservationRepository.findNextPending(5L)).thenReturn(Optional.empty());

        assertTrue(reservationService.promoteNext(5L, "admin").isEmpty());
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void expireStaleReadyMarksExpiredAndPromotesNext() {
        Reservation stale = new Reservation(7L, 5L, 10L, NOW.minusDays(10), 1, ReservationStatus.READY);
        when(settingsService.getInt("reservation.ready.expiry.days", 3)).thenReturn(3);
        when(reservationRepository.findReadyExpired(any())).thenReturn(List.of(stale));
        when(reservationRepository.findNextPending(5L)).thenReturn(Optional.empty());
        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        int expired = reservationService.expireStaleReady("system");

        assertEquals(1, expired);
        ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository).save(captor.capture());
        assertEquals(ReservationStatus.EXPIRED, captor.getValue().getStatus());
    }
}
