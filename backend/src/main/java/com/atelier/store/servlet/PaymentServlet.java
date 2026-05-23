package com.atelier.store.servlet;

import com.atelier.store.config.Database;
import com.atelier.store.dao.OrderDao;
import com.atelier.store.model.Models.Order;
import com.atelier.store.util.Json;
import com.razorpay.Utils;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Map;

@WebServlet("/api/payments/*")
public class PaymentServlet extends BaseServlet {
    private final OrderDao orders = new OrderDao();

    public record VerifyInput(long orderId, String razorpayPaymentId, String razorpayOrderId, String razorpaySignature) {
    }

    public record FailureInput(long orderId, String reason) {
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!requireUser(request, response)) {
            return;
        }
        String path = request.getPathInfo();
        try {
            if ("/verify".equals(path)) {
                verify(request, response);
            } else if ("/failure".equals(path)) {
                FailureInput body = Json.read(request, FailureInput.class);
                orders.markFailed(userId(request), body.orderId(), body.reason());
                Json.send(response, Map.of("message", "Payment failure recorded."));
            } else {
                Json.error(response, 404, "Payment endpoint was not found.");
            }
        } catch (IllegalArgumentException exception) {
            Json.error(response, 400, exception.getMessage());
        } catch (Exception exception) {
            Json.error(response, 500, "Payment update failed.");
        }
    }

    private void verify(HttpServletRequest request, HttpServletResponse response) throws Exception {
        VerifyInput body = Json.read(request, VerifyInput.class);
        if (blank(body.razorpayPaymentId()) || blank(body.razorpayOrderId()) || blank(body.razorpaySignature())) {
            throw new IllegalArgumentException("Payment verification details are required.");
        }
        JSONObject attributes = new JSONObject();
        attributes.put("razorpay_payment_id", body.razorpayPaymentId());
        attributes.put("razorpay_order_id", body.razorpayOrderId());
        attributes.put("razorpay_signature", body.razorpaySignature());
        String secret = Database.setting("RAZORPAY_SECRET", "");
        if (blank(secret)) {
            throw new IllegalArgumentException("Razorpay secret is not configured.");
        }
        boolean valid;
        try {
            valid = Utils.verifyPaymentSignature(attributes, secret);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Payment signature verification failed.");
        }
        if (!valid) {
            throw new IllegalArgumentException("Payment signature verification failed.");
        }
        Order order = orders.markPaid(userId(request), body.orderId(), body.razorpayOrderId(), body.razorpayPaymentId())
                .orElseThrow(() -> new IllegalArgumentException("Matching order was not found."));
        CartServlet.cart(request).clear();
        Json.send(response, Map.of("order", order, "message", "Payment verified."));
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
