package com.atelier.store.servlet;

import com.atelier.store.dao.OrderDao;
import com.atelier.store.config.Database;
import com.atelier.store.util.Json;
import com.razorpay.RazorpayClient;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Map;

@WebServlet("/api/orders/*")
public class OrderServlet extends BaseServlet {
    private final OrderDao orders = new OrderDao();

    public record CheckoutInput(String shippingName, String shippingPhone, String shippingAddress) {
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!requireUser(request, response)) {
            return;
        }
        try {
            Json.send(response, Map.of("orders", orders.findAll(userId(request))));
        } catch (Exception exception) {
            Json.error(response, 500, "Unable to load orders.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!requireUser(request, response)) {
            return;
        }
        try {
            CheckoutInput body = Json.read(request, CheckoutInput.class);
            if (blank(body.shippingName()) || blank(body.shippingPhone()) || blank(body.shippingAddress())) {
                Json.error(response, 400, "Shipping name, phone, and address are required.");
                return;
            }
            var order = orders.checkout(userId(request), CartServlet.cart(request),
                    body.shippingName(), body.shippingPhone(), body.shippingAddress());
            String keyId = Database.setting("RAZORPAY_KEY_ID", "");
            String secret = Database.setting("RAZORPAY_SECRET", "");
            if (blank(keyId) || blank(secret)) {
                Json.send(response, 201, Map.of("order", order, "paymentRequired", true,
                        "message", "Order created, but Razorpay credentials are not configured."));
                return;
            }
            RazorpayClient client = new RazorpayClient(keyId, secret);
            JSONObject options = new JSONObject();
            options.put("amount", order.total().multiply(new java.math.BigDecimal("100")).intValueExact());
            options.put("currency", "INR");
            options.put("receipt", "order_" + order.id());
            options.put("payment_capture", 1);
            com.razorpay.Order razorpayOrder = client.orders.create(options);
            String razorpayOrderId = razorpayOrder.get("id");
            var updated = orders.attachRazorpayOrder(userId(request), order.id(), razorpayOrderId).orElse(order);
            Json.send(response, 201, Map.of("order", updated, "razorpayOrderId", razorpayOrderId,
                    "razorpayKeyId", keyId, "amount", options.getInt("amount"), "currency", "INR"));
        } catch (IllegalArgumentException exception) {
            Json.error(response, 400, exception.getMessage());
        } catch (Exception exception) {
            Json.error(response, 500, "Checkout failed.");
        }
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
