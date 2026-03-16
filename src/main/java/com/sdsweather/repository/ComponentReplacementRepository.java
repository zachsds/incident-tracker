package com.sdsweather.repository;

import com.sdsweather.security.SessionManager;
import com.sdsweather.security.SSLConfig;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * ComponentReplacementRepository - CRUD operations for component replacement records.
 *
 * Manages the history of component replacements for units. Tracks when components
 * were replaced, who replaced them, cost, and any additional notes.
 *
 * API Base: https://192.168.0.237:3000/component-replacements
 *
 * @author Zachary Sneed
 * @version 1.0
 * @since 2026-03-16
 */
public class ComponentReplacementRepository {

    private static final String BASE = "https://192.168.0.237:3000";
    private static final HttpClient CLIENT = SSLConfig.createHttpClient();

    /**
     * ComponentReplacement data structure for holding replacement records.
     */
    public static class ComponentReplacement {
        public String id;
        public String unitId;
        public String componentId;
        public String incidentId;  // Optional - links replacement to triggering incident
        public String replacedAt;
        public String replacedBy;
        public Double cost;
        public String notes;
    }

    /**
     * Creates a new component replacement record.
     * 
     * @param unitId The UUID of the unit
     * @param componentId The UUID of the component that was replaced
     * @param incidentId The UUID of the incident that triggered replacement (optional, can be null)
     * @param replacedBy Username of person who performed the replacement
     * @param cost Cost of the replacement (optional, can be null)
     * @param notes Additional notes about the replacement (optional)
     * @throws Exception If the API call fails
     */
    public static void create(String unitId, String componentId, String incidentId,
                              String replacedBy, Double cost, String notes) throws Exception {

        // Generate new UUID for this replacement record
        String id = UUID.randomUUID().toString();
        String replacedAt = Instant.now().toString();

        // Build JSON request body
        String json = String.format("""
                {
                  "id": "%s",
                  "unitId": "%s",
                  "componentId": "%s",
                  "incidentId": %s,
                  "replacedAt": "%s",
                  "replacedBy": "%s",
                  "cost": %s,
                  "notes": %s
                }
                """,
                id,
                unitId,
                componentId,
                incidentId != null ? "\"" + incidentId + "\"" : "null",
                replacedAt,
                replacedBy,
                cost != null ? cost.toString() : "null",
                notes != null ? "\"" + notes.replace("\"", "\\\"") + "\"" : "null"
        );

        // Send POST request to create replacement record
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/component-replacements"))
                .header("Content-Type", "application/json")
                .header("Authorization", SessionManager.getAuthHeader())
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response =
                CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Create replacement failed: " + response.body());
        }
    }

    /**
     * Retrieves all component replacement records for a specific unit.
     * 
     * @param unitId The UUID of the unit
     * @return List of replacement records for this unit
     * @throws Exception If the API call fails
     */
    public static List<ComponentReplacement> getByUnit(String unitId) throws Exception {

        // URL-encode the unitId to handle spaces and special characters
        String encodedUnitId = URLEncoder.encode(unitId, StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/component-replacements/" + encodedUnitId))
                .header("Authorization", SessionManager.getAuthHeader())
                .GET()
                .build();

        HttpResponse<String> response =
                CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Get replacements failed: " + response.body());
        }

        String body = response.body();
        List<ComponentReplacement> list = new ArrayList<>();

        // Return empty list if no replacements found
        if (body == null || body.isBlank() || body.equals("[]")) return list;

        // Parse JSON array manually
        String[] rows = body.split("\\},\\{");

        for (String row : rows) {

            // Clean up JSON delimiters
            row = row.replace("[", "")
                     .replace("]", "")
                     .replace("{", "")
                     .replace("}", "");

            ComponentReplacement replacement = new ComponentReplacement();

            // Parse each field in the JSON object
            for (String field : row.split(",(?=\\\"[a-zA-Z]+\\\":)")) {  // Split on commas before quoted keys

                String[] kv = field.split(":", 2);
                if (kv.length != 2) continue;

                String key = kv[0].replace("\"", "").trim();
                String val = kv[1].replace("\"", "").trim();

                // Map JSON fields to ComponentReplacement object properties
                switch (key) {
                    case "id" -> replacement.id = val;
                    case "unitId" -> replacement.unitId = val;
                    case "componentId" -> replacement.componentId = val;
                    case "incidentId" -> {
                        if (!val.equals("null")) {
                            replacement.incidentId = val;
                        }
                    }
                    case "replacedAt" -> replacement.replacedAt = val;
                    case "replacedBy" -> replacement.replacedBy = val;
                    case "cost" -> {
                        if (!val.equals("null")) {
                            replacement.cost = Double.parseDouble(val);
                        }
                    }
                    case "notes" -> {
                        if (!val.equals("null")) {
                            replacement.notes = val;
                        }
                    }
                }
            }

            list.add(replacement);
        }

        return list;
    }

    /**
     * Updates an existing component replacement record.
     * 
     * @param id The UUID of the replacement record
     * @param componentId Updated component UUID
     * @param incidentId Updated incident UUID (can be null)
     * @param replacedBy Updated username
     * @param cost Updated cost (can be null)
     * @param notes Updated notes (can be null)
     * @throws Exception If the API call fails
     */
    public static void update(String id, String componentId, String incidentId,
                              String replacedBy, Double cost, String notes) throws Exception {

        // Build JSON request body
        String json = String.format("""
                {
                  "componentId": "%s",
                  "incidentId": %s,
                  "replacedBy": "%s",
                  "cost": %s,
                  "notes": %s
                }
                """,
                componentId,
                incidentId != null ? "\"" + incidentId + "\"" : "null",
                replacedBy,
                cost != null ? cost.toString() : "null",
                notes != null ? "\"" + notes.replace("\"", "\\\"") + "\"" : "null"
        );

        // URL-encode the id to handle special characters
        String encodedId = URLEncoder.encode(id, StandardCharsets.UTF_8);

        // Send PUT request to update replacement record
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/component-replacements/" + encodedId))
                .header("Content-Type", "application/json")
                .header("Authorization", SessionManager.getAuthHeader())
                .PUT(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response =
                CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Update replacement failed: " + response.body());
        }
    }

    /**
     * Retrieves all component replacement records across all units.
     * Used for global analytics and reporting.
     * 
     * @return List of all replacement records in the system
     * @throws Exception If the API call fails
     */
    public static List<ComponentReplacement> getAll() throws Exception {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/component-replacements"))
                .header("Authorization", SessionManager.getAuthHeader())
                .GET()
                .build();

        HttpResponse<String> response =
                CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Get all replacements failed: " + response.body());
        }

        String body = response.body();
        List<ComponentReplacement> list = new ArrayList<>();

        // Return empty list if no replacements found
        if (body == null || body.isBlank() || body.equals("[]")) return list;

        // Parse JSON array manually
        String[] rows = body.split("\\},\\{");

        for (String row : rows) {

            // Clean up JSON delimiters
            row = row.replace("[", "")
                     .replace("]", "")
                     .replace("{", "")
                     .replace("}", "");

            ComponentReplacement replacement = new ComponentReplacement();

            // Parse each field in the JSON object
            for (String field : row.split(",(?=\\\"[a-zA-Z]+\\\":)")) {  // Split on commas before quoted keys

                String[] kv = field.split(":", 2);
                if (kv.length != 2) continue;

                String key = kv[0].replace("\"", "").trim();
                String val = kv[1].replace("\"", "").trim();

                // Map JSON fields to ComponentReplacement object properties
                switch (key) {
                    case "id" -> replacement.id = val;
                    case "unitId" -> replacement.unitId = val;
                    case "componentId" -> replacement.componentId = val;
                    case "incidentId" -> {
                        if (!val.equals("null")) {
                            replacement.incidentId = val;
                        }
                    }
                    case "replacedAt" -> replacement.replacedAt = val;
                    case "replacedBy" -> replacement.replacedBy = val;
                    case "cost" -> {
                        if (!val.equals("null")) {
                            replacement.cost = Double.parseDouble(val);
                        }
                    }
                    case "notes" -> {
                        if (!val.equals("null")) {
                            replacement.notes = val;
                        }
                    }
                }
            }

            list.add(replacement);
        }

        return list;
    }

    /**
     * Deletes a component replacement record.
     * 
     * @param id The UUID of the replacement record to delete
     * @throws Exception If the API call fails
     */
    public static void delete(String id) throws Exception {

        // URL-encode the id to handle special characters
        String encodedId = URLEncoder.encode(id, StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/component-replacements/" + encodedId))
                .header("Authorization", SessionManager.getAuthHeader())
                .DELETE()
                .build();

        HttpResponse<String> response =
                CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Delete replacement failed: " + response.body());
        }
    }
}