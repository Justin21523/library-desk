package com.justin.libradesk.infrastructure.database;

import com.justin.libradesk.config.AppConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Owns the PostgreSQL connection pool for the whole application. A single
 * instance is created at startup and shared with every repository; callers
 * obtain short-lived {@link Connection}s via {@link #getConnection()} and must
 * close them (try-with-resources), which returns them to the pool.
 */
public class DatabaseManager implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(DatabaseManager.class);

    private final HikariDataSource dataSource;

    public DatabaseManager(AppConfig config) {
        HikariConfig hikari = new HikariConfig();
        hikari.setPoolName("libradesk-pool");
        hikari.setJdbcUrl(config.getString("db.url"));
        hikari.setUsername(config.getString("db.user"));
        hikari.setPassword(config.getString("db.password"));
        hikari.setMaximumPoolSize(config.getInt("db.pool.maxSize", 10));
        hikari.setMinimumIdle(config.getInt("db.pool.minIdle", 2));
        hikari.setAutoCommit(true);

        this.dataSource = new HikariDataSource(hikari);
        log.info("Initialised PostgreSQL connection pool for {}", config.getString("db.url"));
    }

    /** @return a pooled connection; close it to return it to the pool. */
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public DataSource dataSource() {
        return dataSource;
    }

    @Override
    public void close() {
        log.info("Closing PostgreSQL connection pool");
        dataSource.close();
    }
}
