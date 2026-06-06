package com.justin.libradesk.domain.service;

import com.justin.libradesk.domain.enumtype.CopyStatus;
import com.justin.libradesk.domain.enumtype.LoanStatus;
import com.justin.libradesk.domain.enumtype.PatronStatus;
import com.justin.libradesk.domain.enumtype.PatronType;
import com.justin.libradesk.domain.model.Book;
import com.justin.libradesk.domain.model.BookCopy;
import com.justin.libradesk.domain.model.Loan;
import com.justin.libradesk.domain.model.Patron;
import com.justin.libradesk.dto.DailyCount;
import com.justin.libradesk.dto.NamedCount;
import com.justin.libradesk.repository.BookCopyRepository;
import com.justin.libradesk.repository.BookRepository;
import com.justin.libradesk.repository.LoanRepository;
import com.justin.libradesk.repository.PatronRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportsServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 1, 15, 9, 0);
    private static final ZoneId ZONE = ZoneId.of("UTC");

    @Mock
    private LoanRepository loanRepository;
    @Mock
    private BookRepository bookRepository;
    @Mock
    private BookCopyRepository bookCopyRepository;
    @Mock
    private PatronRepository patronRepository;

    private ReportsService reportsService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW.atZone(ZONE).toInstant(), ZONE);
        reportsService = new ReportsService(loanRepository, bookRepository, bookCopyRepository,
                patronRepository, clock);
    }

    private Loan loan(long copyId, long patronId, LocalDateTime loanedAt, LoanStatus status) {
        LocalDateTime returned = status == LoanStatus.RETURNED ? loanedAt.plusDays(1) : null;
        return new Loan(null, copyId, patronId, loanedAt, loanedAt.plusDays(14), returned, status);
    }

    @Test
    void mostBorrowedRanksBooksByLoanCount() {
        // copies 100,101 -> book 1 ; copy 200 -> book 2
        when(bookCopyRepository.findAll()).thenReturn(List.of(
                new BookCopy(100L, 1L, "c1", CopyStatus.AVAILABLE, null, NOW),
                new BookCopy(101L, 1L, "c2", CopyStatus.AVAILABLE, null, NOW),
                new BookCopy(200L, 2L, "c3", CopyStatus.AVAILABLE, null, NOW)));
        when(bookRepository.findAll()).thenReturn(List.of(
                new Book(1L, null, "Popular", null, null, null, NOW),
                new Book(2L, null, "Rare", null, null, null, NOW)));
        when(loanRepository.findAll()).thenReturn(List.of(
                loan(100L, 1L, NOW, LoanStatus.RETURNED),
                loan(101L, 1L, NOW, LoanStatus.RETURNED),
                loan(200L, 1L, NOW, LoanStatus.RETURNED)));

        List<NamedCount> ranked = reportsService.mostBorrowed(5);

        assertEquals("Popular", ranked.get(0).name());
        assertEquals(2, ranked.get(0).count());
        assertEquals("Rare", ranked.get(1).name());
        assertEquals(1, ranked.get(1).count());
    }

    @Test
    void activeLoansByPatronTypeCountsOutstandingOnly() {
        when(patronRepository.findAll()).thenReturn(List.of(
                new Patron(1L, "A", "A", null, null, PatronType.STUDENT, PatronStatus.ACTIVE, NOW),
                new Patron(2L, "B", "B", null, null, PatronType.STAFF, PatronStatus.ACTIVE, NOW)));
        when(loanRepository.findAll()).thenReturn(List.of(
                loan(10L, 1L, NOW, LoanStatus.ACTIVE),
                loan(11L, 1L, NOW, LoanStatus.ACTIVE),
                loan(12L, 2L, NOW, LoanStatus.RETURNED))); // returned -> not counted

        List<NamedCount> byType = reportsService.activeLoansByPatronType();

        assertEquals(PatronType.values().length, byType.size());
        assertEquals(2, byType.stream().filter(n -> n.name().equals("STUDENT")).findFirst().orElseThrow().count());
        assertEquals(0, byType.stream().filter(n -> n.name().equals("STAFF")).findFirst().orElseThrow().count());
    }

    @Test
    void loansPerDayBucketsByDayAndCoversTheWindow() {
        lenient().when(loanRepository.findAll()).thenReturn(List.of(
                loan(10L, 1L, NOW, LoanStatus.ACTIVE),
                loan(11L, 1L, NOW, LoanStatus.ACTIVE),
                loan(12L, 1L, NOW.minusDays(2), LoanStatus.ACTIVE)));

        List<DailyCount> series = reportsService.loansPerDay(7);

        assertEquals(7, series.size());
        assertEquals(LocalDate.of(2026, 1, 15), series.get(6).date()); // last day is today
        assertEquals(2, series.get(6).count());
        assertEquals(1, series.get(4).count()); // two days before today
    }
}
