package com.justin.libradesk.infrastructure.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Runs the bundled {@code db/schema.sql} script against the database at
 * startup. The script is idempotent ({@code CREATE TABLE IF NOT EXISTS}), so
 * it is safe to run on every launch. This is intentionally lightweight; a real
 * deployment would adopt a migration tool (Flyway/Liquibase) in a later phase.
 */
public class SchemaInitializer {

    private static final Logger log = LoggerFactory.getLogger(SchemaInitializer.class);
    private static final String SCHEMA_RESOURCE = "/db/schema.sql";

    private final DatabaseManager databaseManager;

    public SchemaInitializer(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    /**
     * Executes the schema script.
     *
     * @throws IllegalStateException if the schema cannot be read or applied
     */
    public void initialize() {
        String script = readScript();
        try (Connection connection = databaseManager.getConnection();
             Statement statement = connection.createStatement()) {
            // The PostgreSQL driver accepts multiple ';'-separated statements
            // in a single execute() call via the simple query protocol.
            statement.execute(script);
            log.info("Database schema verified/initialised");
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to apply database schema", e);
        }
    }

    private String readScript() {
        try (InputStream in = getClass().getResourceAsStream(SCHEMA_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("Missing schema resource: " + SCHEMA_RESOURCE);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read schema resource: " + SCHEMA_RESOURCE, e);
        }
    }
}
