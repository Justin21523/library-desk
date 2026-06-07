package com.justin.libradesk.controller;

import com.justin.libradesk.config.AppContext;
import com.justin.libradesk.domain.model.Book;
import com.justin.libradesk.dto.CatalogRecord;
import com.justin.libradesk.dto.CatalogSearchResult;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.util.List;
import java.util.Map;

/**
 * OPAC / catalog search screen: keyword search with facet narrowing and a record
 * detail pane. Reads from {@code CatalogSearchService}; facet narrowing filters
 * the loaded keyword result in memory.
 */
public class CatalogSearchController {

    private static final String ANY = "(any)";

    @FXML
    private TextField keywordField;
    @FXML
    private ComboBox<String> authorFacet;
    @FXML
    private ComboBox<String> subjectFacet;
    @FXML
    private ComboBox<String> yearFacet;
    @FXML
    private ComboBox<String> languageFacet;
    @FXML
    private ComboBox<String> materialFacet;
    @FXML
    private TableView<CatalogRecord> resultTable;
    @FXML
    private TableColumn<CatalogRecord, String> titleColumn;
    @FXML
    private TableColumn<CatalogRecord, String> authorsColumn;
    @FXML
    private TableColumn<CatalogRecord, String> yearColumn;
    @FXML
    private TableColumn<CatalogRecord, String> callColumn;
    @FXML
    private TextArea detailArea;

    /** The current keyword result; facet combos narrow this in memory. */
    private List<CatalogRecord> base = List.of();
    private boolean populating;

    @FXML
    private void initialize() {
        titleColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().book().getTitle()));
        authorsColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().authorsJoined()));
        yearColumn.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().book().getPublishedYear() == null ? "" : c.getValue().book().getPublishedYear().toString()));
        callColumn.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().book().getCallNumber() == null ? "" : c.getValue().book().getCallNumber()));
        resultTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, selected) -> showDetail(selected));
        for (ComboBox<String> facet : facets()) {
            facet.valueProperty().addListener((obs, old, value) -> applyFacets());
        }
        onSearch();
    }

    @FXML
    private void onSearch() {
        CatalogSearchResult result = AppContext.get().catalogSearchService().search(keywordField.getText());
        base = result.records();
        populateFacet(authorFacet, result.authorFacet());
        populateFacet(subjectFacet, result.subjectFacet());
        populateFacet(yearFacet, result.yearFacet());
        populateFacet(languageFacet, result.languageFacet());
        populateFacet(materialFacet, result.materialFacet());
        resultTable.setItems(FXCollections.observableArrayList(base));
        detailArea.clear();
    }

    @FXML
    private void onClear() {
        keywordField.clear();
        onSearch();
    }

    private void applyFacets() {
        if (populating) {
            return;
        }
        List<CatalogRecord> filtered = base.stream()
                .filter(r -> selectedOrAny(authorFacet) == null || r.authors().contains(selectedOrAny(authorFacet)))
                .filter(r -> selectedOrAny(subjectFacet) == null || r.subjects().contains(selectedOrAny(subjectFacet)))
                .filter(r -> matchesYear(r) && matchesLanguage(r) && matchesMaterial(r))
                .toList();
        resultTable.setItems(FXCollections.observableArrayList(filtered));
    }

    private boolean matchesYear(CatalogRecord r) {
        String year = selectedOrAny(yearFacet);
        return year == null || year.equals(r.book().getPublishedYear() == null ? null
                : r.book().getPublishedYear().toString());
    }

    private boolean matchesLanguage(CatalogRecord r) {
        String language = selectedOrAny(languageFacet);
        return language == null || language.equals(r.book().getLanguage());
    }

    private boolean matchesMaterial(CatalogRecord r) {
        String material = selectedOrAny(materialFacet);
        return material == null || material.equals(
                r.book().getMaterialType() == null ? null : r.book().getMaterialType().name());
    }

    private void populateFacet(ComboBox<String> facet, Map<String, Long> counts) {
        populating = true;
        var items = FXCollections.<String>observableArrayList(ANY);
        counts.forEach((value, count) -> items.add(value + " (" + count + ")"));
        facet.setItems(items);
        facet.getSelectionModel().selectFirst();
        populating = false;
    }

    /** @return the selected facet value without its count suffix, or null for "(any)". */
    private String selectedOrAny(ComboBox<String> facet) {
        String value = facet.getValue();
        if (value == null || value.equals(ANY)) {
            return null;
        }
        int suffix = value.lastIndexOf(" (");
        return suffix > 0 ? value.substring(0, suffix) : value;
    }

    private void showDetail(CatalogRecord record) {
        if (record == null) {
            detailArea.clear();
            return;
        }
        Book book = record.book();
        StringBuilder sb = new StringBuilder();
        line(sb, "Title", book.getTitle());
        line(sb, "Author(s)", record.authorsJoined());
        line(sb, "Publisher", record.publisher());
        line(sb, "Year", book.getPublishedYear() == null ? null : book.getPublishedYear().toString());
        line(sb, "Edition", book.getEdition());
        line(sb, "Extent", book.getExtent());
        line(sb, "Series", book.getSeries());
        line(sb, "Language", book.getLanguage());
        line(sb, "Material", book.getMaterialType() == null ? null : book.getMaterialType().name());
        line(sb, "Call number", book.getCallNumber());
        line(sb, "ISBN", book.getIsbn());
        line(sb, "Subjects", String.join("; ", record.subjects()));
        line(sb, "Summary", book.getSummary());
        detailArea.setText(sb.toString().strip());
    }

    private void line(StringBuilder sb, String label, String value) {
        if (value != null && !value.isBlank()) {
            sb.append(label).append(": ").append(value).append('\n');
        }
    }

    private List<ComboBox<String>> facets() {
        return List.of(authorFacet, subjectFacet, yearFacet, languageFacet, materialFacet);
    }
}
