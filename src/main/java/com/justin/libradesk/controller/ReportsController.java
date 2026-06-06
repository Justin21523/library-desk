package com.justin.libradesk.controller;

import com.justin.libradesk.config.AppContext;
import com.justin.libradesk.domain.model.Loan;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.List;

/**
 * Reports screen: lists overdue loans, lets staff trigger the overdue sweep on
 * demand, and exports the overdue report as CSV.
 */
public class ReportsController {

    @FXML
    private TableView<Loan> overdueTable;
    @FXML
    private TableColumn<Loan, String> loanColumn;
    @FXML
    private TableColumn<Loan, String> copyColumn;
    @FXML
    private TableColumn<Loan, String> patronColumn;
    @FXML
    private TableColumn<Loan, String> dueColumn;
    @FXML
    private TableColumn<Loan, String> statusColumn;

    @FXML
    private void initialize() {
        loanColumn.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getId())));
        copyColumn.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getCopyId())));
        patronColumn.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getPatronId())));
        dueColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDueAt().toString()));
        statusColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus().name()));
        refresh();
    }

    @FXML
    private void onRefresh() {
        refresh();
    }

    @FXML
    private void onRunSweep() {
        try {
            int count = AppContext.get().circulationService().markOverdueLoans();
            refresh();
            Dialogs.info("Overdue sweep complete. " + count + " loan(s) marked overdue.");
        } catch (RuntimeException e) {
            Dialogs.error(e.getMessage());
        }
    }

    @FXML
    private void onExportCsv() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export overdue loans");
        chooser.setInitialFileName("overdue-loans.csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        File file = chooser.showSaveDialog(window());
        if (file == null) {
            return;
        }
        try {
            AppContext.get().csvService().writeLoans(file, AppContext.get().reportsService().overdueLoans());
            Dialogs.info("Exported to " + file.getName());
        } catch (RuntimeException e) {
            Dialogs.error("Export failed: " + e.getMessage());
        }
    }

    private void refresh() {
        List<Loan> overdue = AppContext.get().reportsService().overdueLoans();
        overdueTable.setItems(FXCollections.observableArrayList(overdue));
    }

    private javafx.stage.Window window() {
        Node anyNode = overdueTable;
        return anyNode.getScene().getWindow();
    }
}
