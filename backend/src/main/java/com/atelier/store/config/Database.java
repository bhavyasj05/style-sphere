package com.atelier.store.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Database {
    private static final Logger LOGGER = Logger.getLogger(Database.class.getName());
    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/style_sphere?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String DEFAULT_USER = "fashion_app";
    private static final String DEFAULT_PASSWORD = "fashion_app_password";

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("MySQL JDBC driver was not found.", exception);
        }
    }

    private Database() {
    }

    public static Connection connection() throws SQLException {
        ResolvedSetting url = resolvedSetting("DB_URL", DEFAULT_URL);
        ResolvedSetting user = resolvedSetting("DB_USER", DEFAULT_USER);
        ResolvedSetting password = resolvedSetting("DB_PASSWORD", DEFAULT_PASSWORD);
        LOGGER.info(() -> "Opening database connection url=" + url.value()
                + " [" + url.source() + "], user=" + user.value() + " [" + user.source()
                + "], password=" + describePassword(password));
        try {
            return DriverManager.getConnection(url.value(), user.value(), password.value());
        } catch (SQLException exception) {
            LOGGER.log(Level.SEVERE, "Database connection failed for url=" + url.value()
                    + ", user=" + user.value() + ", password=" + describePassword(password), exception);
            throw exception;
        }
    }

    public static String setting(String key, String fallback) {
        return resolvedSetting(key, fallback).value();
    }

    private static ResolvedSetting resolvedSetting(String key, String fallback) {
        String value = System.getenv(key);
        if (value != null && !value.isBlank()) {
            return new ResolvedSetting(value, "environment " + key);
        }
        value = System.getProperty(key);
        if (value != null && !value.isBlank()) {
            return new ResolvedSetting(value, "system property " + key);
        }
        return new ResolvedSetting(fallback, "default");
    }

    private static String describePassword(ResolvedSetting password) {
        return password.source() + " value length " + password.value().length();
    }

    private record ResolvedSetting(String value, String source) {
    }
}
