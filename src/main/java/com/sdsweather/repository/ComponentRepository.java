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
 * for use in incident forms, replacement tracking, and analytics. Only active 
 * components are returned for selection in new incidents, but all components
 * (active and inactive) are available for replacement history tracking.
 *
 * API Base: https://its.zsneed.com/components
 *
 * @author Zachary Sneed
 * @version 1.0
 * @since 2026-02-16
 */
public class ComponentRepository {

    private static final String BASE = com.sdsweather.config.ServerConfig.getBaseUrl();
    private static final HttpClient CLIENT = SSLConfig.createHttpClient();

    /**
     * Creates a new component within a specific category.
     * 
     * @param componentName The display name of the component
     * @param categoryId The UUID of the parent category
     * @throws Exception If the API call fails
     */
    public static void create(String componentName, String categoryId) throws Exception {

        // Generate new UUID for this component
        String componentId = UUID.randomUUID().toString();

        // Build JSON request body
        String json = """
                {
                  "componentId": "%s",
                  "name": "%s",
                  "categoryId": "%s",
                  "isActive": true
                }
                """.formatted(componentId, componentName, categoryId);

        // Send POST request to create component
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

    /**
     * Retrieves all active component names within a specific category.
     * Used for incident forms where only active components should be selectable.
     * 
     * @param categoryId The UUID of the category to filter by
     * @return List of active component names in this category
     * @throws Exception If the API call fails
     */
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

        // Return empty list if no components found
        if (body == null || body.isBlank() || body.equals("[]")) return names;

        // Parse JSON array manually
        String[] rows = body.split("\\},\\{");

        for (String row : rows) {

            // Clean up JSON delimiters
            row = row.replace("[", "")
                     .replace("]", "")
                     .replace("{", "")
                     .replace("}", "");

            // Extract component name from JSON fields
            for (String field : row.split(",")) {

                String[] kv = field.split(":", 2);
                if (kv.length != 2) continue;

                String key = kv[0].replace("\"", "").trim();
                String val = kv[1].replace("\"", "").trim();

                // Add name to list when found
                if (key.equals("name")) {
                    names.add(val);
                    break;
                }
            }
        }

        return names;
    }

    /**
     * Retrieves all component names (both active and inactive).
     * Used for replacement history and other contexts where all components
     * should be available regardless of active status.
     * 
     * @return List of all component names
     * @throws Exception If the API call fails
     */
    public static List<String> getAllNames() throws Exception {

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
        List<String> names = new ArrayList<>();

        // Return empty list if no components found
        if (body == null || body.isBlank() || body.equals("[]")) return names;

        // Parse JSON array manually
        String[] rows = body.split("\\},\\{");

        for (String row : rows) {

            // Clean up JSON delimiters
            row = row.replace("[", "")
                     .replace("]", "")
                     .replace("{", "")
                     .replace("}", "");

            // Extract component name from JSON fields
            for (String field : row.split(",")) {

                String[] kv = field.split(":", 2);
                if (kv.length != 2) continue;

                String key = kv[0].replace("\"", "").trim();
                String val = kv[1].replace("\"", "").trim();

                // Add name to list when found
                if (key.equals("name")) {
                    names.add(val);
                    break;
                }
            }
        }

        return names;
    }

    /**
     * Looks up a component UUID by its display name.
     * Used when converting user-selected component names to IDs for database storage.
     * 
     * @param componentName The display name of the component to look up
     * @return The UUID of the component, or null if not found
     * @throws Exception If the API call fails
     */
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

        // Return null if no components exist
        if (body == null || body.isBlank() || body.equals("[]")) return null;

        // Parse JSON array manually
        String[] rows = body.split("\\},\\{");

        for (String row : rows) {

            // Clean up JSON delimiters
            row = row.replace("[", "")
                     .replace("]", "")
                     .replace("{", "")
                     .replace("}", "");

            String id = null;
            String name = null;

            // Extract id and name fields from JSON
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

            // Return ID if name matches
            if (componentName.equals(name)) {
                return id;
            }
        }

        return null;
    }

    /**
     * Looks up a component display name by its UUID.
     * Used when displaying component names in tables and reports.
     * 
     * @param componentId The UUID of the component to look up
     * @return The display name of the component, or null if not found
     * @throws Exception If the API call fails
     */
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

        // Return null if no components exist
        if (body == null || body.isBlank() || body.equals("[]")) return null;

        // Parse JSON array manually
        String[] rows = body.split("\\},\\{");

        for (String row : rows) {

            // Clean up JSON delimiters
            row = row.replace("[", "")
                     .replace("]", "")
                     .replace("{", "")
                     .replace("}", "");

            String id = null;
            String name = null;

            // Extract id and name fields from JSON
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

            // Return name if ID matches
            if (componentId.equals(id)) {
                return name;
            }
        }

        return null;
    }

    /**
     * Deletes a component by its UUID.
     * 
     * @param componentId The UUID of the component to delete
     * @throws Exception If the API call fails
     */
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