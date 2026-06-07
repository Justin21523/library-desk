package com.justin.libradesk.controller;

import com.justin.libradesk.config.AppContext;
import com.justin.libradesk.domain.model.Author;
import com.justin.libradesk.domain.model.Category;
import com.justin.libradesk.domain.model.Publisher;
import com.justin.libradesk.domain.model.Subject;
import com.justin.libradesk.domain.service.CatalogService;
import com.justin.libradesk.dto.AuthoritySuggestion;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;

import java.util.List;
import java.util.Optional;

/**
 * Manages catalog reference data — authors, publishers, categories, and subjects.
 * Authors and subjects also manage authority see-from variants for the selected
 * heading. Delegates to {@code CatalogService} and {@code AuthorityService}.
 */
public class ReferenceDataController {

    @FXML
    private TextField authorField;
    @FXML
    private TextField authorRenameField;
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
    private TextField subjectRenameField;
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

    @FXML
    private void onRenameAuthor() {
        Author author = authorList.getSelectionModel().getSelectedItem();
        if (author == null) {
            Dialogs.error("Select an author to rename.");
            return;
        }
        runAndRefresh(() -> AppContext.get().authorityService()
                .renameAuthor(author.id(), authorRenameField.getText(), actor()), authorRenameField);
    }

    @FXML
    private void onRenameSubject() {
        Subject subject = subjectList.getSelectionModel().getSelectedItem();
        if (subject == null) {
            Dialogs.error("Select a subject to rename.");
            return;
        }
        runAndRefresh(() -> AppContext.get().authorityService()
                .renameSubject(subject.id(), subjectRenameField.getText(), actor()), subjectRenameField);
    }

    @FXML
    private void onMergeAuthor() {
        Author from = authorList.getSelectionModel().getSelectedItem();
        if (from == null) {
            Dialogs.error("Select the author to merge (it will be removed).");
            return;
        }
        List<Author> others = catalog().listAuthors().stream()
                .filter(a -> !a.id().equals(from.id())).toList();
        choose("Merge author", "Merge \"" + from.name() + "\" into:",
                others.stream().map(Author::name).toList()).ifPresent(name -> runAndRefresh(() ->
                AppContext.get().authorityService().mergeAuthor(from.id(),
                        others.get(indexOfName(others, name, Author::name)).id(), actor()), null));
    }

    @FXML
    private void onMergeSubject() {
        Subject from = subjectList.getSelectionModel().getSelectedItem();
        if (from == null) {
            Dialogs.error("Select the subject to merge (it will be removed).");
            return;
        }
        List<Subject> others = catalog().listSubjects().stream()
                .filter(s -> !s.id().equals(from.id())).toList();
        choose("Merge subject", "Merge \"" + from.term() + "\" into:",
                others.stream().map(Subject::term).toList()).ifPresent(term -> runAndRefresh(() ->
                AppContext.get().authorityService().mergeSubject(from.id(),
                        others.get(indexOfName(others, term, Subject::term)).id(), actor()), null));
    }

    @FXML
    private void onSuggestAuthor() {
        suggest(AppContext.get().authorityService()::suggestNames)
                .ifPresent(label -> runAndRefresh(() -> catalog().addAuthor(label, actor()), null));
    }

    @FXML
    private void onSuggestSubject() {
        suggest(AppContext.get().authorityService()::suggestSubjects)
                .ifPresent(label -> runAndRefresh(() -> catalog().addSubject(label, actor()), null));
    }

    private Optional<String> suggest(java.util.function.Function<String, List<AuthoritySuggestion>> lookup) {
        TextInputDialog input = new TextInputDialog();
        input.setTitle("Suggest from Library of Congress");
        input.setHeaderText("Enter a search term");
        Optional<String> query = input.showAndWait();
        if (query.isEmpty() || query.get().isBlank()) {
            return Optional.empty();
        }
        List<AuthoritySuggestion> results;
        try {
            results = lookup.apply(query.get().trim());
        } catch (RuntimeException e) {
            Dialogs.error("Lookup failed: " + e.getMessage());
            return Optional.empty();
        }
        if (results.isEmpty()) {
            Dialogs.error("No authorized headings found.");
            return Optional.empty();
        }
        return choose("Suggested headings", "Choose an authorized heading",
                results.stream().map(AuthoritySuggestion::label).toList());
    }

    private Optional<String> choose(String title, String header, List<String> options) {
        if (options.isEmpty()) {
            Dialogs.error("Nothing to choose from.");
            return Optional.empty();
        }
        ChoiceDialog<String> dialog = new ChoiceDialog<>(options.get(0), options);
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        return dialog.showAndWait();
    }

    private static <T> int indexOfName(List<T> items, String name, java.util.function.Function<T, String> label) {
        for (int i = 0; i < items.size(); i++) {
            if (label.apply(items.get(i)).equals(name)) {
                return i;
            }
        }
        return 0;
    }

    private void runAndRefresh(Runnable action, TextField fieldToClear) {
        try {
            action.run();
            if (fieldToClear != null) {
                fieldToClear.clear();
            }
            refresh();
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
