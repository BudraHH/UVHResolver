package com.budra.uvh.dbConfig;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public class ConnectionManager {
    private static final Logger log = LoggerFactory.getLogger(ConnectionManager.class);
    private static final String CONFIG_FILE = "/uvh_config.properties"; // Ensure this is in src/main/resources

    private static HikariDataSource dataSource; // Keep this static

    // Static initializer block: runs once when the class is loaded
    static {
        log.info("Initializing HikariCP connection pool...");
        try {
            Properties props = loadProperties();
            HikariConfig config = new HikariConfig();

            // Configure HikariCP from properties
            config.setJdbcUrl(props.getProperty("db.url", "jdbc:postgresql://localhost:5432/postgres")); // Default for safety
            config.setUsername(props.getProperty("db.user", "postgres"));
            config.setPassword(props.getProperty("db.password", "1234"));
            config.setDriverClassName(props.getProperty("db.driver", "org.postgresql.Driver")); // Optional if driver is JDBC 4+ compliant and on classpath

            // --- Pooling settings ---
            // Use Integer.parseInt and provide defaults
            config.setMaximumPoolSize(Integer.parseInt(props.getProperty("db.pool.maxSize", "10")));
            config.setMinimumIdle(Integer.parseInt(props.getProperty("db.pool.minIdle", "2")));
            // Use Long.parseLong and provide defaults
            config.setIdleTimeout(Long.parseLong(props.getProperty("db.pool.idleTimeout", "600000"))); // 10 minutes
            config.setConnectionTimeout(Long.parseLong(props.getProperty("db.pool.connectionTimeout", "30000"))); // 30 seconds
            config.setMaxLifetime(Long.parseLong(props.getProperty("db.pool.maxLifetime", "1800000"))); // 30 minutes

            // Add other recommended settings if needed
            // config.addDataSourceProperty("cachePrepStmts", "true");
            // config.addDataSourceProperty("prepStmtCacheSize", "250");
            // config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.setAutoCommit(Boolean.parseBoolean(props.getProperty("db.pool.autoCommit", "true"))); // Default usually true


            // #############################################################
            // ##          CORRECTION: Create the DataSource!           ##
            // #############################################################
            dataSource = new HikariDataSource(config);
            // #############################################################

            log.info("HikariCP DataSource initialized successfully for JDBC URL: {}", config.getJdbcUrl());

        } catch (Exception e) {
            // Catch broader exceptions during initialization
            log.error("!!! CRITICAL: Failed to initialize HikariCP DataSource !!!", e);
            // Depending on application needs, you might want to prevent startup
            // For now, dataSource will remain null, causing errors on getConnection()
        }
    }

    // loadProperties method remains the same as you provided
    private static Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream input = ConnectionManager.class.getResourceAsStream(CONFIG_FILE)) {
            if (input == null) {
                log.warn("!!! {} not found on classpath. Using hardcoded defaults (NOT recommended for production).", CONFIG_FILE);
                // Set some basic defaults if file not found
                props.setProperty("db.url", "jdbc:postgresql://localhost:5432/uvh_resolver_db"); // Example DB name
                props.setProperty("db.user", "postgres"); // Corrected property name
                props.setProperty("db.password", "1234"); // Corrected property name
                props.setProperty("db.driver", "org.postgresql.Driver");
                props.setProperty("db.pool.maxSize", "5");
                return props;
            }
            props.load(input);
            log.info("Loaded database configuration from {}", CONFIG_FILE);
        } catch (IOException e) {
            log.error("!!! Error loading database configuration file: {}. Using defaults.", CONFIG_FILE, e);
            // Set defaults on error too
            props.setProperty("db.url", "jdbc:postgresql://localhost:5432/uvh_resolver_db");
            props.setProperty("db.user", "postgres"); // Corrected property name
            props.setProperty("db.password", "1234"); // Corrected property name
            props.setProperty("db.driver", "org.postgresql.Driver");
            props.setProperty("db.pool.maxSize", "5");
        }
        return props;
    }

    /**
     * Gets a connection from the configured HikariCP connection pool.
     *
     * @return A database connection.
     * @throws SQLException if the DataSource was not initialized or fails to provide a connection.
     */
    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            log.error("Datasource is null! Initialization likely failed during class loading.");
            throw new SQLException("Database connection pool is not available. Check application startup logs.");
        }
        log.trace("Requesting connection from pool...");
        Connection conn = dataSource.getConnection(); // This might block if pool is exhausted
        log.trace("Connection obtained from pool.");
        return conn;
    }

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private ConnectionManager() {
        // Utility class should not be instantiated
    }

    /**
     * Optional: Add a shutdown hook or a manual method to close the DataSource
     * when the application stops, especially in non-server environments.
     * For web applications, the server shutdown usually handles this implicitly if managed correctly.
     */
    public static void closeDataSource() {
        if (dataSource != null && !dataSource.isClosed()) {
            log.info("Closing HikariCP DataSource...");
            dataSource.close();
            log.info("HikariCP DataSource closed.");
        }
    }
}