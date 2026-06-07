package com.justin.libradesk.controller;

import com.justin.libradesk.config.AppContext;
import com.justin.libradesk.domain.enumtype.MaterialType;
import com.justin.libradesk.domain.model.Book;
import com.justin.libradesk.infrastructure.marc.MarcData;
import com.justin.libradesk.infrastructure.marc.MarcLine;
import com.justin.libradesk.infrastructure.marc.MarcLineCodec;
import com.justin.libradesk.infrastructure.marc.MarcTemplates;
import com.justin.libradesk.infrastructure.marc.MarcValidator;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.Stage;
import org.marc4j.marc.Record;

import java.util.List;

/**
 * Field-level MARC editor: edit the leader and field lines (tag/indicators/inline
 * subfields), validate, and save. Saving stores the full record's MARCXML and
 * re-projects the structured catalog columns ({@code CatalogService.saveFromMarc}),
 * so the MARC record is the source of truth.
 */
public class MarcEditorController {

    /** One editable row backing the field table. */
    public static class FieldRow {
        private final SimpleStringProperty tag;
        private final SimpleStringProperty indicators;
        private final SimpleStringProperty content;

        public FieldRow(String tag, String indicators, String content) {
            this.tag = new SimpleStringProperty(tag);
            this.indicators = new SimpleStringProperty(indicators);
            this.content = new SimpleStringProperty(content);
        }

        public SimpleStringProperty tagProperty() {
            return tag;
        }

        public SimpleStringProperty indicatorsProperty() {
            return indicators;
        }

        public SimpleStringProperty contentProperty() {
            return content;
        }
    }

    @FXML
    private TextField leaderField;
    @FXML
    private TableView<FieldRow> fieldTable;
    @FXML
    private TableColumn<FieldRow, String> tagColumn;
    @FXML
    private TableColumn<FieldRow, String> indColumn;
    @FXML
    private TableColumn<FieldRow, String> contentColumn;
    @FXML
    private TextArea issuesArea;

    private final MarcLineCodec codec = new MarcLineCodec();
    private final MarcValidator validator = new MarcValidator();

    private Long editingBookId;
    private Runnable onSaved;

    @FXML
    private void initialize() {
        fieldTable.setEditable(true);
        tagColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        tagColumn.setCellValueFactory(c -> c.getValue().tagProperty());
        indColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        indColumn.setCellValueFactory(c -> c.getValue().indicatorsProperty());
        contentColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        contentColumn.setCellValueFactory(c -> c.getValue().contentProperty());
        loadNew();
    }

    /** Called by the opener to run a callback (e.g. close + refresh) after a save. */
    public void setOnSaved(Runnable onSaved) {
        this.onSaved = onSaved;
    }

    /** Starts a blank record from a minimal book workform. */
    public void loadNew() {
        editingBookId = null;
        leaderField.setText("00000nam a2200000 a 4500");
        fieldTable.setItems(FXCollections.observableArrayList(
                new FieldRow("008", "", ""),
                new FieldRow("245", "10", "$a")));
        issuesArea.clear();
    }

    /** Starts a new record from a material-type workform template. */
    public void loadTemplate(MaterialType type) {
        editingBookId = null;
        MarcLineCodec.DecodedMarc template = MarcTemplates.forType(type);
        leaderField.setText(template.leader());
        fieldTable.setItems(FXCollections.observableArrayList(
                template.fields().stream()
                        .map(l -> new FieldRow(l.tag(), l.indicators(), l.content()))
                        .toList()));
        issuesArea.clear();
    }

    /** Loads an existing book's MARC (stored record if present, else generated). */
    public void loadForBook(Book book) {
        editingBookId = book.getId();
        String xml = book.getMarcXml() != null
                ? book.getMarcXml()
                : AppContext.get().marcService().toXmlString(AppContext.get().catalogService().toMarcData(book));
        MarcLineCodec.DecodedMarc decoded = codec.fromRecord(AppContext.get().marcService().parseXml(xml));
        leaderField.setText(decoded.leader());
        fieldTable.setItems(FXCollections.observableArrayList(
                decoded.fields().stream()
                        .map(l -> new FieldRow(l.tag(), l.indicators(), l.content()))
                        .toList()));
        issuesArea.clear();
    }

    @FXML
    private void onAddField() {
        fieldTable.getItems().add(new FieldRow("", "", ""));
    }

    @FXML
    private void onRemoveField() {
        FieldRow selected = fieldTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            fieldTable.getItems().remove(selected);
        }
    }

    @FXML
    private void onMoveUp() {
        move(-1);
    }

    @FXML
    private void onMoveDown() {
        move(1);
    }

    @FXML
    private void onValidate() {
        showIssues(validator.validate(buildRecord()));
    }

    @FXML
    private void onSave() {
        Record record = buildRecord();
        List<String> issues = validator.validate(record);
        showIssues(issues);
        if (validator.hasErrors(issues)) {
            Dialogs.error("Fix the errors listed before saving.");
            return;
        }
        try {
            String xml = AppContext.get().marcService().toXml(record);
            MarcData data = AppContext.get().marcService().fromXmlString(xml);
            AppContext.get().catalogService().saveFromMarc(editingBookId, data, actor());
            if (onSaved != null) {
                onSaved.run();
            } else {
                close();
            }
        } catch (RuntimeException e) {
            Dialogs.error(e.getMessage());
        }
    }

    @FXML
    private void onCancel() {
        close();
    }

    private Record buildRecord() {
        List<MarcLine> lines = fieldTable.getItems().stream()
                .map(r -> new MarcLine(r.tagProperty().get(), r.indicatorsProperty().get(),
                        r.contentProperty().get()))
                .toList();
        return codec.toRecord(leaderField.getText(), lines);
    }

    private void move(int delta) {
        int index = fieldTable.getSelectionModel().getSelectedIndex();
        int target = index + delta;
        if (index < 0 || target < 0 || target >= fieldTable.getItems().size()) {
            return;
        }
        FieldRow row = fieldTable.getItems().remove(index);
        fieldTable.getItems().add(target, row);
        fieldTable.getSelectionModel().select(target);
    }

    private void showIssues(List<String> issues) {
        issuesArea.setText(issues.isEmpty() ? "No issues." : String.join("\n", issues));
    }

    private void close() {
        ((Stage) leaderField.getScene().getWindow()).close();
    }

    private static String actor() {
        return AppContext.get().getCurrentUser().getUsername();
    }
}
