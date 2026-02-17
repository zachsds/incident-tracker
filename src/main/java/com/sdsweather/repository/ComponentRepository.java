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
 * ComponentRepository - CRUD operations for components via REST API.
 *
 * Manages hardware components within categories. Provides lookup by name and ID
 * for use in incident forms and analytics. Only active components are returned
 * for selection in new incidents.
 *
 * API Base: https://192.168.0.237:3000/components
 *
 * @author Zachary Sneed
 * @version 1.0
 * @since 2026-02-16
 */
public class ComponentRepository {

    private static final String BASE = "https://192.168.0.237:3000";
    private static final HttpClient CLIENT = SSLConfig.createHttpClient();

    public static void create(String componentName, String categoryId) throws Exception {

        String componentId = UUID.randomUUID().toString();

        String json = """
                {
                  "componentId": "%s",
                  "name": "%s",
                  "categoryId": "%s",
                  "isActive": true
                }
                """.formatted(componentId, componentName, categoryId);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/components"))
                .header("Content-Type", "application/json")
                .header("Authorization", SessionManager.getAuthHeader())
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response =
                CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Create component failed: " + response.body());
        }
    }

    public static List<String> getActiveNamesByCategory(String categoryId) throws Exception {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/components/category/" + categoryId))
                .header("Authorization", SessionManager.getAuthHeader())
                .GET()
                .build();

        HttpResponse<String> response =
                CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Get components failed: " + response.body());
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

    public static String getIdByName(String componentName) throws Exception {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/components"))
                .header("Authorization", SessionManager.getAuthHeader())
                .GET()
                .build();

        HttpResponse<String> response =
                CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Get components failed: " + response.body());
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
            String name = null;

            for (String field : row.split(",")) {

                String[] kv = field.split(":", 2);
                if (kv.length != 2) continue;

                String key = kv[0].replace("\"", "").trim();
                String val = kv[1].replace("\"", "").trim();

                if (key.equals("id")) {
                    id = val;
                } else if (key.equals("name")) {
                    name = val;
                }
            }

            if (componentName.equals(name)) {
                return id;
            }
        }

        return null;
    }

    public static String getNameById(String componentId) throws Exception {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/components"))
                .header("Authorization", SessionManager.getAuthHeader())
                .GET()
                .build();

        HttpResponse<String> response =
                CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Get components failed: " + response.body());
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
            String name = null;

            for (String field : row.split(",")) {

                String[] kv = field.split(":", 2);
                if (kv.length != 2) continue;

                String key = kv[0].replace("\"", "").trim();
                String val = kv[1].replace("\"", "").trim();

                if (key.equals("id")) {
                    id = val;
                } else if (key.equals("name")) {
                    name = val;
                }
            }

            if (componentId.equals(id)) {
                return name;
            }
        }

        return null;
    }

    public static void delete(String componentId) throws Exception {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/components/" + componentId))
                .header("Authorization", SessionManager.getAuthHeader())
                .DELETE()
                .build();

        HttpResponse<String> response =
                CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Delete component failed: " + response.body());
        }
    }
}