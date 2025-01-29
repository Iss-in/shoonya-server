package com.shoonya.trade_server.config;

import com.shoonya.trade_server.entity.SessionVars;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import com.zaxxer.hikari.HikariDataSource;

@Configuration
public class DataSourceConfig {

    @Value("${spring.datasource.primary.url}")
    private String primaryUrl;

    @Value("${spring.datasource.primary.username}")
    private String primaryUsername;

    @Value("${spring.datasource.primary.password}")
    private String primaryPassword;

    @Value("${spring.datasource.primary.driver-class-name}")
    private String primaryDriver;

    @Value("${spring.datasource.secondary.url}")
    private String secondaryUrl;

    @Value("${spring.datasource.secondary.username}")
    private String secondaryUsername;

    @Value("${spring.datasource.secondary.password}")
    private String secondaryPassword;

    @Value("${spring.datasource.secondary.driver-class-name}")
    private String secondaryDriver;

    private static final Logger logger = LogManager.getLogger(DataSourceConfig.class.getName());


    @Bean
    public DataSource dataSource() {
        // Try connecting to the primary DataSource
        try {
            DataSource primaryDataSource = createDataSource(primaryUrl, primaryUsername, primaryPassword, primaryDriver);
            testConnection(primaryDataSource); // Te    st if the connection is valid
            return primaryDataSource; // Use primary if it succeeds
        } catch (SQLException e) {
            logger.error("Failed to connect to primary datasource: " + e.getMessage());
            // Fallback to the secondary DataSource
            DataSource secondaryDataSource = createDataSource(secondaryUrl, secondaryUsername, secondaryPassword, secondaryDriver);
            try {
                testConnection(secondaryDataSource); // Test if the connection is valid
                return secondaryDataSource;
            } catch (SQLException ex) {
                throw new RuntimeException("Failed to connect to both primary and secondary datasources.", ex);
            }
        }
    }

    private DataSource createDataSource(String url, String username, String password, String driverClassName) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setDriverClassName(driverClassName);
        return dataSource;
    }

    private void testConnection(DataSource dataSource) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(5)) { // Timeout in seconds
                logger.debug("Connected successfully to " + dataSource);
            } else {
                throw new SQLException("Connection is not valid.");
            }
        }
    }
}
