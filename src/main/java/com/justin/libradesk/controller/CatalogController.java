package com.justin.libradesk.controller;

import com.justin.libradesk.config.AppContext;
import com.justin.libradesk.domain.model.Book;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.util.ArrayList;

/**
 * Catalog management: search and list books, and add a new bibliographic
 * record. Delegates to {@code CatalogService}.
 */
public class CatalogController {

    @FXML
    private TextField searchField;
    @FXML
    private TextField titleField;
    @FXML
    private TextField isbnField;
    @FXML
    private TextField yearField;
    @FXML
    private TableView<Book> bookTable;
    @FXML
    private TableColumn<Book, String> titleColumn;
    @FXML
    private TableColumn<Book, String> isbnColumn;
    @FXML
    private TableColumn<Book, String> yearColumn;

    @FXML
    private void initialize() {
        titleColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTitle()));
        isbnColumn.setCellValueFactory(c -> new SimpleStringProperty(nullToDash(c.getValue().getIsbn())));
        yearColumn.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getPublishedYear() == null ? "-" : c.getValue().getPublishedYear().toString()));
        onShowAll();
    }

    @FXML
    private void onSearch() {
        String fragment = searchField.getText();
        if (fragment == null || fragment.isBlank()) {
            onShowAll();
            return;
        }
        setBooks(AppContext.get().catalogService().searchByTitle(fragment.trim()));
    }

    @FXML
    private void onShowAll() {
        setBooks(AppContext.get().catalogService().listBooks());
    }

    @FXML
    private void onAddBook() {
        Book book = new Book();
        book.setTitle(text(titleField));
        book.setIsbn(text(isbnField));
        book.setPublishedYear(parseYear());
        try {
            AppContext.get().catalogService().addBook(book, actor());
            titleField.clear();
            isbnField.clear();
            yearField.clear();
            onShowAll();
        } catch (NumberFormatException e) {
            Dialogs.error("Year must be a number.");
        } catch (RuntimeException e) {
            Dialogs.error(e.getMessage());
        }
    }

    @FXML
    private void onExportCsv() {
        FileChooser chooser = csvChooser("Export books", "books.csv");
        File file = chooser.showSaveDialog(window());
        if (file == null) {
            return;
        }
        try {
            AppContext.get().csvService().writeBooks(file, AppContext.get().catalogService().listBooks());
            Dialogs.info("Exported to " + file.getName());
        } catch (RuntimeException e) {
            Dialogs.error("Export failed: " + e.getMessage());
        }
    }

    @FXML
    private void onImportCsv() {
        FileChooser chooser = csvChooser("Import books", null);
        File file = chooser.showOpenDialog(window());
        if (file == null) {
            return;
        }
        int imported = 0;
        var errors = new ArrayList<String>();
        try {
            for (Book book : AppContext.get().csvService().readBooks(file)) {
                try {
                    AppContext.get().catalogService().addBook(book, actor());
                    imported++;
                } catch (RuntimeException rowError) {
                    errors.add(book.getTitle() + ": " + rowError.getMessage());
                }
            }
        } catch (RuntimeException e) {
            Dialogs.error("Import failed: " + e.getMessage());
            return;
        }
        onShowAll();
        Dialogs.info("Imported " + imported + " book(s), skipped " + errors.size() + "."
                + (errors.isEmpty() ? "" : "\n" + String.join("\n", errors)));
    }

    private Integer parseYear() {
        String value = text(yearField);
        return value == null ? null : Integer.valueOf(value);
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
        return bookTable.getScene().getWindow();
    }

    private void setBooks(java.util.List<Book> books) {
        bookTable.setItems(FXCollections.observableArrayList(books));
    }

    private static String nullToDash(String value) {
        return value == null ? "-" : value;
    }

    private static String text(TextField field) {
        String value = field.getText();
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String actor() {
        return AppContext.get().getCurrentUser().getUsername();
    }
}
