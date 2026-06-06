package com.justin.libradesk.controller;

import com.justin.libradesk.config.AppContext;
import com.justin.libradesk.domain.model.Book;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

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

    private Integer parseYear() {
        String value = text(yearField);
        return value == null ? null : Integer.valueOf(value);
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
