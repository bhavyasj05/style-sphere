package com.atelier.store.dao;

import com.atelier.store.config.Database;
import com.atelier.store.model.Models.Product;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ProductDao {
    private static final Logger LOGGER = Logger.getLogger(ProductDao.class.getName());
    private static final String SELECT_PRODUCTS = """
            SELECT p.id, p.name, p.brand, COALESCE(parent.name, category.name) category,
                   p.description, p.image image_url, p.price, p.mrp original_price,
                   COALESCE(SUM(CASE WHEN sizes.is_active THEN sizes.stock_quantity ELSE 0 END), 0) stock,
                   p.rating >= 4.5 featured, p.created_at
            FROM products p
            JOIN categories category ON category.id = p.category_id
            LEFT JOIN categories parent ON parent.id = category.parent_id
            LEFT JOIN product_sizes sizes ON sizes.product_id = p.id
            """;

    public List<Product> findAll(String query, String category) throws SQLException {
        String sql = SELECT_PRODUCTS + """
                WHERE p.is_active
                  AND (? = '' OR LOWER(p.name) LIKE ? OR LOWER(p.brand) LIKE ?)
                  AND (? = '' OR COALESCE(parent.name, category.name) = ?)
                GROUP BY p.id, p.name, p.brand, parent.name, category.name, p.description, p.image,
                         p.price, p.mrp, p.rating, p.created_at
                ORDER BY featured DESC, p.created_at DESC
                """;
        try (Connection connection = Database.connection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            String normalized = query == null ? "" : query.trim().toLowerCase();
            String selectedCategory = category == null ? "" : category.trim();
            statement.setString(1, normalized);
            statement.setString(2, "%" + normalized + "%");
            statement.setString(3, "%" + normalized + "%");
            statement.setString(4, selectedCategory);
            statement.setString(5, selectedCategory);
            try (ResultSet rows = statement.executeQuery()) {
                List<Product> products = new ArrayList<>();
                while (rows.next()) {
                    products.add(map(rows));
                }
                return products;
            }
        } catch (SQLException exception) {
            LOGGER.log(Level.SEVERE, "Product query failed for findAll sql=" + sql, exception);
            throw exception;
        }
    }

    public Optional<Product> find(String id) throws SQLException {
        String sql = SELECT_PRODUCTS + """
                WHERE p.id = ? AND p.is_active
                GROUP BY p.id, p.name, p.brand, parent.name, category.name, p.description, p.image,
                         p.price, p.mrp, p.rating, p.created_at
                """;
        try (Connection connection = Database.connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? Optional.of(map(rows)) : Optional.empty();
            }
        } catch (SQLException exception) {
            LOGGER.log(Level.SEVERE, "Product query failed for find id=" + id + " sql=" + sql, exception);
            throw exception;
        }
    }

    public Product create(Product product) throws SQLException {
        String sql = """
                INSERT INTO products (id, category_id, name, brand, description, image, images, colors, tags, price, mrp, rating, is_active)
                VALUES (?, (SELECT id FROM categories WHERE name=? AND parent_id IS NULL LIMIT 1), ?, ?, ?, ?,
                        JSON_ARRAY(?), JSON_ARRAY(), JSON_ARRAY(), ?, ?, ?, ?)
                """;
        String id = "p" + UUID.randomUUID().toString().replace("-", "").substring(0, 19);
        try (Connection connection = Database.connection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                bind(statement, id, product);
                statement.executeUpdate();
                upsertDefaultSize(connection, id, product.stock());
                connection.commit();
                return find(id).orElseThrow();
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            LOGGER.log(Level.SEVERE, "Product insert failed sql=" + sql, exception);
            throw exception;
        }
    }

    public Optional<Product> update(String id, Product product) throws SQLException {
        String sql = "UPDATE products SET category_id=(SELECT id FROM categories WHERE name=? AND parent_id IS NULL LIMIT 1), "
                + "name=?, brand=?, description=?, image=?, images=JSON_ARRAY(?), price=?, mrp=?, rating=?, is_active=? WHERE id=?";
        try (Connection connection = Database.connection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                bind(statement, null, product);
                statement.setString(11, id);
                if (statement.executeUpdate() == 0) {
                    connection.rollback();
                    return Optional.empty();
                }
                upsertDefaultSize(connection, id, product.stock());
                connection.commit();
                return find(id);
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            LOGGER.log(Level.SEVERE, "Product update failed for id=" + id + " sql=" + sql, exception);
            throw exception;
        }
    }

    public boolean delete(String id) throws SQLException {
        try (Connection connection = Database.connection();
             PreparedStatement statement = connection.prepareStatement("UPDATE products SET is_active=0 WHERE id=?")) {
            statement.setString(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            LOGGER.log(Level.SEVERE, "Product delete failed for id=" + id, exception);
            throw exception;
        }
    }

    private void bind(PreparedStatement statement, String id, Product product) throws SQLException {
        int parameter = 1;
        if (id != null) {
            statement.setString(parameter++, id);
        }
        statement.setString(parameter++, product.category());
        statement.setString(parameter++, product.name());
        statement.setString(parameter++, product.brand());
        statement.setString(parameter++, product.description());
        statement.setString(parameter++, product.imageUrl());
        statement.setString(parameter++, product.imageUrl());
        statement.setBigDecimal(parameter++, product.price());
        statement.setBigDecimal(parameter++, product.originalPrice() == null ? product.price() : product.originalPrice());
        statement.setBigDecimal(parameter++, product.featured() ? new java.math.BigDecimal("4.5") : java.math.BigDecimal.ZERO);
        statement.setBoolean(parameter, true);
    }

    private void upsertDefaultSize(Connection connection, String productId, int stock) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE product_sizes SET stock_quantity=0, is_active=0 WHERE product_id=?")) {
            statement.setString(1, productId);
            statement.executeUpdate();
        }
        String sql = """
                INSERT INTO product_sizes (product_id, size_label, stock_quantity, is_active)
                VALUES (?, 'Free Size', ?, 1)
                ON DUPLICATE KEY UPDATE stock_quantity=VALUES(stock_quantity), is_active=1
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, productId);
            statement.setInt(2, stock);
            statement.executeUpdate();
        }
    }

    private Product map(ResultSet rows) throws SQLException {
        return new Product(rows.getString("id"), rows.getString("name"), rows.getString("brand"),
                rows.getString("category"), rows.getString("description"), rows.getString("image_url"),
                rows.getBigDecimal("price"), rows.getBigDecimal("original_price"), rows.getInt("stock"),
                rows.getBoolean("featured"));
    }
}
