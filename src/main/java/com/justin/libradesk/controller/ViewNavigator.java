package com.justin.libradesk.controller;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Owns the primary {@link Stage} and swaps the scene root between top-level
 * views. Controllers navigate through the shared instance rather than knowing
 * about each other.
 */
public final class ViewNavigator {

    private static ViewNavigator instance;

    private final Stage stage;

    private ViewNavigator(Stage stage) {
        this.stage = stage;
    }

    public static void init(Stage stage) {
        instance = new ViewNavigator(stage);
    }

    public static ViewNavigator get() {
        if (instance == null) {
            throw new IllegalStateException("ViewNavigator not initialised");
        }
        return instance;
    }

    public void showLogin() {
        setRoot("/fxml/LoginView.fxml", "LibraDesk - Sign in");
    }

    public void showMain() {
        setRoot("/fxml/MainLayout.fxml", "LibraDesk");
    }

    private void setRoot(String fxmlResource, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlResource));
            Parent root = loader.load();
            Scene scene = stage.getScene();
            if (scene == null) {
                scene = new Scene(root);
                scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
                stage.setScene(scene);
            } else {
                scene.setRoot(root);
            }
            stage.setTitle(title);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load view: " + fxmlResource, e);
        }
    }
}
