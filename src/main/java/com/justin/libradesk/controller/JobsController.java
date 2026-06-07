package com.justin.libradesk.controller;

import com.justin.libradesk.config.AppContext;
import com.justin.libradesk.domain.enumtype.Permission;
import com.justin.libradesk.infrastructure.scheduling.JobRun;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

/**
 * Lists the registered background jobs with their last-run outcome and lets an
 * admin run one on demand. Delegates to the {@code JobScheduler}; SETTINGS-gated.
 */
public class JobsController {

    @FXML
    private TableView<JobRun> jobTable;
    @FXML
    private TableColumn<JobRun, String> nameColumn;
    @FXML
    private TableColumn<JobRun, String> lastRunColumn;
    @FXML
    private TableColumn<JobRun, String> okColumn;
    @FXML
    private TableColumn<JobRun, String> detailColumn;

    @FXML
    private void initialize() {
        nameColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().name()));
        lastRunColumn.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().lastRun() == null ? "-" : c.getValue().lastRun().toString()));
        okColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().ok() ? "yes" : "NO"));
        detailColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().detail()));
        refresh();
    }

    @FXML
    private void onRunNow() {
        JobRun selected = jobTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Dialogs.error("Select a job first.");
            return;
        }
        try {
            AccessControl.require(Permission.SETTINGS);
            JobRun result = AppContext.get().jobScheduler().runNow(selected.name());
            refresh();
            Dialogs.info("Ran " + result.name() + ": " + (result.ok() ? "OK" : "failed — " + result.detail()));
        } catch (RuntimeException e) {
            Dialogs.error(e.getMessage());
        }
    }

    @FXML
    private void onRefresh() {
        refresh();
    }

    private void refresh() {
        jobTable.setItems(FXCollections.observableArrayList(AppContext.get().jobScheduler().list()));
    }
}
