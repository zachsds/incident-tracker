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
 * IncidentComponentRepository - Manages the link between incidents and components.
 *
 * Handles the many-to-many relationship between incidents and components via
 * the incident_components join table. Used when creating incidents and when
 * displaying which components were involved in each incident.
 *
 * API Base: https://192.168.0.237:3000/incident-components
 *
 * @author Zachary Sneed
 * @version 1.0
 * @since 2026-02-16
 */
public class IncidentComponentRepository {

    private static final String BASE = "https://192.168.0.237:3000";
    private static final HttpClient CLIENT = SSLConfig.createHttpClient();

    /**
     * Adds a component link to an incident.
     * Creates a new record in the incident_components join table.
     * 
     * @param incidentId The incident UUID to link
     * @param componentId The component UUID to link
     * @throws Exception If the API call fails
     */
    public static void addComponentToIncident(String incidentId, String componentId) throws Exception {

        // Generate unique ID for this link record
        String linkId = UUID.randomUUID().toString();

        // Build JSON request body
        String json = """
                {
                  "linkId": "%s",
                  "incidentId": "%s",
                  "componentId": "%s"
                }
                """.formatted(linkId, incidentId, componentId);

        // Send POST request to create link
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/incident-components"))
                .header("Content-Type", "application/json")
                .header("Authorization", SessionManager.getAuthHeader())
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response =
                CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Add component to incident failed: " + response.body());
        }
    }

    /**
     * Removes a component link from an incident.
     * Deletes the record from the incident_components join table.
     * Used when editing incidents to remove old component associations.
     * 
     * @param incidentId The incident UUID
     * @param componentId The component UUID to unlink
     * @throws Exception If the API call fails
     */
    public static void removeComponentFromIncident(String incidentId, String componentId) throws Exception {

        // Build DELETE request with both IDs in URL
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/incident-components/" + incidentId + "/" + componentId))
                .header("Authorization", SessionManager.getAuthHeader())
                .DELETE()
                .build();

        // Send DELETE request
        HttpResponse<String> response =
                CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Remove component from incident failed: " + response.body());
        }
    }

    /**
     * Retrieves all component IDs linked to a specific incident.
     * Queries the incident_components join table for all components.
     * 
     * @param incidentId The incident UUID to look up
     * @return List of component UUIDs linked to this incident
     * @throws Exception If the API call fails
     */
    public static List<String> getComponentIdsForIncident(String incidentId) throws Exception {

        // Send GET request to retrieve component links
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/incident-components/" + incidentId))
                .header("Authorization", SessionManager.getAuthHeader())
                .GET()
                .build();

        HttpResponse<String> response =
                CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Get incident components failed: " + response.body());
        }

        String body = response.body();
        List<String> componentIds = new ArrayList<>();

        // Return empty list if no components found
        if (body == null || body.isBlank() || body.equals("[]")) return componentIds;

        // Parse JSON array manually (simple string parsing approach)
        String[] rows = body.split("\\},\\{");

        for (String row : rows) {

            // Clean up JSON formatting characters
            row = row.replace("[", "")
                     .replace("]", "")
                     .replace("{", "")
                     .replace("}", "");

            // Parse each field in the row
            for (String field : row.split(",")) {

                String[] kv = field.split(":", 2);
                if (kv.length != 2) continue;

                String key = kv[0].replace("\"", "").trim();
                String val = kv[1].replace("\"", "").trim();

                // Extract componentId value
                if (key.equals("componentId")) {
                    componentIds.add(val);
                    break;
                }
            }
        }

        return componentIds;
    }
}