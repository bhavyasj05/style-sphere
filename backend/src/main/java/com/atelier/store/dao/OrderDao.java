package com.atelier.store.dao;

import com.atelier.store.config.Database;
import com.atelier.store.model.Models.CartItem;
import com.atelier.store.model.Models.Order;
import com.atelier.store.model.Models.OrderItem;
import com.atelier.store.model.Models.Product;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

public class OrderDao {
    private final ProductDao products = new ProductDao();

    public Order checkout(long userId, List<CartItem> cart, String name, String phone, String address) throws SQLException {
        if (cart.isEmpty()) {
            throw new IllegalArgumentException("Cart is empty.");
        }
        Map<Product, Integer> selected = new LinkedHashMap<>();
        BigDecimal total = BigDecimal.ZERO;
        for (CartItem item : cart) {
            Product product = products.find(item.productId).orElseThrow(() -> new IllegalArgumentException("A cart product no longer exists."));
            if (item.quantity < 1 || product.stock() < item.quantity) {
                throw new IllegalArgumentException(product.name() + " does not have enough stock.");
            }
            selected.put(product, item.quantity);
            total = total.add(product.price().multiply(BigDecimal.valueOf(item.quantity)));
        }

        try (Connection connection = Database.connection()) {
            connection.setAutoCommit(false);
            try {
                String email = userEmail(connection, userId);
                long orderId = insertOrder(connection, userId, total, name, phone, email, address);
                for (Map.Entry<Product, Integer> entry : selected.entrySet()) {
                    insertItem(connection, orderId, entry.getKey(), entry.getValue());
                    reduceStock(connection, entry.getKey().id(), entry.getValue());
                }
                connection.commit();
                return find(userId, orderId).orElseThrow();
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    public List<Order> findAll(long userId) throws SQLException {
        String sql = "SELECT * FROM orders WHERE user_id=? ORDER BY placed_at DESC";
        try (Connection connection = Database.connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet rows = statement.executeQuery()) {
                List<Order> orders = new ArrayList<>();
                while (rows.next()) {
                    orders.add(map(connection, rows));
                }
                return orders;
            }
        }
    }

    public Optional<Order> find(long userId, long id) throws SQLException {
        try (Connection connection = Database.connection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM orders WHERE id=? AND user_id=?")) {
            statement.setLong(1, id);
            statement.setLong(2, userId);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? Optional.of(map(connection, rows)) : Optional.empty();
            }
        }
    }

    public Map<String, Object> summary() throws SQLException {
        try (Connection connection = Database.connection()) {
            long users = count(connection, "SELECT COUNT(*) FROM users WHERE role='CUSTOMER'");
            long products = count(connection, "SELECT COUNT(*) FROM products WHERE is_active=1");
            Map<String, Object> metrics = metrics(connection);
            return Map.of("orders", metrics.get("orders"), "revenue", metrics.get("revenue"),
                    "customers", users, "products", products, "paymentStatuses", paymentStatuses(connection),
                    "recentOrders", recentOrders(connection));
        }
    }

    public Optional<Order> attachRazorpayOrder(long userId, long orderId, String razorpayOrderId) throws SQLException {
        try (Connection connection = Database.connection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE orders SET razorpay_order_id=?, payment_status='CREATED', payment_method='RAZORPAY' WHERE id=? AND user_id=?")) {
            statement.setString(1, razorpayOrderId);
            statement.setLong(2, orderId);
            statement.setLong(3, userId);
            statement.executeUpdate();
        }
        return find(userId, orderId);
    }

    public Optional<Order> markPaid(long userId, long orderId, String razorpayOrderId, String paymentId) throws SQLException {
        String sql = """
                UPDATE orders
                SET status='CONFIRMED', payment_status='PAID', payment_method='RAZORPAY',
                    payment_id=?, razorpay_order_id=COALESCE(razorpay_order_id, ?)
                WHERE id=? AND user_id=? AND razorpay_order_id=?
                """;
        try (Connection connection = Database.connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, paymentId);
            statement.setString(2, razorpayOrderId);
            statement.setLong(3, orderId);
            statement.setLong(4, userId);
            statement.setString(5, razorpayOrderId);
            if (statement.executeUpdate() == 0) {
                return Optional.empty();
            }
        }
        return find(userId, orderId);
    }

