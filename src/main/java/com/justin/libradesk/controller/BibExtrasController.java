package com.justin.libradesk.controller;

import com.justin.libradesk.config.AppContext;
import com.justin.libradesk.domain.enumtype.Permission;
import com.justin.libradesk.domain.model.Book;
import com.justin.libradesk.domain.model.ELink;
import com.justin.libradesk.domain.model.Holding;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;

/**
 * Manages a bib's MFHD holdings and 856 e-resource links (with link checking).
 * Delegates to {@code HoldingService} and {@code ELinkService}; CATALOG-gated.
 */
public class BibExtrasController {

    @FXML
    private ComboBox<Book> bookCombo;

    @FXML
    private TableView<Holding> holdingTable;
    @FXML
    private TableColumn<Holding, String> holdingCallColumn;
    @FXML
    private TableColumn<Holding, String> holdingSummaryColumn;
    @FXML
    private TableColumn<Holding, String> holdingNoteColumn;
    @FXML
    private TextField callNumberField;
    @FXML
    private TextField summaryField;

    @FXML
    private TableView<ELink> linkTable;
    @FXML
    private TableColumn<ELink, String> linkUrlColumn;
    @FXML
    private TableColumn<ELink, String> linkLabelColumn;
    @FXML
    private TableColumn<ELink, String> linkStatusColumn;
    @FXML
    private TableColumn<ELink, String> linkCheckedColumn;
    @FXML
    private TextField urlField;
    @FXML
    private TextField linkLabelField;

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

        holdingCallColumn.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().callNumber() == null ? "" : c.getValue().callNumber()));
        holdingSummaryColumn.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().summary() == null ? "" : c.getValue().summary()));
        holdingNoteColumn.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().note() == null ? "" : c.getValue().note()));

        linkUrlColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().url()));
        linkLabelColumn.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().label() == null ? "" : c.getValue().label()));
        linkStatusColumn.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().lastStatus() == null ? "-" : String.valueOf(c.getValue().lastStatus())));
        linkCheckedColumn.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().lastChecked() == null ? "-" : c.getValue().lastChecked().toString()));
    }

    @FXML
    private void onLoad() {
        refresh();
    }

    @FXML
    private void onAddHolding() {
        Book book = bookCombo.getValue();
        if (book == null) {
            Dialogs.error("Select a bib first.");
            return;
        }
        try {
            AccessControl.require(Permission.CATALOG);
            AppContext.get().holdingService().save(new Holding(null, book.getId(), null,
                    text(callNumberField), text(summaryField), null), actor());
            callNumberField.clear();
            summaryField.clear();
            refresh();
        } catch (RuntimeException e) {
            Dialogs.error(e.getMessage());
        }
    }

    @FXML
    private void onDeleteHolding() {
        Holding selected = holdingTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Dialogs.error("Select a holding first.");
            return;
        }
        runCatalog(() -> AppContext.get().holdingService().delete(selected.id(), actor()));
    }

    @FXML
    private void onAddLink() {
        Book book = bookCombo.getValue();
        if (book == null) {
            Dialogs.error("Select a bib first.");
            return;
        }
        try {
            AccessControl.require(Permission.CATALOG);
            AppContext.get().eLinkService().add(book.getId(), urlField.getText(),
                    text(linkLabelField), actor());
            urlField.clear();
            linkLabelField.clear();
            refresh();
        } catch (RuntimeException e) {
            Dialogs.error(e.getMessage());
        }
    }

    @FXML
    private void onCheckLinks() {
        Book book = bookCombo.getValue();
        if (book == null) {
            Dialogs.error("Select a bib first.");
            return;
        }
        runCatalog(() -> AppContext.get().eLinkService().checkLinks(book.getId(), actor()));
    }

    @FXML
    private void onDeleteLink() {
        ELink selected = linkTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Dialogs.error("Select a link first.");
            return;
        }
        runCatalog(() -> AppContext.get().eLinkService().delete(selected.id(), actor()));
    }

    private void runCatalog(Runnable action) {
        try {
            AccessControl.require(Permission.CATALOG);
            action.run();
            refresh();
        } catch (RuntimeException e) {
            Dialogs.error(e.getMessage());
        }
    }

    private void refresh() {
        Book book = bookCombo.getValue();
        if (book == null) {
            return;
        }
        holdingTable.setItems(FXCollections.observableArrayList(
                AppContext.get().holdingService().listForBook(book.getId())));
        linkTable.setItems(FXCollections.observableArrayList(
                AppContext.get().eLinkService().listForBook(book.getId())));
    }

    private static String text(TextField field) {
        String value = field.getText();
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String actor() {
        return AppContext.get().getCurrentUser().getUsername();
    }
}
