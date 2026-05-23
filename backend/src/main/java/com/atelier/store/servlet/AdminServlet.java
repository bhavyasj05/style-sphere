package com.atelier.store.servlet;

import com.atelier.store.dao.OrderDao;
import com.atelier.store.dao.ProductDao;
import com.atelier.store.model.Models.Product;
import com.atelier.store.util.Json;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;

@WebServlet("/api/admin/*")
public class AdminServlet extends BaseServlet {
    private final ProductDao products = new ProductDao();
    private final OrderDao orders = new OrderDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!requireAdmin(request, response)) {
            return;
        }
        try {
            if ("/summary".equals(request.getPathInfo())) {
                Json.send(response, orders.summary());
            } else {
                Json.send(response, Map.of("products", products.findAll(null, null)));
            }
        } catch (Exception exception) {
            Json.error(response, 500, "Unable to load admin data.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!requireAdmin(request, response)) {
            return;
        }
        try {
            Json.send(response, 201, Map.of("product", products.create(validProduct(Json.read(request, Product.class)))));
        } catch (Exception exception) {
            Json.error(response, 400, "Product could not be created.");
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!requireAdmin(request, response)) {
            return;
        }
        try {
            String id = id(request);
            Product product = products.update(id, validProduct(Json.read(request, Product.class))).orElse(null);
            if (product == null) {
                Json.error(response, 404, "Product was not found.");
            } else {
                Json.send(response, Map.of("product", product));
            }
        } catch (Exception exception) {
            Json.error(response, 400, "Product could not be updated.");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!requireAdmin(request, response)) {
            return;
        }
        try {
            if (products.delete(id(request))) {
                Json.send(response, Map.of("message", "Product deleted."));
            } else {
                Json.error(response, 404, "Product was not found.");
            }
        } catch (Exception exception) {
            Json.error(response, 409, "Product cannot be deleted after it appears in an order.");
        }
    }

    private String id(HttpServletRequest request) {
        return request.getPathInfo().replace("/products/", "");
    }

    private Product validProduct(Product product) {
        if (product == null || blank(product.name()) || blank(product.brand()) || blank(product.category())
                || blank(product.description()) || blank(product.imageUrl()) || product.price() == null
                || product.price().compareTo(BigDecimal.ZERO) <= 0 || product.stock() < 0) {
            throw new IllegalArgumentException("Invalid product.");
        }
        return product;
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
