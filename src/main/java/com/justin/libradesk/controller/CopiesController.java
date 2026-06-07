package com.justin.libradesk.controller;

import com.justin.libradesk.config.AppContext;
import com.justin.libradesk.domain.enumtype.CopyStatus;
import com.justin.libradesk.domain.model.Book;
import com.justin.libradesk.domain.model.BookCopy;
import com.justin.libradesk.domain.model.ShelfLocation;
import com.justin.libradesk.dto.SpineLabel;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private ComboBox<ShelfLocation> locationCombo;
    @FXML
    private TableView<BookCopy> copyTable;
    @FXML
    private TableColumn<BookCopy, String> barcodeColumn;
    @FXML
    private TableColumn<BookCopy, String> statusColumn;
    @FXML
    private TableColumn<BookCopy, String> shelfColumn;
    @FXML
    private TableColumn<BookCopy, String> locationColumn;

    private final Map<Long, String> locationNames = new HashMap<>();

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
        locationCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(ShelfLocation location) {
                return location == null ? "" : location.name();
            }

            @Override
            public ShelfLocation fromString(String string) {
                return null;
            }
        });
        bookCombo.valueProperty().addListener((obs, old, selected) -> refreshCopies());
        statusCombo.setItems(FXCollections.observableArrayList(CopyStatus.values()));
        loadLocations();
        barcodeColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getBarcode()));
        statusColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus().name()));
        shelfColumn.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getShelfLocation() == null ? "-" : c.getValue().getShelfLocation()));
        locationColumn.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getLocationId() == null ? "-"
                        : locationNames.getOrDefault(c.getValue().getLocationId(), "-")));
        onReloadBooks();
    }

    private void loadLocations() {
        List<ShelfLocation> locations = AppContext.get().locationService().listLocations();
        locationCombo.setItems(FXCollections.observableArrayList(locations));
        locationNames.clear();
        for (ShelfLocation location : locations) {
            locationNames.put(location.id(), location.name());
        }
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
        ShelfLocation location = locationCombo.getValue();
        copy.setLocationId(location == null ? null : location.id());
        try {
            AppContext.get().catalogService().addCopy(copy, actor());
            barcodeField.clear();
            shelfField.clear();
            locationCombo.setValue(null);
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

    @FXML
    private void onPrintSpineLabels() {
        Book book = bookCombo.getValue();
        if (book == null) {
            Dialogs.error("Select a book first.");
            return;
        }
        List<BookCopy> copies = AppContext.get().catalogService().listCopies(book.getId());
        if (copies.isEmpty()) {
            Dialogs.error("This book has no copies to label.");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save spine labels");
        chooser.setInitialFileName("spine-labels.pdf");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        File file = chooser.showSaveDialog(copyTable.getScene().getWindow());
        if (file == null) {
            return;
        }
        List<SpineLabel> labels = copies.stream()
                .map(copy -> new SpineLabel(book.getCallNumber(), copy.getBarcode(), book.getTitle()))
                .toList();
        try {
            AppContext.get().pdfService().writeSpineLabels(file, labels);
            Dialogs.info("Saved " + labels.size() + " label(s) to " + file.getName());
        } catch (RuntimeException e) {
            Dialogs.error("Could not write labels: " + e.getMessage());
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
