package com.justin.libradesk.controller;

import com.justin.libradesk.config.AppContext;
import com.justin.libradesk.domain.model.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Hosts the application shell after login. Each sidebar button swaps a feature
 * view into the central content area. Controllers for those views pull their
 * services from {@link AppContext}, so this class only handles navigation.
 */
public class MainLayoutController {

    @FXML
    private Label userLabel;
    @FXML
    private StackPane contentArea;

    @FXML
    private void initialize() {
        User user = AppContext.get().getCurrentUser();
        if (user != null) {
            userLabel.setText(user.getFullName() + " (" + user.getRole() + ")");
        }
        onDashboard();
    }

    @FXML
    private void onDashboard() {
        load("/fxml/DashboardView.fxml");
    }

    @FXML
    private void onCatalog() {
        load("/fxml/CatalogView.fxml");
    }

    @FXML
    private void onReferenceData() {
        load("/fxml/ReferenceDataView.fxml");
    }

    @FXML
    private void onCopies() {
        load("/fxml/CopiesView.fxml");
    }

    @FXML
    private void onPatrons() {
        load("/fxml/PatronsView.fxml");
    }

    @FXML
    private void onCirculation() {
        load("/fxml/CirculationView.fxml");
    }

    @FXML
    private void onReservations() {
        load("/fxml/ReservationsView.fxml");
    }

    @FXML
    private void onReports() {
        load("/fxml/ReportsView.fxml");
    }

    @FXML
    private void onSettings() {
        load("/fxml/SettingsView.fxml");
    }

    @FXML
    private void onLogout() {
        AppContext.get().setCurrentUser(null);
        ViewNavigator.get().showLogin();
    }

    private void load(String fxmlResource) {
        try {
            Parent view = new FXMLLoader(getClass().getResource(fxmlResource)).load();
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load view: " + fxmlResource, e);
        }
    }
}
