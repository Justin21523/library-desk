package com.justin.libradesk.domain.service;

import com.justin.libradesk.domain.enumtype.PatronType;
import com.justin.libradesk.domain.model.Loan;
import com.justin.libradesk.dto.DailyCount;
import com.justin.libradesk.dto.NamedCount;
import com.justin.libradesk.repository.BookCopyRepository;
import com.justin.libradesk.repository.BookRepository;
import com.justin.libradesk.repository.LoanRepository;
import com.justin.libradesk.repository.PatronRepository;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Read-only queries and aggregates for the Reports screen and exports.
 *
 * <p>Aggregates are computed in memory from the repositories (the same approach as
 * {@link DashboardService}); for this project's scale that keeps the code simple
 * and avoids bespoke SQL.
 */
public class ReportsService {

    private final LoanRepository loanRepository;
    private final BookRepository bookRepository;
    private final BookCopyRepository bookCopyRepository;
    private final PatronRepository patronRepository;
    private final Clock clock;

    public ReportsService(LoanRepository loanRepository,
                          BookRepository bookRepository,
                          BookCopyRepository bookCopyRepository,
                          PatronRepository patronRepository,
                          Clock clock) {
        this.loanRepository = loanRepository;
        this.bookRepository = bookRepository;
        this.bookCopyRepository = bookCopyRepository;
        this.patronRepository = patronRepository;
        this.clock = clock;
    }

    /**
     * @return every unreturned loan whose due date has passed, regardless of
     *         whether the overdue sweep has already flipped its status.
     */
    public List<Loan> overdueLoans() {
        LocalDateTime now = LocalDateTime.now(clock);
        return loanRepository.findAll().stream()
                .filter(loan -> loan.isOverdue(now))
                .toList();
    }

    /** @return every unreturned loan (active or overdue). */
    public List<Loan> outstandingLoans() {
        return loanRepository.findAll().stream()
                .filter(loan -> loan.getReturnedAt() == null)
                .toList();
    }

    /** @return the most-borrowed books (by total loans ever), highest first. */
    public List<NamedCount> mostBorrowed(int limit) {
        Map<Long, Long> copyToBook = new HashMap<>();
        bookCopyRepository.findAll().forEach(copy -> copyToBook.put(copy.getId(), copy.getBookId()));
        Map<Long, String> bookTitles = new HashMap<>();
        bookRepository.findAll().forEach(book -> bookTitles.put(book.getId(), book.getTitle()));

        Map<Long, Long> countByBook = new HashMap<>();
        for (Loan loan : loanRepository.findAll()) {
            Long bookId = copyToBook.get(loan.getCopyId());
            if (bookId != null) {
                countByBook.merge(bookId, 1L, Long::sum);
            }
        }
        return countByBook.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(limit)
                .map(e -> new NamedCount(bookTitles.getOrDefault(e.getKey(), "#" + e.getKey()), e.getValue()))
                .toList();
    }

    /** @return the number of outstanding loans per patron type (every type included). */
    public List<NamedCount> activeLoansByPatronType() {
        Map<Long, PatronType> patronTypes = new HashMap<>();
        patronRepository.findAll().forEach(patron -> patronTypes.put(patron.getId(), patron.getPatronType()));

        Map<PatronType, Long> counts = new HashMap<>();
        for (Loan loan : outstandingLoans()) {
            PatronType type = patronTypes.get(loan.getPatronId());
            if (type != null) {
                counts.merge(type, 1L, Long::sum);
            }
        }
        List<NamedCount> result = new ArrayList<>();
        for (PatronType type : PatronType.values()) {
            result.add(new NamedCount(type.name(), counts.getOrDefault(type, 0L)));
        }
        return result;
    }

    /** @return loans issued on each of the last {@code days} days (oldest first). */
    public List<DailyCount> loansPerDay(int days) {
        LocalDate today = LocalDate.now(clock);
        Map<LocalDate, Long> counts = new HashMap<>();
        for (Loan loan : loanRepository.findAll()) {
            counts.merge(loan.getLoanedAt().toLocalDate(), 1L, Long::sum);
        }
        List<DailyCount> series = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            series.add(new DailyCount(date, counts.getOrDefault(date, 0L)));
        }
        return series.stream().sorted(Comparator.comparing(DailyCount::date)).toList();
    }
}
