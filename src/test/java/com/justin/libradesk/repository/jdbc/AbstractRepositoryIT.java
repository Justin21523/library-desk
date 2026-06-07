package com.justin.libradesk.repository.jdbc;

import com.justin.libradesk.config.AppConfig;
import com.justin.libradesk.infrastructure.database.DatabaseManager;
import com.justin.libradesk.infrastructure.database.FlywayMigrator;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.Properties;

/**
 * Base class for repository integration tests. Starts a single PostgreSQL
 * container (shared across all {@code *IT} classes in the JVM), points a real
 * {@link DatabaseManager} at it, and applies the production schema via
 * {@link SchemaInitializer}. Each test starts from empty tables.
 *
 * <p>Runs only under the {@code it} Maven profile ({@code mvn verify -Pit}),
 * which requires Docker.
 */
public abstract class AbstractRepositoryIT {

    // No nanoseconds: PostgreSQL TIMESTAMP has microsecond precision, so a fixed
    // second-aligned value round-trips exactly and keeps assertions stable.
    protected static final LocalDateTime FIXED = LocalDateTime.of(2026, 1, 1, 9, 0, 0);

    protected static final DatabaseManager databaseManager;

    static {
        // The bundled docker-java client negotiates Docker API 1.32 by default,
        // which modern daemons (minimum API 1.40) reject. Pin a supported version
        // before any client is created so the run is self-contained (no env/flags).
        if (System.getProperty("api.version") == null) {
            System.setProperty("api.version", "1.41");
        }

        PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
        postgres.start();

        Properties props = new Properties();
        props.setProperty("db.url", postgres.getJdbcUrl());
        props.setProperty("db.user", postgres.getUsername());
        props.setProperty("db.password", postgres.getPassword());
        props.setProperty("db.pool.maxSize", "4");
        props.setProperty("db.pool.minIdle", "1");

        databaseManager = new DatabaseManager(AppConfig.fromProperties(props));
        new FlywayMigrator(databaseManager).migrate();
        // Container is intentionally not stopped here; it is reclaimed on JVM exit
        // (Testcontainers' Ryuk also removes it).
    }

    @BeforeEach
    void resetTables() throws SQLException {
        try (Connection c = databaseManager.getConnection();
             Statement s = c.createStatement()) {
            s.execute("""
                    TRUNCATE book_authors, book_subjects, author_variants, subject_variants,
                             book_copies, loans, reservations, fines,
                             books, patrons, authors, publishers, categories, subjects,
                             audit_logs, settings, users
                    RESTART IDENTITY CASCADE
                    """);
        }
    }

    /** Inserts an author directly (there is no author repository yet) and returns its id. */
    protected long insertAuthor(String name) throws SQLException {
        try (Connection c = databaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO authors (name) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong("id");
            }
        }
    }
}
