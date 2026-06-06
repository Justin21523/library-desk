package com.justin.libradesk.controller;

import com.justin.libradesk.config.AppContext;
import com.justin.libradesk.domain.service.SettingsService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Settings screen. Builds an editable row per {@link SettingsService#EDITABLE_KEYS}
 * entry from the current effective values, and persists changes (all values are
 * non-negative integers in this phase).
 */
public class SettingsController {

    @FXML
    private GridPane settingsGrid;
    @FXML
    private Label statusLabel;
    @FXML
    private PasswordField currentPasswordField;
    @FXML
    private PasswordField newPasswordField;
    @FXML
    private PasswordField confirmPasswordField;

    private final Map<String, TextField> fields = new LinkedHashMap<>();

    @FXML
    private void initialize() {
        Map<String, String> values = AppContext.get().settingsService().effectiveSettings();
        int row = 0;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            TextField field = new TextField(entry.getValue());
            field.setPrefWidth(120);
            fields.put(entry.getKey(), field);
            settingsGrid.add(new Label(entry.getKey()), 0, row);
            settingsGrid.add(field, 1, row);
            row++;
        }
    }

    @FXML
    private void onSave() {
        String actor = AppContext.get().getCurrentUser().getUsername();
        try {
            for (Map.Entry<String, TextField> entry : fields.entrySet()) {
                int value = Integer.parseInt(entry.getValue().getText().trim());
                AppContext.get().settingsService().setInt(entry.getKey(), value, actor);
            }
            statusLabel.setText("Settings saved.");
        } catch (NumberFormatException e) {
            Dialogs.error("All settings must be whole numbers.");
        } catch (RuntimeException e) {
            Dialogs.error(e.getMessage());
        }
    }

    @FXML
    private void onChangePassword() {
        if (!newPasswordField.getText().equals(confirmPasswordField.getText())) {
            Dialogs.error("New password and confirmation do not match.");
            return;
        }
        Long userId = AppContext.get().getCurrentUser().getId();
        String actor = AppContext.get().getCurrentUser().getUsername();
        try {
            AppContext.get().userService().changePassword(userId,
                    currentPasswordField.getText(), newPasswordField.getText(), actor);
            currentPasswordField.clear();
            newPasswordField.clear();
            confirmPasswordField.clear();
            Dialogs.info("Password changed.");
        } catch (RuntimeException e) {
            Dialogs.error(e.getMessage());
        }
    }
}
