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
    private static final String CONFIG_FILE = "/uvh_config.properties";

    private static HikariDataSource dataSource;

    static {
        log.info("Initializing connection pool...");
        Properties props = loadProperties();
        HikariConfig config = new HikariConfig();

        config.setJdbcUrl(props.getProperty("db.url", "jdbc:postgresql://Localhost:5432/postgres"));
        config.setUsername(props.getProperty("db.user","postgres"));
        config.setPassword(props.getProperty("db.password","1234"));
        config.setDriverClassName(props.getProperty("db.driver", "org.postgresql.Driver"));
        config.setMaximumPoolSize(Integer.parseInt(props.getProperty("db.pool.maxSize", "10")));
        config.setMinimumIdle(Integer.parseInt(props.getProperty("db.minIdle","2")));
        config.setIdleTimeout(Long.parseLong(props.getProperty("db.pool.idleTimeout","600000")));
        config.setConnectionTimeout(Long.parseLong(props.getProperty("db.pool.maxLifetime","1800000")));

    }


    private static Properties loadProperties(){
        Properties props = new Properties();
        try(InputStream input = ConnectionManager.class.getResourceAsStream(CONFIG_FILE)){
            if(input == null){
                log.warn("!!! {} not found on classpath. Using hardcoded defaults (NOT recommended for production).", CONFIG_FILE);
                props.setProperty("db.url", "jdbc:postgresql://localhost:5432/uvh_resolver_db");
                props.setProperty("db_user","postgres");
                props.setProperty("db_password","1234");
                props.setProperty("db.driver","org.postgresql.Driver");
                props.setProperty("db.pool.maxSize","5");

                return props;
            }
            props.load(input);
            log.info("Loaded database configuration from {}", CONFIG_FILE);
        } catch (IOException e){
            log.error("!!! Error loading database configuration file: {}. Using defaults.", CONFIG_FILE, e);
            props.setProperty("db.url", "jdbc:postgresql://localhost:5432/uvh_resolver_db");
            props.setProperty("db_user","postgres");
            props.setProperty("db_password","1234");
            props.setProperty("db.driver","org.postgresql.Driver");
            props.setProperty("db.pool.maxSize","5");
        }
        return props;
    }

    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            log.error("Datasource is null! Initialization likely failed.");
            throw new SQLException("Database connection pool is not available.");
        }
        log.trace("Requesting connection from pool...");
        Connection conn = dataSource.getConnection();
        log.trace("Connection obtained from pool.");
        return conn;
    }

    private ConnectionManager(){

    }
}
