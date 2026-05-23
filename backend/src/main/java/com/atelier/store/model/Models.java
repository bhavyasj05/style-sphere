package com.atelier.store.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class Models {
    private Models() {
    }

    public record Product(String id, String name, String brand, String category, String description,
                          String imageUrl, BigDecimal price, BigDecimal originalPrice, int stock,
                          boolean featured) {
    }

    public record User(long id, String name, String email, String phone, String addressLine,
                       String city, String state, String postalCode, String role) {
    }

    public record OrderItem(String productId, String productName, BigDecimal unitPrice, int quantity) {
    }

    public record Order(long id, BigDecimal total, String status, String shippingName,
                        String shippingPhone, String shippingAddress, String createdAt,
                        List<OrderItem> items, String paymentId, String paymentStatus,
                        String razorpayOrderId, String paymentMethod) {
    }

    public static final class CartItem {
        public String productId;
        public int quantity;

        public CartItem() {
        }

        public CartItem(String productId, int quantity) {
            this.productId = productId;
            this.quantity = quantity;
        }
    }

    public static List<CartItem> emptyCart() {
        return new ArrayList<>();
    }
}
