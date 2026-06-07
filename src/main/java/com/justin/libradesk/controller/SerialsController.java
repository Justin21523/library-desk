package com.justin.libradesk.controller;

import com.justin.libradesk.config.AppContext;
import com.justin.libradesk.domain.enumtype.Frequency;
import com.justin.libradesk.domain.enumtype.Permission;
import com.justin.libradesk.domain.model.Book;
import com.justin.libradesk.domain.model.SerialIssue;
import com.justin.libradesk.domain.model.Subscription;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Serials control screen: create subscriptions, record the next expected issue,
 * and check issues in. Delegates to {@code SerialsService}; CATALOG-gated.
 */
public class SerialsController {

    @FXML
    private ComboBox<Book> bookCombo;
    @FXML
    private TextField labelField;
    @FXML
    private ComboBox<Frequency> frequencyCombo;
    @FXML
    private DatePicker startPicker;

    @FXML
    private TableView<Subscription> subscriptionTable;
    @FXML
    private TableColumn<Subscription, String> subLabelColumn;
    @FXML
    private TableColumn<Subscription, String> subFrequencyColumn;
    @FXML
    private TableColumn<Subscription, String> subStatusColumn;
    @FXML
    private TableColumn<Subscription, String> subNextColumn;

    @FXML
    private TableView<SerialIssue> issueTable;
    @FXML
    private TableColumn<SerialIssue, String> issueEnumColumn;
    @FXML
    private TableColumn<SerialIssue, String> issueExpectedColumn;
    @FXML
    private TableColumn<SerialIssue, String> issueReceivedColumn;
    @FXML
    private TableColumn<SerialIssue, String> issueStatusColumn;

    @FXML
    private void initialize() {
        bookCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Book book) {
                return book == null ? "" : book.getTitle();
            }

            @Override
            public Book fromString(String string) {
                return null;
            }
        });
        bookCombo.setItems(FXCollections.observableArrayList(AppContext.get().catalogService().listBooks()));
        frequencyCombo.setItems(FXCollections.observableArrayList(Frequency.values()));
        startPicker.setValue(LocalDate.now());

        subLabelColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().label()));
        subFrequencyColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().frequency().name()));
        subStatusColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().status().name()));
        subNextColumn.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().nextExpected() == null ? "-" : c.getValue().nextExpected().toString()));

        issueEnumColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().enumeration()));
        issueExpectedColumn.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().expectedDate() == null ? "-" : c.getValue().expectedDate().toString()));
        issueReceivedColumn.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().receivedDate() == null ? "-" : c.getValue().receivedDate().toString()));
        issueStatusColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().status().name()));

        subscriptionTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, selected) -> showIssues(selected));
        refresh();
    }

    @FXML
    private void onSubscribe() {
        Book book = bookCombo.getValue();
        if (book == null || frequencyCombo.getValue() == null || startPicker.getValue() == null) {
            Dialogs.error("Select a bib, frequency and start date.");
            return;
        }
        try {
            AccessControl.require(Permission.CATALOG);
            AppContext.get().serialsService().subscribe(book.getId(), labelField.getText(),
                    frequencyCombo.getValue(), startPicker.getValue(), actor());
            labelField.clear();
            refresh();
        } catch (RuntimeException e) {
            Dialogs.error(e.getMessage());
        }
    }

    @FXML
    private void onExpectNext() {
        Subscription sub = subscriptionTable.getSelectionModel().getSelectedItem();
        if (sub == null) {
            Dialogs.error("Select a subscription first.");
            return;
        }
        TextInputDialog dialog = new TextInputDialog();
        dialog.setHeaderText("Expect next issue");
        dialog.setContentText("Enumeration (e.g. Vol 12, No 3):");
        Optional<String> enumeration = dialog.showAndWait();
        if (enumeration.isEmpty() || enumeration.get().isBlank()) {
            return;
        }
        try {
            AccessControl.require(Permission.CATALOG);
            AppContext.get().serialsService().expectNext(sub.id(), enumeration.get().trim(), actor());
            refresh();
            showIssues(sub);
        } catch (RuntimeException e) {
            Dialogs.error(e.getMessage());
        }
    }

    @FXML
    private void onCheckIn() {
        SerialIssue issue = issueTable.getSelectionModel().getSelectedItem();
        if (issue == null) {
            Dialogs.error("Select an issue first.");
            return;
        }
        try {
            AccessControl.require(Permission.CATALOG);
            AppContext.get().serialsService().checkIn(issue.id(), actor());
            Subscription sub = subscriptionTable.getSelectionModel().getSelectedItem();
            showIssues(sub);
        } catch (RuntimeException e) {
            Dialogs.error(e.getMessage());
        }
    }

    @FXML
    private void onRefresh() {
        refresh();
    }

    private void showIssues(Subscription sub) {
        issueTable.setItems(sub == null ? FXCollections.observableArrayList()
                : FXCollections.observableArrayList(AppContext.get().serialsService().issues(sub.id())));
    }

    private void refresh() {
        subscriptionTable.setItems(FXCollections.observableArrayList(
                AppContext.get().serialsService().listSubscriptions()));
    }

    private static String actor() {
        return AppContext.get().getCurrentUser().getUsername();
    }
}
