package com.justin.libradesk.controller;

import com.justin.libradesk.config.AppContext;
import com.justin.libradesk.domain.model.User;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

/**
 * Hosts the main application shell after login. Phase 1 only shows placeholder
 * content per section; the individual feature views are added in later phases.
 */
public class MainLayoutController {

    @FXML
    private Label userLabel;
    @FXML
    private Label contentLabel;

    @FXML
    private void initialize() {
        User user = AppContext.get().getCurrentUser();
        if (user != null) {
            userLabel.setText(user.getFullName() + " (" + user.getRole() + ")");
        }
    }

    @FXML
    private void onDashboard() {
        show("Dashboard");
    }

    @FXML
    private void onCatalog() {
        show("Catalog management");
    }

    @FXML
    private void onCopies() {
        show("Book copy management");
    }

    @FXML
    private void onPatrons() {
        show("Patron management");
    }

    @FXML
    private void onCirculation() {
        show("Circulation");
    }

    @FXML
    private void onReservations() {
        show("Reservations");
    }

    @FXML
    private void onReports() {
        show("Reports");
    }

    @FXML
    private void onSettings() {
        show("Settings");
    }

    @FXML
    private void onLogout() {
        AppContext.get().setCurrentUser(null);
        ViewNavigator.get().showLogin();
    }

    private void show(String section) {
        // TODO(phase2): load the section's own FXML view into the content area.
        contentLabel.setText(section + " — coming soon");
    }
}
