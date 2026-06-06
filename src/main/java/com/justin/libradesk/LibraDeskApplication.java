package com.justin.libradesk;

import com.justin.libradesk.config.AppConfig;
import com.justin.libradesk.config.AppContext;
import com.justin.libradesk.controller.ViewNavigator;
import com.justin.libradesk.domain.enumtype.UserRole;
import com.justin.libradesk.domain.model.User;
import com.justin.libradesk.infrastructure.DemoDataSeeder;
import com.justin.libradesk.infrastructure.database.DatabaseManager;
import com.justin.libradesk.infrastructure.database.FlywayMigrator;
import com.justin.libradesk.infrastructure.scheduling.MaintenanceScheduler;
import com.justin.libradesk.repository.UserRepository;
import com.justin.libradesk.repository.jdbc.JdbcUserRepository;
import com.justin.libradesk.util.PasswordHasher;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

/**
 * JavaFX entry point. Performs the startup sequence: load configuration, open
 * the database, apply the schema, seed a default admin account on first run,
 * wire the application context, and show the login screen.
 */
public class LibraDeskApplication extends Application {

    private static final Logger log = LoggerFactory.getLogger(LibraDeskApplication.class);

    /** Application version, shown in the About dialog. */
    public static final String VERSION = "0.1.0";

    private MaintenanceScheduler maintenanceScheduler;

    @Override
    public void start(Stage primaryStage) {
        try {
            AppConfig config = AppConfig.load();
            DatabaseManager databaseManager = new DatabaseManager(config);
            new FlywayMigrator(databaseManager).migrate();
            seedDefaultAdmin(databaseManager);

            AppContext context = AppContext.initialize(config, databaseManager);
            seedDemoDataIfRequested(context);

            long sweepMinutes = context.settingsService().getInt("overdue.sweep.minutes", 60);
            maintenanceScheduler = new MaintenanceScheduler(context.circulationService(),
                    context.reservationService(), sweepMinutes);
            maintenanceScheduler.start();

            primaryStage.setMinWidth(420);
            primaryStage.setMinHeight(360);
            ViewNavigator.init(primaryStage);
            ViewNavigator.get().showLogin();
            primaryStage.show();
        } catch (RuntimeException e) {
            log.error("Startup failed", e);
            showFatalError(e);
            Platform.exit();
        }
    }

    @Override
    public void stop() {
        if (maintenanceScheduler != null) {
            maintenanceScheduler.close();
        }
        // Release the connection pool if the context was initialised.
        try {
            AppContext.get().close();
        } catch (IllegalStateException ignored) {
            // Context never initialised (startup failed); nothing to close.
        }
    }

    /**
     * Creates an initial ADMIN account on a fresh database so the app is usable.
     * TODO(security): force a password change on first login in a later phase.
     */
    private void seedDefaultAdmin(DatabaseManager databaseManager) {
        UserRepository users = new JdbcUserRepository(databaseManager);
        if (!users.findAll().isEmpty()) {
            return;
        }
        User admin = new User(null, "admin", PasswordHasher.hash("admin"), "Default Administrator",
                UserRole.ADMIN, true, LocalDateTime.now());
        admin.setMustChangePassword(true); // force a new password on first login
        users.save(admin);
        log.warn("Seeded default admin account (username 'admin', password 'admin'). "
                + "You will be required to change it on first login.");
    }

    /** Loads sample data when {@code demo.seed=true} (dev/demo only). */
    private void seedDemoDataIfRequested(AppContext context) {
        if (Boolean.parseBoolean(context.config().getString("demo.seed", "false"))) {
            new DemoDataSeeder(context).seedIfEmpty();
        }
    }

    private void showFatalError(Throwable error) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("LibraDesk - Startup error");
        alert.setHeaderText("LibraDesk could not start");
        alert.setContentText(rootCauseMessage(error)
                + "\n\nCheck that PostgreSQL is running and the database exists,"
                + " then review application.properties.");
        alert.showAndWait();
    }

    private String rootCauseMessage(Throwable error) {
        Throwable cause = error;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getMessage();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
