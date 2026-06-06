package com.justin.libradesk.controller;

import com.justin.libradesk.config.AppContext;
import com.justin.libradesk.domain.model.AuditLog;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.util.List;

/**
 * Read-only audit-log viewer (ADMIN). Loads recent entries and filters them
 * in-memory by actor/action substring.
 */
public class AuditController {

    @FXML
    private TextField actorFilter;
    @FXML
    private TextField actionFilter;
    @FXML
    private TableView<AuditLog> auditTable;
    @FXML
    private TableColumn<AuditLog, String> timeColumn;
    @FXML
    private TableColumn<AuditLog, String> actorColumn;
    @FXML
    private TableColumn<AuditLog, String> actionColumn;
    @FXML
    private TableColumn<AuditLog, String> entityColumn;
    @FXML
    private TableColumn<AuditLog, String> detailColumn;

    @FXML
    private void initialize() {
        timeColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCreatedAt().toString()));
        actorColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getActor()));
        actionColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getAction()));
        entityColumn.setCellValueFactory(c -> new SimpleStringProperty(entity(c.getValue())));
        detailColumn.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getDetail() == null ? "" : c.getValue().getDetail()));
        refresh();
    }

    @FXML
    private void onApply() {
        refresh();
    }

    @FXML
    private void onClear() {
        actorFilter.clear();
        actionFilter.clear();
        refresh();
    }

    @FXML
    private void onRefresh() {
        refresh();
    }

    private void refresh() {
        String actor = lower(actorFilter.getText());
        String action = lower(actionFilter.getText());
        List<AuditLog> rows = AppContext.get().auditLogService().recent().stream()
                .filter(entry -> actor.isEmpty() || entry.getActor().toLowerCase().contains(actor))
                .filter(entry -> action.isEmpty() || entry.getAction().toLowerCase().contains(action))
                .toList();
        auditTable.setItems(FXCollections.observableArrayList(rows));
    }

    private static String entity(AuditLog entry) {
        if (entry.getEntityType() == null) {
            return "";
        }
        return entry.getEntityId() == null
                ? entry.getEntityType()
                : entry.getEntityType() + "#" + entry.getEntityId();
    }

    private static String lower(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}
