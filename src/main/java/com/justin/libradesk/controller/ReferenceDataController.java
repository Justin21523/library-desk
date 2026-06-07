package com.justin.libradesk.controller;

import com.justin.libradesk.config.AppContext;
import com.justin.libradesk.domain.model.Author;
import com.justin.libradesk.domain.model.Category;
import com.justin.libradesk.domain.model.Publisher;
import com.justin.libradesk.domain.model.Subject;
import com.justin.libradesk.domain.service.CatalogService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

/**
 * Manages catalog reference data — authors, publishers, categories, and subjects.
 * Authors and subjects also manage authority see-from variants for the selected
 * heading. Delegates to {@code CatalogService} and {@code AuthorityService}.
 */
public class ReferenceDataController {

    @FXML
    private TextField authorField;
    @FXML
    private ListView<Author> authorList;
    @FXML
    private ListView<String> authorVariantList;
    @FXML
    private TextField authorVariantField;
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
    private ListView<Subject> subjectList;
    @FXML
    private ListView<String> subjectVariantList;
    @FXML
    private TextField subjectVariantField;

    @FXML
    private void initialize() {
        authorList.setCellFactory(l -> new ListCell<>() {
            @Override
            protected void updateItem(Author author, boolean empty) {
                super.updateItem(author, empty);
                setText(empty || author == null ? null : author.name());
            }
        });
        subjectList.setCellFactory(l -> new ListCell<>() {
            @Override
            protected void updateItem(Subject subject, boolean empty) {
                super.updateItem(subject, empty);
                setText(empty || subject == null ? null : subject.term());
            }
        });
        authorList.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, selected) -> refreshAuthorVariants(selected));
        subjectList.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, selected) -> refreshSubjectVariants(selected));
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

    @FXML
    private void onAddAuthorVariant() {
        Author author = authorList.getSelectionModel().getSelectedItem();
        if (author == null) {
            Dialogs.error("Select an author first.");
            return;
        }
        try {
            AppContext.get().authorityService().addAuthorVariant(author.id(), authorVariantField.getText(), actor());
            authorVariantField.clear();
            refreshAuthorVariants(author);
        } catch (RuntimeException e) {
            Dialogs.error(e.getMessage());
        }
    }

    @FXML
    private void onAddSubjectVariant() {
        Subject subject = subjectList.getSelectionModel().getSelectedItem();
        if (subject == null) {
            Dialogs.error("Select a subject first.");
            return;
        }
        try {
            AppContext.get().authorityService().addSubjectVariant(subject.id(), subjectVariantField.getText(), actor());
            subjectVariantField.clear();
            refreshSubjectVariants(subject);
        } catch (RuntimeException e) {
            Dialogs.error(e.getMessage());
        }
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
        authorList.setItems(FXCollections.observableArrayList(catalog.listAuthors()));
        subjectList.setItems(FXCollections.observableArrayList(catalog.listSubjects()));
        publisherList.setItems(FXCollections.observableArrayList(
                catalog.listPublishers().stream().map(Publisher::name).toList()));
        categoryList.setItems(FXCollections.observableArrayList(
                catalog.listCategories().stream().map(Category::name).toList()));
    }

    private void refreshAuthorVariants(Author author) {
        authorVariantList.setItems(author == null ? FXCollections.observableArrayList()
                : FXCollections.observableArrayList(
                        AppContext.get().authorityService().listAuthorVariants(author.id())));
    }

    private void refreshSubjectVariants(Subject subject) {
        subjectVariantList.setItems(subject == null ? FXCollections.observableArrayList()
                : FXCollections.observableArrayList(
                        AppContext.get().authorityService().listSubjectVariants(subject.id())));
    }

    private static CatalogService catalog() {
        return AppContext.get().catalogService();
    }

    private static String actor() {
        return AppContext.get().getCurrentUser().getUsername();
    }
}
