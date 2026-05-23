package com.atelier.store.dao;

import com.atelier.store.config.Database;
import com.atelier.store.model.Models.User;

import java.sql.*;
import java.util.Optional;

public class UserDao {
    public record Account(User user, String passwordHash) {
    }

    public Optional<Account> findByEmail(String email) throws SQLException {
        try (Connection connection = Database.connection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM users WHERE email = ? AND is_active = 1")) {
            statement.setString(1, email.toLowerCase());
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? Optional.of(account(rows)) : Optional.empty();
            }
        }
    }

    public Optional<User> find(long id) throws SQLException {
        try (Connection connection = Database.connection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM users WHERE id = ?")) {
            statement.setLong(1, id);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? Optional.of(user(rows)) : Optional.empty();
            }
        }
    }

    public User create(String name, String email, String passwordHash, String role) throws SQLException {
        String sql = "INSERT INTO users (name, email, password_hash, role) VALUES (?, ?, ?, ?)";
        try (Connection connection = Database.connection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, name);
            statement.setString(2, email.toLowerCase());
            statement.setString(3, passwordHash);
            statement.setString(4, role);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return find(keys.getLong(1)).orElseThrow();
            }
        }
    }

    public Optional<User> update(long id, String name, String phone, String addressLine,
                                 String city, String state, String postalCode) throws SQLException {
        String sql = """
                UPDATE users
                SET name=?, phone=?, address_line=?, city=?, state=?, postal_code=?
                WHERE id=? AND is_active = 1
                """;
        try (Connection connection = Database.connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            statement.setString(2, phone);
            statement.setString(3, addressLine);
            statement.setString(4, city);
            statement.setString(5, state);
            statement.setString(6, postalCode);
            statement.setLong(7, id);
            return statement.executeUpdate() == 0 ? Optional.empty() : find(id);
        }
    }

    private Account account(ResultSet rows) throws SQLException {
        return new Account(user(rows), rows.getString("password_hash"));
    }

    private User user(ResultSet rows) throws SQLException {
        return new User(rows.getLong("id"), rows.getString("name"), rows.getString("email"),
                rows.getString("phone"), nullable(rows, "address_line"), nullable(rows, "city"),
                nullable(rows, "state"), nullable(rows, "postal_code"), rows.getString("role"));
    }

    private String nullable(ResultSet rows, String column) throws SQLException {
        try {
            return rows.getString(column);
        } catch (SQLException exception) {
            if ("S0022".equals(exception.getSQLState())) {
                return null;
            }
            throw exception;
        }
    }
}
