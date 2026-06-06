package com.justin.libradesk.controller;

import com.justin.libradesk.config.AppContext;
import com.justin.libradesk.domain.model.Loan;
import com.justin.libradesk.dto.DailyCount;
import com.justin.libradesk.dto.NamedCount;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.FileChooser;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Reports screen: overdue loans (with on-demand sweep and CSV/PDF export) plus
 * aggregate charts. Read data comes from {@code ReportsService}.
 */
public class ReportsController {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("MM-dd");

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
    private BarChart<String, Number> mostBorrowedChart;
    @FXML
    private BarChart<String, Number> byTypeChart;
    @FXML
    private LineChart<String, Number> loansPerDayChart;

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
        File file = chooser("overdue-loans.csv").showSaveDialog(window());
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

    @FXML
    private void onExportPdf() {
        File file = chooser("overdue-loans.pdf").showSaveDialog(window());
        if (file == null) {
            return;
        }
        try {
            AppContext.get().pdfService().writeOverdueReport(file, AppContext.get().reportsService().overdueLoans());
            Dialogs.info("Exported to " + file.getName());
        } catch (RuntimeException e) {
            Dialogs.error("Export failed: " + e.getMessage());
        }
    }

    private void refresh() {
        overdueTable.setItems(FXCollections.observableArrayList(
                AppContext.get().reportsService().overdueLoans()));
        populateCharts();
    }

    private void populateCharts() {
        var reports = AppContext.get().reportsService();
        setBars(mostBorrowedChart, reports.mostBorrowed(5));
        setBars(byTypeChart, reports.activeLoansByPatronType());

        XYChart.Series<String, Number> daily = new XYChart.Series<>();
        for (DailyCount point : reports.loansPerDay(14)) {
            daily.getData().add(new XYChart.Data<>(point.date().format(DAY), point.count()));
        }
        loansPerDayChart.getData().setAll(daily);
    }

    private void setBars(BarChart<String, Number> chart, List<NamedCount> counts) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (NamedCount count : counts) {
            series.getData().add(new XYChart.Data<>(count.name(), count.count()));
        }
        chart.getData().setAll(series);
    }

    private FileChooser chooser(String initialName) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export overdue loans");
        chooser.setInitialFileName(initialName);
        return chooser;
    }

    private javafx.stage.Window window() {
        Node anyNode = overdueTable;
        return anyNode.getScene().getWindow();
    }
}
