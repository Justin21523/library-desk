package com.justin.libradesk.controller;

import com.justin.libradesk.config.AppContext;
import com.justin.libradesk.domain.enumtype.Permission;
import com.justin.libradesk.domain.model.CalendarDay;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

/**
 * Manages the library calendar (closed days). Delegates to
 * {@code CalendarService}; guarded by the SETTINGS permission.
 */
public class CalendarController {

    @FXML
    private TableView<CalendarDay> dayTable;
    @FXML
    private TableColumn<CalendarDay, String> dateColumn;
    @FXML
    private TableColumn<CalendarDay, String> noteColumn;
    @FXML
    private DatePicker datePicker;
    @FXML
    private TextField noteField;

    @FXML
    private void initialize() {
        dateColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().date().toString()));
        noteColumn.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().note() == null ? "" : c.getValue().note()));
        refresh();
    }

    @FXML
    private void onAdd() {
        if (datePicker.getValue() == null) {
            Dialogs.error("Pick a date first.");
            return;
        }
        try {
            AccessControl.require(Permission.SETTINGS);
            AppContext.get().calendarService().add(datePicker.getValue(), text(noteField), actor());
            datePicker.setValue(null);
            noteField.clear();
            refresh();
        } catch (RuntimeException e) {
            Dialogs.error(e.getMessage());
        }
    }

    @FXML
    private void onRemove() {
        CalendarDay selected = dayTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Dialogs.error("Select a day first.");
            return;
        }
        try {
            AccessControl.require(Permission.SETTINGS);
            AppContext.get().calendarService().remove(selected.date(), actor());
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
        dayTable.setItems(FXCollections.observableArrayList(AppContext.get().calendarService().list()));
    }

    private static String text(TextField field) {
        String value = field.getText();
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String actor() {
        return AppContext.get().getCurrentUser().getUsername();
    }
}
