package com.justin.libradesk.controller;

import com.justin.libradesk.config.AppContext;
import com.justin.libradesk.domain.enumtype.Permission;
import com.justin.libradesk.domain.model.Fine;
import com.justin.libradesk.domain.model.Loan;
import com.justin.libradesk.domain.model.Patron;
import com.justin.libradesk.domain.model.Reservation;
import com.justin.libradesk.dto.PatronAccount;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * The circulation desk's view of one patron: outstanding loans, active holds,
 * unpaid fines and blocks, with actions to take payments, waive fines, and
 * declare items lost or damaged. Delegates to {@code PatronAccountService},
 * {@code FineService} and {@code CirculationService}.
 */
public class PatronAccountController {

    @FXML
    private ComboBox<Patron> patronCombo;
    @FXML
    private javafx.scene.control.Label blocksLabel;
    @FXML
    private javafx.scene.control.Label balanceLabel;

    @FXML
    private TableView<Loan> loanTable;
    @FXML
    private TableColumn<Loan, String> loanIdColumn;
    @FXML
    private TableColumn<Loan, String> loanCopyColumn;
    @FXML
    private TableColumn<Loan, String> loanDueColumn;
    @FXML
    private TableColumn<Loan, String> loanStatusColumn;
    @FXML
    private TableColumn<Loan, String> loanRenewalsColumn;

    @FXML
    private TableView<Reservation> holdTable;
    @FXML
    private TableColumn<Reservation, String> holdBookColumn;
    @FXML
    private TableColumn<Reservation, String> holdStatusColumn;
    @FXML
    private TableColumn<Reservation, String> holdPositionColumn;

    @FXML
    private TableView<Fine> fineTable;
    @FXML
    private TableColumn<Fine, String> fineIdColumn;
    @FXML
    private TableColumn<Fine, String> fineTypeColumn;
    @FXML
    private TableColumn<Fine, String> fineAmountColumn;
    @FXML
    private TableColumn<Fine, String> fineBalanceColumn;
    @FXML
    private TableColumn<Fine, String> fineStatusColumn;

    @FXML
    private void initialize() {
        patronCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Patron patron) {
                return patron == null ? "" : patron.getFullName() + " (" + patron.getMembershipNo() + ")";
            }

            @Override
            public Patron fromString(String string) {
                return null;
            }
        });
        loanIdColumn.setCellValueFactory(c -> str(c.getValue().getId()));
        loanCopyColumn.setCellValueFactory(c -> str(c.getValue().getCopyId()));
        loanDueColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDueAt().toString()));
        loanStatusColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus().name()));
        loanRenewalsColumn.setCellValueFactory(c -> str(c.getValue().getRenewalCount()));

        holdBookColumn.setCellValueFactory(c -> str(c.getValue().getBookId()));
        holdStatusColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus().name()));
        holdPositionColumn.setCellValueFactory(c -> str(c.getValue().getQueuePosition()));

        fineIdColumn.setCellValueFactory(c -> str(c.getValue().getId()));
        fineTypeColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFeeType().name()));
        fineAmountColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getAmount().toPlainString()));
        fineBalanceColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().balance().toPlainString()));
        fineStatusColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus().name()));

        patronCombo.setItems(FXCollections.observableArrayList(AppContext.get().patronService().listAll()));
    }

    @FXML
    private void onLoad() {
        refresh();
    }

    @FXML
    private void onRefresh() {
        refresh();
    }

    @FXML
    private void onPay() {
        Fine fine = fineTable.getSelectionModel().getSelectedItem();
        if (fine == null) {
            Dialogs.error("Select a fine first.");
            return;
        }
        Optional<BigDecimal> amount = promptAmount("Pay fine",
                "Payment amount (balance " + fine.balance().toPlainString() + "):",
                fine.balance().toPlainString());
        amount.ifPresent(value -> run(() ->
                AppContext.get().fineService().pay(fine.getId(), value, "CASH", actor())));
    }

    @FXML
    private void onWaive() {
        Fine fine = fineTable.getSelectionModel().getSelectedItem();
        if (fine == null) {
            Dialogs.error("Select a fine first.");
            return;
        }
        TextInputDialog dialog = new TextInputDialog();
        dialog.setHeaderText("Waive fine");
        dialog.setContentText("Reason:");
        dialog.showAndWait().ifPresent(reason ->
                run(() -> AppContext.get().fineService().waive(fine.getId(), reason, actor())));
    }

    @FXML
    private void onMarkLost() {
        Loan loan = loanTable.getSelectionModel().getSelectedItem();
        if (loan == null) {
            Dialogs.error("Select a loan first.");
            return;
        }
        promptAmount("Mark lost", "Replacement cost:", "0.00").ifPresent(cost ->
                run(() -> AppContext.get().circulationService().markLost(loan.getCopyId(), cost, actor())));
    }

    @FXML
    private void onMarkDamaged() {
        Loan loan = loanTable.getSelectionModel().getSelectedItem();
        if (loan == null) {
            Dialogs.error("Select a loan first.");
            return;
        }
        promptAmount("Mark damaged", "Damage fee:", "0.00").ifPresent(fee ->
                run(() -> AppContext.get().circulationService().markDamaged(loan.getCopyId(), fee, actor())));
    }

    private void run(Runnable action) {
        try {
            AccessControl.require(Permission.FINES);
            action.run();
            refresh();
        } catch (RuntimeException e) {
            Dialogs.error(e.getMessage());
        }
    }

    private Optional<BigDecimal> promptAmount(String title, String prompt, String initial) {
        TextInputDialog dialog = new TextInputDialog(initial);
        dialog.setHeaderText(title);
        dialog.setContentText(prompt);
        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(new BigDecimal(result.get().trim()));
        } catch (NumberFormatException e) {
            Dialogs.error("Enter a valid amount.");
            return Optional.empty();
        }
    }

    private void refresh() {
        Patron patron = patronCombo.getValue();
        if (patron == null) {
            return;
        }
        PatronAccount account = AppContext.get().patronAccountService().accountFor(patron.getId());
        blocksLabel.setText(account.blocked()
                ? "BLOCKED: " + String.join("; ", account.blocks())
                : "In good standing.");
        balanceLabel.setText("Outstanding balance: " + account.balance().toPlainString());
        loanTable.setItems(FXCollections.observableArrayList(account.loans()));
        holdTable.setItems(FXCollections.observableArrayList(account.holds()));
        fineTable.setItems(FXCollections.observableArrayList(account.fines()));
    }

    private static SimpleStringProperty str(Object value) {
        return new SimpleStringProperty(String.valueOf(value));
    }

    private static String actor() {
        return AppContext.get().getCurrentUser().getUsername();
    }
}
