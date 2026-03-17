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
 * ComponentCategoryRepository - CRUD operations for component categories via REST API.
 *
 * Manages the component categories used to organize hardware components.
 * Categories are fetched when populating the incident form and settings page.
 *
 * API Base: https://its.zsneed.com/component-categories
 *
 * @author Zachary Sneed
 * @version 1.0
 * @since 2026-02-16
 */
public class ComponentCategoryRepository {

    private static final String BASE = com.sdsweather.config.ServerConfig.getBaseUrl();
    private static final HttpClient CLIENT = SSLConfig.createHttpClient();

    public static void create(String categoryName) throws Exception {

        String categoryId = UUID.randomUUID().toString();

        String json = """
                {
                  "categoryId": "%s",
                  "name": "%s",
                  "isActive": true
                }
                """.formatted(categoryId, categoryName);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/component-categories"))
                .header("Content-Type", "application/json")
                .header("Authorization", SessionManager.getAuthHeader())
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response =
                CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Create category failed: " + response.body());
        }
    }

    public static List<String> getAllActiveNames() throws Exception {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/component-categories"))
                .header("Authorization", SessionManager.getAuthHeader())
                .GET()
                .build();

        HttpResponse<String> response =
                CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Get categories failed: " + response.body());
        }

        String body = response.body();
        List<String> names = new ArrayList<>();

        if (body == null || body.isBlank() || body.equals("[]")) return names;

        String[] rows = body.split("\\},\\{");

        for (String row : rows) {

            row = row.replace("[", "")
                     .replace("]", "")
                     .replace("{", "")
                     .replace("}", "");

            for (String field : row.split(",")) {

                String[] kv = field.split(":", 2);
                if (kv.length != 2) continue;

                String key = kv[0].replace("\"", "").trim();
                String val = kv[1].replace("\"", "").trim();

                if (key.equals("name")) {
                    names.add(val);
                    break;
                }
            }
        }

        return names;
    }

    public static String getIdByName(String name) throws Exception {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/component-categories"))
                .header("Authorization", SessionManager.getAuthHeader())
                .GET()
                .build();

        HttpResponse<String> response =
                CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Get categories failed: " + response.body());
        }

        String body = response.body();

        if (body == null || body.isBlank() || body.equals("[]")) return null;

        String[] rows = body.split("\\},\\{");

        for (String row : rows) {

            row = row.replace("[", "")
                     .replace("]", "")
                     .replace("{", "")
                     .replace("}", "");

            String id = null;
            String categoryName = null;

            for (String field : row.split(",")) {

                String[] kv = field.split(":", 2);
                if (kv.length != 2) continue;

                String key = kv[0].replace("\"", "").trim();
                String val = kv[1].replace("\"", "").trim();

                if (key.equals("id")) {
                    id = val;
                } else if (key.equals("name")) {
                    categoryName = val;
                }
            }

            if (name.equals(categoryName)) {
                return id;
            }
        }

        return null;
    }

    public static void disable(String categoryId) throws Exception {

        String json = """
                {
                  "isActive": false
                }
                """;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/component-categories/" + categoryId))
                .header("Content-Type", "application/json")
                .header("Authorization", SessionManager.getAuthHeader())
                .method("PUT", HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response =
                CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Disable category failed: " + response.body());
        }
    }

    public static void delete(String categoryId) throws Exception {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/component-categories/" + categoryId))
                .header("Authorization", SessionManager.getAuthHeader())
                .DELETE()
                .build();

        HttpResponse<String> response =
                CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Delete category failed: " + response.body());
        }
    }
}