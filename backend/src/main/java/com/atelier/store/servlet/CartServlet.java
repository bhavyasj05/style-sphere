package com.atelier.store.servlet;

import com.atelier.store.dao.ProductDao;
import com.atelier.store.model.Models.CartItem;
import com.atelier.store.model.Models.Product;
import com.atelier.store.util.Json;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@WebServlet("/api/cart/*")
public class CartServlet extends BaseServlet {
    private final ProductDao products = new ProductDao();

    public record CartChange(String productId, int quantity) {
    }

    public record CartLine(Product product, int quantity, BigDecimal lineTotal) {
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            Json.send(response, view(request));
        } catch (Exception exception) {
            Json.error(response, 500, "Unable to load cart.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        change(request, response, true);
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        change(request, response, false);
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String id = request.getPathInfo().substring(1);
            cart(request).removeIf(item -> item.productId.equals(id));
            Json.send(response, view(request));
        } catch (Exception exception) {
            Json.error(response, 400, "Unable to remove the cart item.");
        }
    }

    private void change(HttpServletRequest request, HttpServletResponse response, boolean add) throws IOException {
        try {
            CartChange body = Json.read(request, CartChange.class);
            Product product = products.find(body.productId()).orElse(null);
            if (product == null || product.stock() == 0) {
                Json.error(response, 404, "Product is unavailable.");
                return;
            }
            int quantity = add ? Math.max(1, body.quantity()) : body.quantity();
            List<CartItem> cart = cart(request);
            CartItem existing = cart.stream().filter(item -> item.productId.equals(body.productId())).findFirst().orElse(null);
            if (quantity <= 0) {
                cart.removeIf(item -> item.productId.equals(body.productId()));
            } else if (existing == null) {
                cart.add(new CartItem(body.productId(), Math.min(quantity, product.stock())));
            } else {
                existing.quantity = Math.min(add ? existing.quantity + quantity : quantity, product.stock());
            }
            Json.send(response, view(request));
        } catch (Exception exception) {
            Json.error(response, 400, "Unable to update cart.");
        }
    }

    @SuppressWarnings("unchecked")
    public static List<CartItem> cart(HttpServletRequest request) {
        Object stored = request.getSession(true).getAttribute("cart");
        if (stored instanceof List<?>) {
            return (List<CartItem>) stored;
        }
        List<CartItem> cart = new ArrayList<>();
        request.getSession().setAttribute("cart", cart);
        return cart;
    }

    private Map<String, Object> view(HttpServletRequest request) throws Exception {
        List<CartLine> lines = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem item : cart(request)) {
            Product product = products.find(item.productId).orElse(null);
            if (product != null) {
                BigDecimal lineTotal = product.price().multiply(BigDecimal.valueOf(item.quantity));
                lines.add(new CartLine(product, item.quantity, lineTotal));
                subtotal = subtotal.add(lineTotal);
            }
        }
        return Map.of("items", lines, "subtotal", subtotal, "count", lines.stream().mapToInt(CartLine::quantity).sum());
    }
}
