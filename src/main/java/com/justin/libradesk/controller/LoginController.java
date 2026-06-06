package com.justin.libradesk.controller;

import com.justin.libradesk.config.AppContext;
import com.justin.libradesk.domain.model.User;
import javafx.fxml.FXML;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import java.util.Optional;

/**
 * Handles the login screen. Coordinates the UI only: it delegates credential
 * checking to {@code AuthService} and navigation to {@link ViewNavigator}. If the
 * account is flagged {@code mustChangePassword}, the user must set a new password
 * before reaching the main layout.
 */
public class LoginController {

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label errorLabel;

    @FXML
    private void onLogin() {
        errorLabel.setText("");
        String username = usernameField.getText();
        String password = passwordField.getText();

        Optional<User> authenticated = AppContext.get().authService().authenticate(username, password);
        if (authenticated.isEmpty()) {
            errorLabel.setText("Invalid username or password.");
            passwordField.clear();
            return;
        }
        User user = authenticated.get();
        AppContext.get().setCurrentUser(user);

        if (user.isMustChangePassword() && !forcePasswordChange(user, password)) {
            AppContext.get().setCurrentUser(null); // cancelled the mandatory change
            return;
        }
        ViewNavigator.get().showMain();
    }

    /**
     * Shows a modal that requires a new password, retrying until it succeeds or
     * the user cancels.
     *
     * @return true if the password was changed, false if the user cancelled
     */
    private boolean forcePasswordChange(User user, String currentPassword) {
        while (true) {
            PasswordField newField = new PasswordField();
            PasswordField confirmField = new PasswordField();
            GridPane grid = new GridPane();
            grid.setHgap(8);
            grid.setVgap(8);
            grid.addRow(0, new Label("New password"), newField);
            grid.addRow(1, new Label("Confirm"), confirmField);

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Password change required");
            dialog.setHeaderText("You must set a new password before continuing.");
            dialog.getDialogPane().setContent(grid);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            Optional<ButtonType> result = dialog.showAndWait();
            if (result.isEmpty() || result.get() == ButtonType.CANCEL) {
                return false;
            }
            if (!newField.getText().equals(confirmField.getText())) {
                Dialogs.error("New password and confirmation do not match.");
                continue;
            }
            try {
                AppContext.get().userService()
                        .changePassword(user.getId(), currentPassword, newField.getText(), user.getUsername());
                return true;
            } catch (RuntimeException e) {
                Dialogs.error(e.getMessage());
            }
        }
    }
}
