package com.sdsweather.repository;

import com.sdsweather.model.Incident;
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
 * IncidentRepository - CRUD operations for incidents via REST API.
 *
 * Handles creating, retrieving, and deleting incident records. The
 * createAndReturnId() method is used when component links also need
 * to be created immediately after the incident.
 *
 * All API calls properly URL-encode parameters to handle special characters
 * like spaces in unit IDs and titles.
 *
 * API Base: https://192.168.0.237:3000/incidents
 *
 * @author Zachary Sneed
 * @version 1.0
 * @since 2026-02-16
 */
public class IncidentRepository {

    private static final String BASE = "https://192.168.0.237:3000";
    private static final HttpClient CLIENT = SSLConfig.createHttpClient();

    /**
     * Creates a new incident without returning the ID.
     * 
     * @param unitId The UUID of the unit this incident belongs to
     * @param summary Brief description of the incident
     * @param severity Severity level (LOW, MEDIUM, HIGH)
     * @throws Exception If the API call fails
     */
    public static void create(String unitId, String summary, String severity) throws Exception {
        createAndReturnId(unitId, summary, severity);
    }

    /**
     * Creates a new incident and returns its generated UUID.
     * Used when component links need to be created immediately after.
     * Properly escapes JSON strings to handle special characters.
     * 
     * @param unitId The UUID of the unit this incident belongs to
     * @param summary Brief description of the incident
     * @param severity Severity level (LOW, MEDIUM, HIGH)
     * @return The generated incident UUID
     * @throws Exception If the API call fails
     */
    public static String createAndReturnId(String unitId, String summary, String severity) throws Exception {

        // Generate new UUID for this incident
        String incidentId = UUID.randomUUID().toString();
        String reportedAt = Instant.now().toString();

        // Escape JSON strings to handle quotes, backslashes, and other special characters
        String escapedUnitId = unitId.replace("\\", "\\\\").replace("\"", "\\\"");
        String escapedSummary = summary.replace("\\", "\\\\").replace("\"", "\\\"");

        // Build JSON request body
        String json = """
                {
                  "incidentId": "%s",
                  "unitId": "%s",
                  "summary": "%s",
                  "severity": "%s",
                  "reportedAt": "%s"
                }
                """.formatted(incidentId, escapedUnitId, escapedSummary, severity, reportedAt);

        // Send POST request to create incident
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/incidents"))
                .header("Content-Type", "application/json")
                .header("Authorization", SessionManager.getAuthHeader())
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response =
                CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Create incident failed: " + response.body());
        }

        return incidentId;
    }

    /**
     * Retrieves all incidents for a specific unit.
     * URL-encodes the unitId to handle spaces and special characters.
     * 
     * @param unitId The UUID of the unit to get incidents for
     * @return List of incidents for this unit
     * @throws Exception If the API call fails
     */
    public static List<Incident> getByUnit(String unitId) throws Exception {

        // URL-encode the unitId to handle spaces and special characters
        String encodedUnitId = URLEncoder.encode(unitId, StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/incidents/" + encodedUnitId))
                .header("Authorization", SessionManager.getAuthHeader())
                .GET()
                .build();

        HttpResponse<String> response =
                CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Get incidents failed: " + response.body());
        }

        String body = response.body();
        List<Incident> list = new ArrayList<>();

        // Return empty list if no incidents found
        if (body == null || body.isBlank() || body.equals("[]")) return list;

        // Parse JSON array manually (simple approach for this use case)
        String[] rows = body.split("\\},\\{");

        for (String row : rows) {

            // Clean up JSON delimiters
            row = row.replace("[", "")
                     .replace("]", "")
                     .replace("{", "")
                     .replace("}", "");

            Incident incident = new Incident();

            // Parse each field in the JSON object
            for (String field : row.split(",")) {

                String[] kv = field.split(":", 2);
                if (kv.length != 2) continue;

                String key = kv[0].replace("\"", "").trim();
                String val = kv[1].replace("\"", "").trim();

                // Map JSON fields to Incident object properties
                switch (key) {
                    case "id" -> incident.incidentId = val;
                    case "unitId" -> incident.unitId = val;
                    case "summary" -> incident.summary = val;
                    case "severity" -> incident.severity = val;
                    case "reportedAt" -> incident.reportedAt = val;
                }
            }

            list.add(incident);
        }

        return list;
    }

    /**
     * Updates an existing incident's summary and severity.
     * Used when editing incidents to save changes to the database.
     * 
     * @param incidentId The UUID of the incident to update
     * @param summary Updated incident description
     * @param severity Updated severity level (LOW, MEDIUM, HIGH)
     * @throws Exception If the API call fails
     */
    public static void update(String incidentId, String summary, String severity) throws Exception {

        // Build JSON request body with updated fields
        String json = """
                {
                  "summary": "%s",
                  "severity": "%s"
                }
                """.formatted(summary, severity);

        // URL-encode the incidentId to handle special characters
        String encodedIncidentId = URLEncoder.encode(incidentId, StandardCharsets.UTF_8);

        // Send PUT request to update incident
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/incidents/" + encodedIncidentId))
                .header("Content-Type", "application/json")
                .header("Authorization", SessionManager.getAuthHeader())
                .PUT(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response =
                CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Update incident failed: " + response.body());
        }
    }

    /**
     * Retrieves all incidents across all units.
     * Used for global analytics and reporting.
     * 
     * @return List of all incidents in the system
     * @throws Exception If the API call fails
     */
    public static List<Incident> getAll() throws Exception {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/incidents"))
                .header("Authorization", SessionManager.getAuthHeader())
                .GET()
                .build();

        HttpResponse<String> response =
                CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Get all incidents failed: " + response.body());
        }

        String body = response.body();
        List<Incident> list = new ArrayList<>();

        // Return empty list if no incidents found
        if (body == null || body.isBlank() || body.equals("[]")) return list;

        // Parse JSON array manually
        String[] rows = body.split("\\},\\{");

        for (String row : rows) {

            // Clean up JSON delimiters
            row = row.replace("[", "")
                     .replace("]", "")
                     .replace("{", "")
                     .replace("}", "");

            Incident incident = new Incident();

            // Parse each field in the JSON object
            for (String field : row.split(",")) {

                String[] kv = field.split(":", 2);
                if (kv.length != 2) continue;

                String key = kv[0].replace("\"", "").trim();
                String val = kv[1].replace("\"", "").trim();

                // Map JSON fields to Incident object properties
                switch (key) {
                    case "id" -> incident.incidentId = val;
                    case "unitId" -> incident.unitId = val;
                    case "summary" -> incident.summary = val;
                    case "severity" -> incident.severity = val;
                    case "reportedAt" -> incident.reportedAt = val;
                }
            }

            list.add(incident);
        }

        return list;
    }

    /**
     * Deletes an incident by its UUID.
     * URL-encodes the incidentId to handle special characters.
     * 
     * @param incidentId The UUID of the incident to delete
     * @throws Exception If the API call fails
     */
    public static void delete(String incidentId) throws Exception {

        // URL-encode the incidentId to handle special characters
        String encodedIncidentId = URLEncoder.encode(incidentId, StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/incidents/" + encodedIncidentId))
                .header("Authorization", SessionManager.getAuthHeader())
                .DELETE()
                .build();

        HttpResponse<String> response =
                CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Delete incident failed: " + response.body());
        }
    }
}