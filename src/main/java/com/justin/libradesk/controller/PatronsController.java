package com.justin.libradesk.controller;

import com.justin.libradesk.config.AppContext;
import com.justin.libradesk.domain.enumtype.PatronStatus;
import com.justin.libradesk.domain.enumtype.PatronType;
import com.justin.libradesk.domain.model.Patron;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;

/**
 * Patron management: register new members, list them, and suspend the selected
 * one. All persistence and rules live in {@code PatronService}.
 */
public class PatronsController {

    @FXML
    private TextField membershipField;
    @FXML
    private TextField nameField;
    @FXML
    private TextField emailField;
    @FXML
    private TextField phoneField;
    @FXML
    private ComboBox<PatronType> typeCombo;
    @FXML
    private TableView<Patron> patronTable;
    @FXML
    private TableColumn<Patron, String> membershipColumn;
    @FXML
    private TableColumn<Patron, String> nameColumn;
    @FXML
    private TableColumn<Patron, String> typeColumn;
    @FXML
    private TableColumn<Patron, String> statusColumn;

    @FXML
    private void initialize() {
        typeCombo.setItems(FXCollections.observableArrayList(PatronType.values()));
        membershipColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getMembershipNo()));
        nameColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFullName()));
        typeColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPatronType().name()));
        statusColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus().name()));
        refresh();
    }

    @FXML
    private void onRegister() {
        if (typeCombo.getValue() == null) {
            Dialogs.error("Please choose a patron type.");
            return;
        }
        Patron patron = new Patron(null, text(membershipField), text(nameField),
                text(emailField), text(phoneField), typeCombo.getValue(),
                PatronStatus.ACTIVE, LocalDateTime.now());
        try {
            AppContext.get().patronService().register(patron, actor());
            clearForm();
            refresh();
        } catch (RuntimeException e) {
            Dialogs.error(e.getMessage());
        }
    }

    @FXML
    private void onSuspend() {
        Patron selected = patronTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Dialogs.error("Select a patron to suspend.");
            return;
        }
        try {
            AppContext.get().patronService().suspend(selected.getId(), actor());
            refresh();
        } catch (RuntimeException e) {
            Dialogs.error(e.getMessage());
        }
    }

    @FXML
    private void onRefresh() {
        refresh();
    }

    @FXML
    private void onExportCsv() {
        FileChooser chooser = csvChooser("Export patrons", "patrons.csv");
        File file = chooser.showSaveDialog(window());
        if (file == null) {
            return;
        }
        try {
            AppContext.get().csvService().writePatrons(file, AppContext.get().patronService().listAll());
            Dialogs.info("Exported to " + file.getName());
        } catch (RuntimeException e) {
            Dialogs.error("Export failed: " + e.getMessage());
        }
    }

    @FXML
    private void onImportCsv() {
        FileChooser chooser = csvChooser("Import patrons", null);
        File file = chooser.showOpenDialog(window());
        if (file == null) {
            return;
        }
        int imported = 0;
        var errors = new ArrayList<String>();
        try {
            for (Patron patron : AppContext.get().csvService().readPatrons(file)) {
                try {
                    AppContext.get().patronService().register(patron, actor());
                    imported++;
                } catch (RuntimeException rowError) {
                    errors.add(patron.getMembershipNo() + ": " + rowError.getMessage());
                }
            }
        } catch (RuntimeException e) {
            Dialogs.error("Import failed: " + e.getMessage());
            return;
        }
        refresh();
        Dialogs.info("Imported " + imported + " patron(s), skipped " + errors.size() + "."
                + (errors.isEmpty() ? "" : "\n" + String.join("\n", errors)));
    }

    private void refresh() {
        patronTable.setItems(FXCollections.observableArrayList(
                AppContext.get().patronService().listAll()));
    }

    private FileChooser csvChooser(String title, String initialName) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        if (initialName != null) {
            chooser.setInitialFileName(initialName);
        }
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        return chooser;
    }

    private Window window() {
        return patronTable.getScene().getWindow();
    }

    private void clearForm() {
        membershipField.clear();
        nameField.clear();
        emailField.clear();
        phoneField.clear();
        typeCombo.getSelectionModel().clearSelection();
    }

    private static String text(TextField field) {
        String value = field.getText();
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String actor() {
        return AppContext.get().getCurrentUser().getUsername();
    }
}
