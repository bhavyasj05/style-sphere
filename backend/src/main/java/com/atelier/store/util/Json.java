package com.atelier.store.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

public final class Json {
    public static final Gson GSON = new GsonBuilder().serializeNulls().create();

    private Json() {
    }

    public static <T> T read(HttpServletRequest request, Class<T> type) throws IOException {
        return GSON.fromJson(request.getReader(), type);
    }

    public static void send(HttpServletResponse response, Object body) throws IOException {
        send(response, HttpServletResponse.SC_OK, body);
    }

    public static void send(HttpServletResponse response, int status, Object body) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        GSON.toJson(body, response.getWriter());
    }

    public static void error(HttpServletResponse response, int status, String message) throws IOException {
        send(response, status, Map.of("message", message));
    }
}

