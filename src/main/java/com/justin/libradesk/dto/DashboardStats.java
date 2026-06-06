package com.justin.libradesk.dto;

/**
 * Snapshot of headline counts shown on the dashboard.
 */
public record DashboardStats(long totalBooks, long totalCopies, long availableCopies,
                             long totalPatrons, long activeLoans, long overdueLoans,
                             long pendingReservations) {
}
