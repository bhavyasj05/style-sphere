package com.atelier.store.servlet;

import com.atelier.store.dao.ProductDao;
import com.atelier.store.model.Models.Product;
import com.atelier.store.util.Json;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

@WebServlet("/api/products/*")
public class ProductServlet extends BaseServlet {
    private final ProductDao products = new ProductDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            if (request.getPathInfo() == null || "/".equals(request.getPathInfo())) {
                Json.send(response, Map.of("products", products.findAll(request.getParameter("q"), request.getParameter("category"))));
                return;
            }
            String id = request.getPathInfo().substring(1);
            Product product = products.find(id).orElse(null);
            if (product == null) {
                Json.error(response, 404, "Product was not found.");
            } else {
                Json.send(response, Map.of("product", product));
            }
        } catch (Exception exception) {
            getServletContext().log("Unable to load products for " + request.getRequestURI(), exception);
            Json.error(response, 500, "Unable to load products.");
        }
    }
}
