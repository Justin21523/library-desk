package com.justin.libradesk.infrastructure.database;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Applies versioned schema migrations from {@code classpath:db/migration} at
 * startup, replacing the earlier idempotent-script initializer.
 *
 * <p>{@code baselineOnMigrate} lets Flyway adopt a database that already has the
 * pre-Flyway schema (created by the old startup script) without failing: it marks
 * the existing schema as the baseline and applies only newer migrations.
 */
public class FlywayMigrator {

    private static final Logger log = LoggerFactory.getLogger(FlywayMigrator.class);

    private final DatabaseManager databaseManager;

    public FlywayMigrator(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public void migrate() {
        Flyway flyway = Flyway.configure()
                .dataSource(databaseManager.dataSource())
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load();
        int applied = flyway.migrate().migrationsExecuted;
        log.info("Database migrations applied: {}", applied);
    }
}
