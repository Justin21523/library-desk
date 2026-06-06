package com.justin.libradesk.controller;

import com.justin.libradesk.config.AppContext;
import com.justin.libradesk.domain.enumtype.CopyStatus;
import com.justin.libradesk.domain.model.Book;
import com.justin.libradesk.domain.model.BookCopy;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;

/**
 * Book copy management: pick a book, list its copies, add new copies, and
 * change a copy's status. Delegates to {@code CatalogService}.
 */
public class CopiesController {

    @FXML
    private ComboBox<Book> bookCombo;
    @FXML
    private TextField barcodeField;
    @FXML
    private TextField shelfField;
    @FXML
    private ComboBox<CopyStatus> statusCombo;
    @FXML
    private TableView<BookCopy> copyTable;
    @FXML
    private TableColumn<BookCopy, String> barcodeColumn;
    @FXML
    private TableColumn<BookCopy, String> statusColumn;
    @FXML
    private TableColumn<BookCopy, String> shelfColumn;

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
        bookCombo.valueProperty().addListener((obs, old, selected) -> refreshCopies());
        statusCombo.setItems(FXCollections.observableArrayList(CopyStatus.values()));
        barcodeColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getBarcode()));
        statusColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus().name()));
        shelfColumn.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getShelfLocation() == null ? "-" : c.getValue().getShelfLocation()));
        onReloadBooks();
    }

    @FXML
    private void onReloadBooks() {
        bookCombo.setItems(FXCollections.observableArrayList(AppContext.get().catalogService().listBooks()));
    }

    @FXML
    private void onAddCopy() {
        Book book = bookCombo.getValue();
        if (book == null) {
            Dialogs.error("Select a book first.");
            return;
        }
        BookCopy copy = new BookCopy();
        copy.setBookId(book.getId());
        copy.setBarcode(text(barcodeField));
        copy.setShelfLocation(text(shelfField));
        try {
            AppContext.get().catalogService().addCopy(copy, actor());
            barcodeField.clear();
            shelfField.clear();
            refreshCopies();
        } catch (RuntimeException e) {
            Dialogs.error(e.getMessage());
        }
    }

    @FXML
    private void onSetStatus() {
        BookCopy selected = copyTable.getSelectionModel().getSelectedItem();
        if (selected == null || statusCombo.getValue() == null) {
            Dialogs.error("Select a copy and a target status.");
            return;
        }
        try {
            AppContext.get().catalogService().updateCopyStatus(selected.getId(), statusCombo.getValue(), actor());
            refreshCopies();
        } catch (RuntimeException e) {
            Dialogs.error(e.getMessage());
        }
    }

    private void refreshCopies() {
        Book book = bookCombo.getValue();
        if (book == null) {
            copyTable.getItems().clear();
            return;
        }
        copyTable.setItems(FXCollections.observableArrayList(
                AppContext.get().catalogService().listCopies(book.getId())));
    }

    private static String text(TextField field) {
        String value = field.getText();
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String actor() {
        return AppContext.get().getCurrentUser().getUsername();
    }
}
