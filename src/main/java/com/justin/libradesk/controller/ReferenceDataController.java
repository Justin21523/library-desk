package com.justin.libradesk.controller;

import com.justin.libradesk.config.AppContext;
import com.justin.libradesk.domain.model.Author;
import com.justin.libradesk.domain.model.Category;
import com.justin.libradesk.domain.model.Publisher;
import com.justin.libradesk.domain.model.Subject;
import com.justin.libradesk.domain.service.CatalogService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

/**
 * Manages catalog reference data — authors, publishers, and categories — each
 * in its own tab. Delegates to {@code CatalogService}.
 */
public class ReferenceDataController {

    @FXML
    private TextField authorField;
    @FXML
    private ListView<String> authorList;
    @FXML
    private TextField publisherField;
    @FXML
    private ListView<String> publisherList;
    @FXML
    private TextField categoryField;
    @FXML
    private ListView<String> categoryList;
    @FXML
    private TextField subjectField;
    @FXML
    private ListView<String> subjectList;

    @FXML
    private void initialize() {
        refresh();
    }

    @FXML
    private void onAddAuthor() {
        add(authorField, () -> catalog().addAuthor(authorField.getText(), actor()));
    }

    @FXML
    private void onAddPublisher() {
        add(publisherField, () -> catalog().addPublisher(publisherField.getText(), actor()));
    }

    @FXML
    private void onAddCategory() {
        add(categoryField, () -> catalog().addCategory(categoryField.getText(), actor()));
    }

    @FXML
    private void onAddSubject() {
        add(subjectField, () -> catalog().addSubject(subjectField.getText(), actor()));
    }

    private void add(TextField field, Runnable action) {
        try {
            action.run();
            field.clear();
            refresh();
        } catch (RuntimeException e) {
            Dialogs.error(e.getMessage());
        }
    }

    private void refresh() {
        CatalogService catalog = catalog();
        authorList.setItems(FXCollections.observableArrayList(
                catalog.listAuthors().stream().map(Author::name).toList()));
        publisherList.setItems(FXCollections.observableArrayList(
                catalog.listPublishers().stream().map(Publisher::name).toList()));
        categoryList.setItems(FXCollections.observableArrayList(
                catalog.listCategories().stream().map(Category::name).toList()));
        subjectList.setItems(FXCollections.observableArrayList(
                catalog.listSubjects().stream().map(Subject::term).toList()));
    }

    private static CatalogService catalog() {
        return AppContext.get().catalogService();
    }

    private static String actor() {
        return AppContext.get().getCurrentUser().getUsername();
    }
}
