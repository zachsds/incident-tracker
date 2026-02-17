package com.sdsweather.repository;

import com.sdsweather.security.SessionManager;
import com.sdsweather.security.SSLConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * UserRepository - CRUD operations for user accounts via REST API.
 *
 * Manages user accounts including creation, password resets, role changes,
 * and deletion. All operations require admin authentication. Used exclusively
 * by the Settings page user management section.
 *
 * API Base: https://192.168.0.237:3000/users
 *
 * @author Zachary Sneed
 * @version 1.0
 * @since 2026-02-16
 */
public class UserRepository {

    private static final String BASE = "https://192.168.0.237:3000";
    private static final HttpClient CLIENT = SSLConfig.createHttpClient();

    /** Data transfer object representing a user account. */
    public static class User {
        public String id;
        public String username;
        public String email;
        public String role;
        public boolean isDisabled;
        public String createdAt;
    }

    public static List<User> getAll() throws Exception {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/users"))
                .header("Authorization", SessionManager.getAuthHeader())
                .GET()
                .build();

        HttpResponse<String> response =
                CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Get users failed: " + response.body());
        }

        String body = response.body();
        List<User> users = new ArrayList<>();

        if (body == null || body.isBlank() || body.equals("[]")) return users;

        String[] rows = body.split("\\},\\{");

        for (String row : rows) {

            row = row.replace("[", "")
                     .replace("]", "")
                     .replace("{", "")
                     .replace("}", "");

            User user = new User();

            for (String field : row.split(",")) {

                String[] kv = field.split(":", 2);
                if (kv.length != 2) continue;

                String key = kv[0].replace("\"", "").trim();
                String val = kv[1].replace("\"", "").trim();

                switch (key) {
                    case "id" -> user.id = val;
                    case "username" -> user.username = val;
                    case "email" -> user.email = val.equals("null") ? null : val;
                    case "role" -> user.role = val;
                    case "isDisabled" -> user.isDisabled = Boolean.parseBoolean(val);
                    case "createdAt" -> user.createdAt = val;
                }
            }

            users.add(user);
        }

        return users;
    }

    public static void create(String username, String password, String role) throws Exception {

        String userId = UUID.randomUUID().toString();

        String json = """
                {
                  "userId": "%s",
                  "username": "%s",
                  "password": "%s",
                  "role": "%s"
                }
                """.formatted(userId, username, password, role);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/users"))
                .header("Content-Type", "application/json")
                .header("Authorization", SessionManager.getAuthHeader())
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response =
                CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Create user failed: " + response.body());
        }
    }

    public static void updatePassword(String userId, String newPassword) throws Exception {

        String json = """
                {
                  "password": "%s"
                }
                """.formatted(newPassword);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/users/" + userId))
                .header("Content-Type", "application/json")
                .header("Authorization", SessionManager.getAuthHeader())
                .method("PUT", HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response =
                CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Update password failed: " + response.body());
        }
    }

    public static void updateRole(String userId, String newRole) throws Exception {

        String json = """
                {
                  "role": "%s"
                }
                """.formatted(newRole);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/users/" + userId))
                .header("Content-Type", "application/json")
                .header("Authorization", SessionManager.getAuthHeader())
                .method("PUT", HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response =
                CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Update role failed: " + response.body());
        }
    }

    public static void delete(String userId) throws Exception {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/users/" + userId))
                .header("Authorization", SessionManager.getAuthHeader())
                .DELETE()
                .build();

        HttpResponse<String> response =
                CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Delete user failed: " + response.body());
        }
    }
}