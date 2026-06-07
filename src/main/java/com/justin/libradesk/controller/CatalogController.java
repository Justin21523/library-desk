package com.justin.libradesk.controller;

import com.justin.libradesk.config.AppContext;
import com.justin.libradesk.domain.enumtype.ClassificationScheme;
import com.justin.libradesk.domain.model.Author;
import com.justin.libradesk.domain.model.Book;
import com.justin.libradesk.domain.model.Category;
import com.justin.libradesk.domain.model.Publisher;
import com.justin.libradesk.domain.model.Subject;
import com.justin.libradesk.domain.service.CatalogService;
import com.justin.libradesk.infrastructure.marc.MarcData;
import com.justin.libradesk.util.Isbn;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.StringConverter;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Catalog management: search/list books and add new records, including
 * publisher, category, and authors selected from the reference data.
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
    private ComboBox<Publisher> publisherCombo;
    @FXML
    private ComboBox<Category> categoryCombo;
    @FXML
    private TextField callNumberField;
    @FXML
    private ComboBox<ClassificationScheme> schemeCombo;
    @FXML
    private ListView<Author> authorsList;
    @FXML
    private ListView<Subject> subjectsList;
    @FXML
    private TableView<Book> bookTable;
    @FXML
    private TableColumn<Book, String> titleColumn;
    @FXML
    private TableColumn<Book, String> isbnColumn;
    @FXML
    private TableColumn<Book, String> yearColumn;
    @FXML
    private TableColumn<Book, String> publisherColumn;
    @FXML
    private TableColumn<Book, String> categoryColumn;

    private final Map<Long, String> publisherNames = new HashMap<>();
    private final Map<Long, String> categoryNames = new HashMap<>();

    @FXML
    private void initialize() {
        publisherCombo.setConverter(nameConverter(Publisher::name));
        categoryCombo.setConverter(nameConverter(Category::name));
        schemeCombo.setItems(FXCollections.observableArrayList(ClassificationScheme.values()));
        authorsList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        authorsList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Author author, boolean empty) {
                super.updateItem(author, empty);
                setText(empty || author == null ? null : author.name());
            }
        });
        subjectsList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        subjectsList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Subject subject, boolean empty) {
                super.updateItem(subject, empty);
                setText(empty || subject == null ? null : subject.term());
            }
        });

        titleColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTitle()));
        isbnColumn.setCellValueFactory(c -> new SimpleStringProperty(nullToDash(c.getValue().getIsbn())));
        yearColumn.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getPublishedYear() == null ? "-" : c.getValue().getPublishedYear().toString()));
        publisherColumn.setCellValueFactory(c -> new SimpleStringProperty(
                lookup(publisherNames, c.getValue().getPublisherId())));
        categoryColumn.setCellValueFactory(c -> new SimpleStringProperty(
                lookup(categoryNames, c.getValue().getCategoryId())));

        loadReferenceData();
        onShowAll();
    }

    @FXML
    private void onSearch() {
        String fragment = searchField.getText();
        if (fragment == null || fragment.isBlank()) {
            onShowAll();
            return;
        }
        setBooks(catalog().searchByTitle(fragment.trim()));
    }

    @FXML
    private void onShowAll() {
        setBooks(catalog().listBooks());
    }

    @FXML
    private void onAddBook() {
        Book book = new Book();
        book.setTitle(text(titleField));
        book.setIsbn(text(isbnField));
        Publisher publisher = publisherCombo.getValue();
        Category category = categoryCombo.getValue();
        book.setPublisherId(publisher == null ? null : publisher.id());
        book.setCategoryId(category == null ? null : category.id());
        book.setCallNumber(text(callNumberField));
        book.setClassificationScheme(schemeCombo.getValue());
        for (Author author : authorsList.getSelectionModel().getSelectedItems()) {
            book.getAuthorIds().add(author.id());
        }
        for (Subject subject : subjectsList.getSelectionModel().getSelectedItems()) {
            book.getSubjectIds().add(subject.id());
        }
        try {
            book.setPublishedYear(parseYear());
            catalog().addBook(book, actor());
            clearForm();
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
            AppContext.get().csvService().writeBooks(file, catalog().listBooks());
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
                    catalog().addBook(book, actor());
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

    @FXML
    private void onImportMarc() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Import MARC records");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("MARC (.mrc, .xml)", "*.mrc", "*.xml"));
        File file = chooser.showOpenDialog(window());
        if (file == null) {
            return;
        }
        int imported = 0;
        var errors = new ArrayList<String>();
        try {
            for (MarcData data : AppContext.get().marcService().read(file)) {
                try {
                    AppContext.get().catalogService().importMarc(data, actor());
                    imported++;
                } catch (RuntimeException rowError) {
                    errors.add((data.book().getTitle() == null ? "(untitled)" : data.book().getTitle())
                            + ": " + rowError.getMessage());
                }
            }
        } catch (RuntimeException e) {
            Dialogs.error("Import failed: " + e.getMessage());
            return;
        }
        loadReferenceData();
        onShowAll();
        Dialogs.info("Imported " + imported + " record(s), skipped " + errors.size() + "."
                + (errors.isEmpty() ? "" : "\n" + String.join("\n", errors)));
    }

    @FXML
    private void onExportMarc() {
        FileChooser chooser = csvChooser("Export MARCXML", "catalog.xml");
        chooser.getExtensionFilters().setAll(new FileChooser.ExtensionFilter("MARCXML", "*.xml"));
        File file = chooser.showSaveDialog(window());
        if (file == null) {
            return;
        }
        try {
            AppContext.get().marcService().writeMarcXml(file, catalog().exportMarc());
            Dialogs.info("Exported to " + file.getName());
        } catch (RuntimeException e) {
            Dialogs.error("Export failed: " + e.getMessage());
        }
    }

    @FXML
    private void onViewMarc() {
        Book selected = bookTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Dialogs.error("Select a book to view its MARC record.");
            return;
        }
        String xml = selected.getMarcXml() != null
                ? selected.getMarcXml()
                : AppContext.get().marcService().toXmlString(catalog().toMarcData(selected));

        TextArea area = new TextArea(xml);
        area.setEditable(false);
        area.setWrapText(false);
        area.setPrefRowCount(24);
        area.setPrefColumnCount(80);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("MARC record");
        dialog.setHeaderText(selected.getTitle());
        dialog.getDialogPane().setContent(area);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    @FXML
    private void onSearchLoc() {
        TextField query = new TextField();
        query.setPromptText("ISBN or title");
        ListView<MarcData> results = new ListView<>();
        results.setPrefHeight(220);
        results.setPrefWidth(420);
        results.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(MarcData data, boolean empty) {
                super.updateItem(data, empty);
                setText(empty || data == null ? null : describe(data));
            }
        });

        Button search = new Button("Search");
        search.setOnAction(e -> {
            String text = query.getText() == null ? "" : query.getText().trim();
            if (text.isEmpty()) {
                return;
            }
            try {
                List<MarcData> found = Isbn.isValid(text)
                        ? AppContext.get().locSruClient().searchByIsbn(text)
                        : AppContext.get().locSruClient().searchByTitle(text);
                results.setItems(FXCollections.observableArrayList(found));
                if (found.isEmpty()) {
                    Dialogs.error("No records found at the Library of Congress.");
                }
            } catch (RuntimeException ex) {
                Dialogs.error("LoC search failed: " + ex.getMessage());
            }
        });

        VBox content = new VBox(8, new HBox(8, query, search), new Label("Results:"), results);
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Search Library of Congress");
        dialog.setHeaderText("Find a record to import (copy cataloging)");
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> choice = dialog.showAndWait();
        if (choice.isEmpty() || choice.get() != ButtonType.OK) {
            return;
        }
        MarcData selected = results.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Dialogs.error("Select a record to import.");
            return;
        }
        try {
            catalog().importMarc(selected, actor());
            loadReferenceData();
            onShowAll();
            Dialogs.info("Imported: " + selected.book().getTitle());
        } catch (RuntimeException ex) {
            Dialogs.error(ex.getMessage());
        }
    }

    private static String describe(MarcData data) {
        String title = data.book().getTitle() == null ? "(untitled)" : data.book().getTitle();
        return data.book().getIsbn() == null ? title : title + " (" + data.book().getIsbn() + ")";
    }

    private void loadReferenceData() {
        List<Publisher> publishers = catalog().listPublishers();
        List<Category> categories = catalog().listCategories();
        publisherCombo.setItems(FXCollections.observableArrayList(publishers));
        categoryCombo.setItems(FXCollections.observableArrayList(categories));
        authorsList.setItems(FXCollections.observableArrayList(catalog().listAuthors()));
        subjectsList.setItems(FXCollections.observableArrayList(catalog().listSubjects()));
        publisherNames.clear();
        publishers.forEach(p -> publisherNames.put(p.id(), p.name()));
        categoryNames.clear();
        categories.forEach(c -> categoryNames.put(c.id(), c.name()));
    }

    private Integer parseYear() {
        String value = text(yearField);
        return value == null ? null : Integer.valueOf(value);
    }

    private void clearForm() {
        titleField.clear();
        isbnField.clear();
        yearField.clear();
        publisherCombo.getSelectionModel().clearSelection();
        categoryCombo.getSelectionModel().clearSelection();
        callNumberField.clear();
        schemeCombo.getSelectionModel().clearSelection();
        authorsList.getSelectionModel().clearSelection();
        subjectsList.getSelectionModel().clearSelection();
    }

    private void setBooks(List<Book> books) {
        bookTable.setItems(FXCollections.observableArrayList(books));
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

    private static <T> StringConverter<T> nameConverter(java.util.function.Function<T, String> toName) {
        return new StringConverter<>() {
            @Override
            public String toString(T item) {
                return item == null ? "" : toName.apply(item);
            }

            @Override
            public T fromString(String string) {
                return null;
            }
        };
    }

    private static String lookup(Map<Long, String> names, Long id) {
        return id == null ? "-" : names.getOrDefault(id, "-");
    }

    private static String nullToDash(String value) {
        return value == null ? "-" : value;
    }

    private static String text(TextField field) {
        String value = field.getText();
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static CatalogService catalog() {
        return AppContext.get().catalogService();
    }

    private Window window() {
        return bookTable.getScene().getWindow();
    }

    private static String actor() {
        return AppContext.get().getCurrentUser().getUsername();
    }
}
