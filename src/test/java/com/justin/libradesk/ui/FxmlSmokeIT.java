package com.justin.libradesk.ui;

import com.justin.libradesk.config.AppConfig;
import com.justin.libradesk.config.AppContext;
import com.justin.libradesk.domain.enumtype.UserRole;
import com.justin.libradesk.domain.model.User;
import com.justin.libradesk.repository.jdbc.AbstractRepositoryIT;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Loads every FXML view with a fully wired {@link AppContext} (over the
 * Testcontainers database) and asserts each loads without error — catching
 * {@code fx:id}/handler mismatches between FXML and controllers automatically.
 *
 * <p>Needs a display to start the JavaFX toolkit, so it is skipped when {@code
 * DISPLAY} is unset (e.g. headless CI). No windows are shown — only {@code load()}.
 */
class FxmlSmokeIT extends AbstractRepositoryIT {

    private static final List<String> VIEWS = List.of(
            "/fxml/LoginView.fxml",
            "/fxml/MainLayout.fxml",
            "/fxml/DashboardView.fxml",
            "/fxml/CatalogView.fxml",
            "/fxml/CatalogSearchView.fxml",
            "/fxml/ReferenceDataView.fxml",
            "/fxml/CopiesView.fxml",
            "/fxml/PatronsView.fxml",
            "/fxml/CirculationView.fxml",
            "/fxml/ReservationsView.fxml",
            "/fxml/FinesView.fxml",
            "/fxml/ReportsView.fxml",
            "/fxml/UsersView.fxml",
            "/fxml/AuditView.fxml",
            "/fxml/SettingsView.fxml",
            "/fxml/MarcEditorView.fxml");

    @BeforeAll
    static void startToolkitAndContext() {
        Assumptions.assumeTrue(System.getenv("DISPLAY") != null, "No DISPLAY; skipping GUI smoke test");
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException alreadyStarted) {
            // toolkit already up in this JVM
        } catch (Throwable cannotStart) {
            Assumptions.abort("JavaFX toolkit could not start: " + cannotStart.getMessage());
        }
        if (AppContext.tryGet() == null) {
            AppContext.initialize(AppConfig.load(), databaseManager);
        }
        AppContext.get().setCurrentUser(new User(1L, "admin", "x", "Administrator",
                UserRole.ADMIN, true, LocalDateTime.now()));
    }

    @Test
    void everyViewLoadsWithoutError() throws Exception {
        for (String view : VIEWS) {
            loadOnFxThread(view);
        }
    }

    private void loadOnFxThread(String resource) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                new FXMLLoader(getClass().getResource(resource)).load();
            } catch (Throwable t) {
                error.set(t);
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(20, TimeUnit.SECONDS), "Timed out loading " + resource);
        if (error.get() != null) {
            throw new AssertionError("Failed to load " + resource, error.get());
        }
    }
}
