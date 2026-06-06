package com.justin.libradesk.controller;

import com.justin.libradesk.config.AppContext;
import com.justin.libradesk.domain.enumtype.Permission;
import com.justin.libradesk.domain.model.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
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
    private Button catalogButton;
    @FXML
    private Button catalogDataButton;
    @FXML
    private Button copiesButton;
    @FXML
    private Button patronsButton;
    @FXML
    private Button finesButton;
    @FXML
    private Button reportsButton;
    @FXML
    private Button usersButton;
    @FXML
    private Button settingsButton;

    @FXML
    private void initialize() {
        User user = AppContext.get().getCurrentUser();
        if (user != null) {
            userLabel.setText(user.getFullName() + " (" + user.getRole() + ")");
        }
        applyPermissions();
        onDashboard();
    }

    /** Hides sidebar entries the current role may not use. */
    private void applyPermissions() {
        gate(catalogButton, Permission.CATALOG);
        gate(catalogDataButton, Permission.CATALOG);
        gate(copiesButton, Permission.CATALOG);
        gate(patronsButton, Permission.PATRONS);
        gate(finesButton, Permission.FINES);
        gate(reportsButton, Permission.REPORTS);
        gate(usersButton, Permission.USERS);
        gate(settingsButton, Permission.SETTINGS);
    }

    private void gate(Button button, Permission permission) {
        boolean allowed = AccessControl.can(permission);
        button.setVisible(allowed);
        button.setManaged(allowed);
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
    private void onFines() {
        load("/fxml/FinesView.fxml");
    }

    @FXML
    private void onReports() {
        load("/fxml/ReportsView.fxml");
    }

    @FXML
    private void onUsers() {
        load("/fxml/UsersView.fxml");
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
