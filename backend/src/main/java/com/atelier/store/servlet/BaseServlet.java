package com.atelier.store.servlet;

import com.atelier.store.model.Models.User;
import com.atelier.store.util.Json;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public abstract class BaseServlet extends HttpServlet {
    protected Long userId(HttpServletRequest request) {
        Object id = request.getSession().getAttribute("userId");
        return id instanceof Long value ? value : null;
    }

    protected boolean requireUser(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (userId(request) != null) {
            return true;
        }
        Json.error(response, HttpServletResponse.SC_UNAUTHORIZED, "Sign in to continue.");
        return false;
    }

    protected boolean requireAdmin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Object role = request.getSession().getAttribute("role");
        if ("ADMIN".equals(role)) {
            return true;
        }
        Json.error(response, HttpServletResponse.SC_FORBIDDEN, "Admin access is required.");
        return false;
    }

    protected void login(HttpServletRequest request, User user) {
        request.getSession(true).setAttribute("userId", user.id());
        request.getSession().setAttribute("role", user.role());
    }
}

