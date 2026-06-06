package com.justin.libradesk.controller;

import com.justin.libradesk.config.AppContext;
import com.justin.libradesk.dto.DashboardStats;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

/**
 * Shows headline counts from {@code DashboardService}. Refresh re-queries.
 */
public class DashboardController {

    @FXML
    private Label booksLabel;
    @FXML
    private Label copiesLabel;
    @FXML
    private Label patronsLabel;
    @FXML
    private Label activeLoansLabel;
    @FXML
    private Label overdueLabel;
    @FXML
    private Label reservationsLabel;

    @FXML
    private void initialize() {
        refresh();
    }

    @FXML
    private void onRefresh() {
        refresh();
    }

    private void refresh() {
        try {
            DashboardStats stats = AppContext.get().dashboardService().getStats();
            booksLabel.setText(String.valueOf(stats.totalBooks()));
            copiesLabel.setText(stats.totalCopies() + " / " + stats.availableCopies());
            patronsLabel.setText(String.valueOf(stats.totalPatrons()));
            activeLoansLabel.setText(String.valueOf(stats.activeLoans()));
            overdueLabel.setText(String.valueOf(stats.overdueLoans()));
            reservationsLabel.setText(String.valueOf(stats.pendingReservations()));
        } catch (RuntimeException e) {
            Dialogs.error("Could not load dashboard: " + e.getMessage());
        }
    }
}
