package com.justin.libradesk.controller;

import com.justin.libradesk.config.AppContext;
import com.justin.libradesk.domain.model.User;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.util.Optional;

/**
 * Handles the login screen. Coordinates the UI only: it delegates credential
 * checking to {@code AuthService} and navigation to {@link ViewNavigator}.
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

        Optional<User> user = AppContext.get().authService().authenticate(username, password);
        if (user.isEmpty()) {
            errorLabel.setText("Invalid username or password.");
            passwordField.clear();
            return;
        }
        AppContext.get().setCurrentUser(user.get());
        ViewNavigator.get().showMain();
    }
}
