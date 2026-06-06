package com.justin.libradesk.controller;

import javafx.scene.control.Alert;

/**
 * Small helper for the modal alerts controllers show on success/failure.
 */
public final class Dialogs {

    private Dialogs() {
    }

    public static void error(String message) {
        show(Alert.AlertType.ERROR, "Error", message);
    }

    public static void info(String message) {
        show(Alert.AlertType.INFORMATION, "Done", message);
    }

    private static void show(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
