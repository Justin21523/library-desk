package com.justin.libradesk.controller;

import com.justin.libradesk.config.AppContext;
import com.justin.libradesk.domain.service.SettingsService;
import com.justin.libradesk.util.Messages;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.util.StringConverter;

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
    @FXML
    private ComboBox<Language> languageCombo;

    private final Map<String, TextField> fields = new LinkedHashMap<>();

    /** A selectable UI language: a locale tag plus its display label. */
    private record Language(String tag, String label) {
    }

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
        initLanguageCombo();
    }

    private void initLanguageCombo() {
        languageCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Language language) {
                return language == null ? "" : language.label();
            }

            @Override
            public Language fromString(String string) {
                return null;
            }
        });
        Language en = new Language("en", Messages.get("language.en"));
        Language zh = new Language("zh-TW", Messages.get("language.zhTW"));
        languageCombo.setItems(FXCollections.observableArrayList(en, zh));
        String current = AppContext.get().settingsService().getString("ui.locale", "en");
        languageCombo.setValue(current.startsWith("zh") ? zh : en);
    }

    @FXML
    private void onApplyLanguage() {
        Language selected = languageCombo.getValue();
        if (selected == null) {
            return;
        }
        try {
            AccessControl.require(com.justin.libradesk.domain.enumtype.Permission.SETTINGS);
            AppContext.get().settingsService().setString("ui.locale", selected.tag(),
                    AppContext.get().getCurrentUser().getUsername());
            Messages.setLocale(selected.tag());
            Dialogs.info("Language set. Re-open screens (or restart) to apply it everywhere.");
        } catch (RuntimeException e) {
            Dialogs.error(e.getMessage());
        }
    }

    @FXML
    private void onSave() {
        String actor = AppContext.get().getCurrentUser().getUsername();
        try {
            AccessControl.require(com.justin.libradesk.domain.enumtype.Permission.SETTINGS);
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
