package com.atelier.store.servlet;

import com.atelier.store.dao.UserDao;
import com.atelier.store.util.Json;
import com.atelier.store.util.Passwords;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Map;

@WebServlet("/api/auth/*")
public class AuthServlet extends BaseServlet {
    private final UserDao users = new UserDao();

    public record Credentials(String name, String email, String password) {
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!"/me".equals(request.getPathInfo()) || userId(request) == null) {
            Json.error(response, HttpServletResponse.SC_UNAUTHORIZED, "No active session.");
            return;
        }
        try {
            Json.send(response, Map.of("user", users.find(userId(request)).orElseThrow()));
        } catch (Exception exception) {
            Json.error(response, 500, "Unable to load the session user.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = request.getPathInfo();
        if ("/logout".equals(path)) {
            request.getSession().invalidate();
            Json.send(response, Map.of("message", "Signed out."));
            return;
        }
        Credentials body = Json.read(request, Credentials.class);
        try {
            if ("/register".equals(path)) {
                register(request, response, body);
            } else if ("/login".equals(path)) {
                signIn(request, response, body);
            } else {
                Json.error(response, 404, "Authentication route was not found.");
            }
        } catch (SQLIntegrityConstraintViolationException exception) {
            Json.error(response, 409, "That email is already registered.");
        } catch (Exception exception) {
            Json.error(response, 500, "Authentication failed.");
        }
    }

    private void register(HttpServletRequest request, HttpServletResponse response, Credentials body) throws Exception {
        if (blank(body.name()) || blank(body.email()) || body.password() == null || body.password().length() < 8) {
            Json.error(response, 400, "Provide name, email, and a password with at least 8 characters.");
            return;
        }
        var user = users.create(body.name().trim(), body.email().trim(), Passwords.hash(body.password()), "CUSTOMER");
        login(request, user);
        Json.send(response, 201, Map.of("user", user));
    }

    private void signIn(HttpServletRequest request, HttpServletResponse response, Credentials body) throws Exception {
        var account = users.findByEmail(body.email() == null ? "" : body.email().trim()).orElse(null);
        if (account == null || !Passwords.matches(body.password() == null ? "" : body.password(), account.passwordHash())) {
            Json.error(response, 401, "Email or password is incorrect.");
            return;
        }
        login(request, account.user());
        Json.send(response, Map.of("user", account.user()));
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}

