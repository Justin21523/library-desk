package com.justin.libradesk.domain.service;

import com.justin.libradesk.domain.enumtype.CopyStatus;
import com.justin.libradesk.domain.enumtype.LoanStatus;
import com.justin.libradesk.domain.enumtype.ReservationStatus;
import com.justin.libradesk.dto.DashboardStats;
import com.justin.libradesk.repository.BookCopyRepository;
import com.justin.libradesk.repository.BookRepository;
import com.justin.libradesk.repository.LoanRepository;
import com.justin.libradesk.repository.PatronRepository;
import com.justin.libradesk.repository.ReservationRepository;

/**
 * Computes the dashboard summary counts from the repositories.
 *
 * <p>Phase 3 keeps this simple by aggregating in memory; if the catalog grows
 * large, these become dedicated COUNT queries on the repositories.
 */
public class DashboardService {

    private final BookRepository bookRepository;
    private final BookCopyRepository bookCopyRepository;
    private final PatronRepository patronRepository;
    private final LoanRepository loanRepository;
    private final ReservationRepository reservationRepository;

    public DashboardService(BookRepository bookRepository,
                            BookCopyRepository bookCopyRepository,
                            PatronRepository patronRepository,
                            LoanRepository loanRepository,
                            ReservationRepository reservationRepository) {
        this.bookRepository = bookRepository;
        this.bookCopyRepository = bookCopyRepository;
        this.patronRepository = patronRepository;
        this.loanRepository = loanRepository;
        this.reservationRepository = reservationRepository;
    }

    public DashboardStats getStats() {
        long totalCopies = bookCopyRepository.findAll().size();
        long availableCopies = bookCopyRepository.findAll().stream()
                .filter(copy -> copy.getStatus() == CopyStatus.AVAILABLE)
                .count();
        long activeLoans = loanRepository.findAll().stream()
                .filter(loan -> loan.getStatus() == LoanStatus.ACTIVE)
                .count();
        long overdueLoans = loanRepository.findOverdue().size();
        long pendingReservations = reservationRepository.findAllActive().stream()
                .filter(reservation -> reservation.getStatus() == ReservationStatus.PENDING)
                .count();

        return new DashboardStats(
                bookRepository.findAll().size(),
                totalCopies,
                availableCopies,
                patronRepository.findAll().size(),
                activeLoans,
                overdueLoans,
                pendingReservations);
    }
}
