package com.justin.libradesk.controller;

import com.justin.libradesk.config.AppContext;
import com.justin.libradesk.domain.enumtype.Permission;
import com.justin.libradesk.domain.enumtype.UserRole;
import com.justin.libradesk.domain.model.User;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

/**
 * Staff account management (ADMIN only): list, create, and deactivate users.
 * Delegates to {@code UserService}.
 */
public class UsersController {

    @FXML
    private TextField usernameField;
    @FXML
    private TextField fullNameField;
    @FXML
    private ComboBox<UserRole> roleCombo;
    @FXML
    private PasswordField passwordField;
    @FXML
    private TableView<User> userTable;
    @FXML
    private TableColumn<User, String> usernameColumn;
    @FXML
    private TableColumn<User, String> nameColumn;
    @FXML
    private TableColumn<User, String> roleColumn;
    @FXML
    private TableColumn<User, String> activeColumn;

    @FXML
    private void initialize() {
        roleCombo.setItems(FXCollections.observableArrayList(UserRole.values()));
        usernameColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getUsername()));
        nameColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFullName()));
        roleColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getRole().name()));
        activeColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().isActive() ? "yes" : "no"));
        refresh();
    }

    @FXML
    private void onCreate() {
        if (roleCombo.getValue() == null) {
            Dialogs.error("Choose a role.");
            return;
        }
        try {
            AccessControl.require(Permission.USERS);
            AppContext.get().userService().createStaff(text(usernameField), text(fullNameField),
                    roleCombo.getValue(), passwordField.getText(), actor());
            usernameField.clear();
            fullNameField.clear();
            passwordField.clear();
            roleCombo.getSelectionModel().clearSelection();
            refresh();
        } catch (RuntimeException e) {
            Dialogs.error(e.getMessage());
        }
    }

    @FXML
    private void onDeactivate() {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Dialogs.error("Select a user to deactivate.");
            return;
        }
        if (selected.getId().equals(AppContext.get().getCurrentUser().getId())) {
            Dialogs.error("You cannot deactivate your own account.");
            return;
        }
        try {
            AccessControl.require(Permission.USERS);
            AppContext.get().userService().deactivate(selected.getId(), actor());
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
        userTable.setItems(FXCollections.observableArrayList(AppContext.get().userService().listUsers()));
    }

    private static String text(TextField field) {
        String value = field.getText();
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String actor() {
        return AppContext.get().getCurrentUser().getUsername();
    }
}
