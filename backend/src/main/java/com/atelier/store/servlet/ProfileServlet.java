package com.atelier.store.servlet;

import com.atelier.store.dao.UserDao;
import com.atelier.store.util.Json;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

@WebServlet("/api/profile")
public class ProfileServlet extends BaseServlet {
    private final UserDao users = new UserDao();

    public record ProfileInput(String name, String phone, String addressLine, String city, String state, String postalCode) {
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!requireUser(request, response)) {
            return;
        }
        try {
            Json.send(response, Map.of("user", users.find(userId(request)).orElseThrow()));
        } catch (Exception exception) {
            Json.error(response, 500, "Unable to load profile.");
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!requireUser(request, response)) {
            return;
        }
        try {
            ProfileInput body = Json.read(request, ProfileInput.class);
            if (body.name() == null || body.name().isBlank()) {
                Json.error(response, 400, "Name is required.");
                return;
            }
            Json.send(response, Map.of("user", users.update(userId(request), body.name(), body.phone(),
                    body.addressLine(), body.city(), body.state(), body.postalCode()).orElseThrow()));
        } catch (Exception exception) {
            Json.error(response, 500, "Unable to update profile.");
        }
    }
}

