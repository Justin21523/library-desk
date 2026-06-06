package com.justin.libradesk.controller;

import com.justin.libradesk.config.AppContext;
import com.justin.libradesk.domain.enumtype.Permission;
import com.justin.libradesk.domain.model.Fine;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

/**
 * Lists unpaid fines and lets staff settle them (pay or waive). Delegates to
 * {@code FineService}; guarded by the FINES permission.
 */
public class FinesController {

    @FXML
    private TableView<Fine> fineTable;
    @FXML
    private TableColumn<Fine, String> fineColumn;
    @FXML
    private TableColumn<Fine, String> patronColumn;
    @FXML
    private TableColumn<Fine, String> loanColumn;
    @FXML
    private TableColumn<Fine, String> amountColumn;
    @FXML
    private TableColumn<Fine, String> createdColumn;

    @FXML
    private void initialize() {
        fineColumn.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getId())));
        patronColumn.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getPatronId())));
        loanColumn.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getLoanId() == null ? "-" : String.valueOf(c.getValue().getLoanId())));
        amountColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getAmount().toPlainString()));
        createdColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCreatedAt().toString()));
        refresh();
    }

    @FXML
    private void onPay() {
        settle(true);
    }

    @FXML
    private void onWaive() {
        settle(false);
    }

    private void settle(boolean pay) {
        Fine selected = fineTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Dialogs.error("Select a fine first.");
            return;
        }
        try {
            AccessControl.require(Permission.FINES);
            if (pay) {
                AppContext.get().fineService().pay(selected.getId(), actor());
            } else {
                AppContext.get().fineService().waive(selected.getId(), actor());
            }
            refresh();
        } catch (RuntimeException e) {
            Dialogs.error(e.getMessage());
        }
    }

    @FXML
    private void onRefresh() {
        refresh();
    }

    private void refresh() {
        fineTable.setItems(FXCollections.observableArrayList(AppContext.get().fineService().listUnpaid()));
    }

    private static String actor() {
        return AppContext.get().getCurrentUser().getUsername();
    }
}
