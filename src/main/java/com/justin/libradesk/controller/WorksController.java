package com.justin.libradesk.controller;

import com.justin.libradesk.config.AppContext;
import com.justin.libradesk.domain.enumtype.Permission;
import com.justin.libradesk.domain.model.Book;
import com.justin.libradesk.domain.model.Work;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

/**
 * Groups bibs into FRBR works and shows a work's manifestations. Delegates to
 * {@code WorkService}; the grouping action is CATALOG-gated.
 */
public class WorksController {

    @FXML
    private TableView<Work> workTable;
    @FXML
    private TableColumn<Work, String> workTitleColumn;
    @FXML
    private TableColumn<Work, String> workAuthorColumn;
    @FXML
    private TableView<Book> manifestationTable;
    @FXML
    private TableColumn<Book, String> bookTitleColumn;
    @FXML
    private TableColumn<Book, String> bookIsbnColumn;
    @FXML
    private TableColumn<Book, String> bookYearColumn;

    @FXML
    private void initialize() {
        workTitleColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().title()));
        workAuthorColumn.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().author() == null ? "" : c.getValue().author()));
        bookTitleColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTitle()));
        bookIsbnColumn.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getIsbn() == null ? "" : c.getValue().getIsbn()));
        bookYearColumn.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getPublishedYear() == null ? "" : String.valueOf(c.getValue().getPublishedYear())));
        workTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, selected) -> showManifestations(selected));
        refresh();
    }

    @FXML
    private void onGroup() {
        try {
            AccessControl.require(Permission.CATALOG);
            int works = AppContext.get().workService().groupIntoWorks(actor());
            refresh();
            Dialogs.info("Grouped the catalog into " + works + " work(s).");
        } catch (RuntimeException e) {
            Dialogs.error(e.getMessage());
        }
    }

    @FXML
    private void onRefresh() {
        refresh();
    }

    private void showManifestations(Work work) {
        manifestationTable.setItems(work == null ? FXCollections.observableArrayList()
                : FXCollections.observableArrayList(AppContext.get().workService().manifestationsOf(work.id())));
    }

    private void refresh() {
        workTable.setItems(FXCollections.observableArrayList(AppContext.get().workService().listWorks()));
    }

    private static String actor() {
        return AppContext.get().getCurrentUser().getUsername();
    }
}
