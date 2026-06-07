package com.justin.libradesk.controller;

import com.justin.libradesk.config.AppContext;
import com.justin.libradesk.domain.enumtype.MaterialType;
import com.justin.libradesk.domain.enumtype.Permission;
import com.justin.libradesk.domain.enumtype.PatronType;
import com.justin.libradesk.domain.model.CircPolicy;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Edits the circulation policy matrix (patron type × material type → loan rules).
 * Delegates to {@code CircPolicyService}; guarded by the SETTINGS permission.
 */
public class CircPolicyController {

    private static final String DEFAULT_MATERIAL = "(default)";

    @FXML
    private TableView<CircPolicy> policyTable;
    @FXML
    private TableColumn<CircPolicy, String> patronColumn;
    @FXML
    private TableColumn<CircPolicy, String> materialColumn;
    @FXML
    private TableColumn<CircPolicy, String> loanDaysColumn;
    @FXML
    private TableColumn<CircPolicy, String> maxLoansColumn;
    @FXML
    private TableColumn<CircPolicy, String> renewalColumn;
    @FXML
    private TableColumn<CircPolicy, String> holdsColumn;
    @FXML
    private TableColumn<CircPolicy, String> finePerDayColumn;
    @FXML
    private TableColumn<CircPolicy, String> fineCapColumn;
    @FXML
    private TableColumn<CircPolicy, String> graceColumn;

    @FXML
    private ComboBox<PatronType> patronCombo;
    @FXML
    private ComboBox<String> materialCombo;
    @FXML
    private TextField loanDaysField;
    @FXML
    private TextField maxLoansField;
    @FXML
    private TextField renewalField;
    @FXML
    private TextField holdsField;
    @FXML
    private TextField finePerDayField;
    @FXML
    private TextField fineCapField;
    @FXML
    private TextField graceField;

    private Long selectedId;

    @FXML
    private void initialize() {
        patronColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().patronType().name()));
        materialColumn.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().materialType() == null ? DEFAULT_MATERIAL : c.getValue().materialType().name()));
        loanDaysColumn.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().loanDays())));
        maxLoansColumn.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().maxLoans())));
        renewalColumn.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().renewalLimit())));
        holdsColumn.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().maxHolds())));
        finePerDayColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().finePerDay().toPlainString()));
        fineCapColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().fineCap().toPlainString()));
        graceColumn.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().graceDays())));

        patronCombo.setItems(FXCollections.observableArrayList(PatronType.values()));
        List<String> materials = new ArrayList<>();
        materials.add(DEFAULT_MATERIAL);
        for (MaterialType t : MaterialType.values()) {
            materials.add(t.name());
        }
        materialCombo.setItems(FXCollections.observableArrayList(materials));

        policyTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, selected) -> populate(selected));
        refresh();
    }

    @FXML
    private void onSave() {
        try {
            AccessControl.require(Permission.SETTINGS);
            if (patronCombo.getValue() == null || materialCombo.getValue() == null) {
                Dialogs.error("Select a patron type and material type.");
                return;
            }
            String material = materialCombo.getValue();
            CircPolicy policy = new CircPolicy(
                    selectedId,
                    patronCombo.getValue(),
                    DEFAULT_MATERIAL.equals(material) ? null : MaterialType.valueOf(material),
                    Integer.parseInt(loanDaysField.getText().trim()),
                    Integer.parseInt(maxLoansField.getText().trim()),
                    Integer.parseInt(renewalField.getText().trim()),
                    Integer.parseInt(holdsField.getText().trim()),
                    new BigDecimal(finePerDayField.getText().trim()),
                    new BigDecimal(fineCapField.getText().trim()),
                    Integer.parseInt(graceField.getText().trim()));
            AppContext.get().circPolicyService().save(policy, actor());
            onClear();
            refresh();
        } catch (NumberFormatException e) {
            Dialogs.error("Loan days, limits and grace must be whole numbers; fines must be numbers.");
        } catch (RuntimeException e) {
            Dialogs.error(e.getMessage());
        }
    }

    @FXML
    private void onClear() {
        selectedId = null;
        policyTable.getSelectionModel().clearSelection();
        patronCombo.setValue(null);
        materialCombo.setValue(null);
        loanDaysField.clear();
        maxLoansField.clear();
        renewalField.clear();
        holdsField.clear();
        finePerDayField.clear();
        fineCapField.clear();
        graceField.clear();
    }

    @FXML
    private void onDelete() {
        CircPolicy selected = policyTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Dialogs.error("Select a policy first.");
            return;
        }
        try {
            AccessControl.require(Permission.SETTINGS);
            AppContext.get().circPolicyService().delete(selected.id(), actor());
            onClear();
            refresh();
        } catch (RuntimeException e) {
            Dialogs.error(e.getMessage());
        }
    }

    @FXML
    private void onRefresh() {
        refresh();
    }

    private void populate(CircPolicy policy) {
        if (policy == null) {
            return;
        }
        selectedId = policy.id();
        patronCombo.setValue(policy.patronType());
        materialCombo.setValue(policy.materialType() == null ? DEFAULT_MATERIAL : policy.materialType().name());
        loanDaysField.setText(String.valueOf(policy.loanDays()));
        maxLoansField.setText(String.valueOf(policy.maxLoans()));
        renewalField.setText(String.valueOf(policy.renewalLimit()));
        holdsField.setText(String.valueOf(policy.maxHolds()));
        finePerDayField.setText(policy.finePerDay().toPlainString());
        fineCapField.setText(policy.fineCap().toPlainString());
        graceField.setText(String.valueOf(policy.graceDays()));
    }

    private void refresh() {
        policyTable.setItems(FXCollections.observableArrayList(AppContext.get().circPolicyService().list()));
    }

    private static String actor() {
        return AppContext.get().getCurrentUser().getUsername();
    }
}