    public void markFailed(long userId, long orderId, String reason) throws SQLException {
        String sql = """
                UPDATE orders
                SET payment_status='FAILED', status='CANCELLED', payment_method='RAZORPAY'
                WHERE id=? AND user_id=? AND payment_status <> 'PAID'
                """;
        try (Connection connection = Database.connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, orderId);
            statement.setLong(2, userId);
            statement.executeUpdate();
        }
    }

    private Map<String, Object> metrics(Connection connection) throws SQLException {
        String sql = """
                SELECT COUNT(*) orders,
                       COALESCE(SUM(CASE WHEN payment_status='PAID' THEN total_amount ELSE 0 END),0) revenue
                FROM orders
                """;
        try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery(sql)) {
            rows.next();
            return Map.of("orders", rows.getLong("orders"), "revenue", rows.getBigDecimal("revenue"));
        }
    }

    private List<Map<String, Object>> paymentStatuses(Connection connection) throws SQLException {
        String sql = "SELECT payment_status, COUNT(*) total FROM orders GROUP BY payment_status ORDER BY payment_status";
        try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery(sql)) {
            List<Map<String, Object>> statuses = new ArrayList<>();
            while (rows.next()) {
                statuses.add(Map.of("status", rows.getString("payment_status"), "total", rows.getLong("total")));
            }
            return statuses;
        }
    }

    private List<Map<String, Object>> recentOrders(Connection connection) throws SQLException {
        String sql = """
                SELECT o.id, o.order_number, u.name customer, o.total_amount, o.status, o.payment_status, o.placed_at
                FROM orders o
                JOIN users u ON u.id = o.user_id
                ORDER BY o.placed_at DESC
                LIMIT 6
                """;
        try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery(sql)) {
            List<Map<String, Object>> recent = new ArrayList<>();
            while (rows.next()) {
                recent.add(Map.of("id", rows.getLong("id"), "orderNumber", rows.getString("order_number"),
                        "customer", rows.getString("customer"), "total", rows.getBigDecimal("total_amount"),
                        "status", rows.getString("status"), "paymentStatus", rows.getString("payment_status"),
                        "placedAt", rows.getTimestamp("placed_at").toLocalDateTime().toString()));
            }
            return recent;
        }
    }

    private long count(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery(sql)) {
            rows.next();
            return rows.getLong(1);
        }
    }

    private String userEmail(Connection connection, long userId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT email FROM users WHERE id=?")) {
            statement.setLong(1, userId);
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) {
                    throw new IllegalArgumentException("User account no longer exists.");
                }
                return rows.getString("email");
            }
        }
    }

    private long insertOrder(Connection connection, long userId, BigDecimal total, String name,
                             String phone, String email, String address) throws SQLException {
        String sql = """
                INSERT INTO orders (order_number, user_id, status, subtotal, shipping_amount, total_amount,
                                    shipping_name, shipping_phone, shipping_email, shipping_address,
                                    shipping_city, shipping_state, shipping_pincode)
                VALUES (?, ?, 'PENDING', ?, 0, ?, ?, ?, ?, ?, '', '', '')
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, "ORD" + System.currentTimeMillis());
            statement.setLong(2, userId);
            statement.setBigDecimal(3, total);
            statement.setBigDecimal(4, total);
            statement.setString(5, name);
            statement.setString(6, phone);
            statement.setString(7, email);
            statement.setString(8, address);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    private void insertItem(Connection connection, long orderId, Product product, int quantity) throws SQLException {
        String sql = """
                INSERT INTO order_items (order_id, product_id, product_name, brand, size_label, color,
                                         unit_price, quantity, line_total)
                VALUES (?, ?, ?, ?, ?, 'Default', ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, orderId);
            statement.setString(2, product.id());
            statement.setString(3, product.name());
            statement.setString(4, product.brand());
            statement.setString(5, sizeLabel(connection, product.id()));
            statement.setBigDecimal(6, product.price());
            statement.setInt(7, quantity);
            statement.setBigDecimal(8, product.price().multiply(BigDecimal.valueOf(quantity)));
            statement.executeUpdate();
        }
    }

    private void reduceStock(Connection connection, String productId, int quantity) throws SQLException {
        String sql = """
                UPDATE product_sizes
                SET stock_quantity=stock_quantity-?
                WHERE product_id=? AND is_active=1 AND stock_quantity>=?
                ORDER BY id
                LIMIT 1
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, quantity);
            statement.setString(2, productId);
            statement.setInt(3, quantity);
            if (statement.executeUpdate() == 0) {
                throw new IllegalArgumentException("Stock changed during checkout. Review the cart.");
            }
        }
    }

    private String sizeLabel(Connection connection, String productId) throws SQLException {
        String sql = "SELECT size_label FROM product_sizes WHERE product_id=? AND is_active=1 ORDER BY stock_quantity DESC, id LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, productId);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? rows.getString("size_label") : "Free Size";
            }
        }
    }

    private Order map(Connection connection, ResultSet rows) throws SQLException {
        long id = rows.getLong("id");
        List<OrderItem> items = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM order_items WHERE order_id=?")) {
            statement.setLong(1, id);
            try (ResultSet itemRows = statement.executeQuery()) {
                while (itemRows.next()) {
                    items.add(new OrderItem(itemRows.getString("product_id"), itemRows.getString("product_name"),
                            itemRows.getBigDecimal("unit_price"), itemRows.getInt("quantity")));
                }
            }
        }
        return new Order(id, rows.getBigDecimal("total_amount"), rows.getString("status"),
                rows.getString("shipping_name"), rows.getString("shipping_phone"),
                rows.getString("shipping_address"), rows.getTimestamp("placed_at").toLocalDateTime().toString(), items,
                rows.getString("payment_id"), rows.getString("payment_status"),
                rows.getString("razorpay_order_id"), rows.getString("payment_method"));
    }
}
