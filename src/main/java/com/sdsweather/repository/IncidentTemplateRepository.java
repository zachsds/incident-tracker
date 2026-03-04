package com.sdsweather.repository;

import com.sdsweather.security.SessionManager;
import com.sdsweather.security.SSLConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * IncidentTemplateRepository - Manages incident templates via REST API.
 * 
 * Templates allow admins to pre-configure common incident scenarios with
 * default severity and component selections for faster incident creation.
 * This repository handles all CRUD operations for templates through the
 * its-server REST API.
 * 
 * Template Structure:
 *   - id: Unique identifier (UUID)
 *   - name: Display name (e.g., "Lightning Strike", "ESP32 Failure")
 *   - severity: Incident severity level (LOW, MEDIUM, HIGH)
 *   - description: Optional description text for the template
 *   - componentIds: List of component UUIDs pre-selected for this template
 * 
 * API Endpoints:
 *   GET    /incident-templates     - Retrieve all templates
 *   POST   /incident-templates     - Create new template
 *   DELETE /incident-templates/:id - Delete template by ID
 * 
 * @author Zachary Sneed
 * @version 1.0
 * @since 2026-03-02
 */
public class IncidentTemplateRepository {

    private static final String BASE = "https://192.168.0.237:3000";
    private static final HttpClient CLIENT = SSLConfig.createHttpClient();

    /**
     * Template - Data transfer object for incident templates.
     * 
     * Represents a reusable incident configuration that can be applied
     * when creating new incidents to pre-fill common scenarios.
     */
    public static class Template {
        public String id;
        public String name;
        public String severity;
        public String description;
        public List<String> componentIds;
    }

    /**
     * Retrieves all incident templates from the server.
     * 
     * Makes a GET request to /incident-templates and parses the JSON response
     * into a list of Template objects. Uses simplified string parsing to extract
     * template fields from the JSON response.
     * 
     * @return List of all available templates, empty list if none exist
     * @throws Exception If the API call fails or response parsing fails
     */
    public static List<Template> getAll() throws Exception {
        // Build GET request with authorization header
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/incident-templates"))
                .header("Authorization", SessionManager.getAuthHeader())
                .GET()
                .build();

        // Send request and get response
        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        // Check for successful response
        if (response.statusCode() != 200) {
            throw new RuntimeException("Get templates failed: " + response.body());
        }

        String body = response.body();
        
        // DEBUG - print raw response to console for troubleshooting
        System.out.println("Templates API response: " + body);
        
        List<Template> templates = new ArrayList<>();

        // Return empty list if no templates exist
        if (body == null || body.isBlank() || body.equals("[]")) return templates;

        // Remove outer array brackets from JSON response
        body = body.substring(1, body.length() - 1);
        
        // Split the response into individual template JSON objects
        // Each template is separated by "},{" in the array
        String[] templateStrings = body.split("\\},\\{");
        
        // Parse each template JSON object
        for (String templateStr : templateStrings) {
            // Clean up any remaining brackets
            templateStr = templateStr.replace("{", "").replace("}", "");
            
            Template template = new Template();
            template.componentIds = new ArrayList<>();
            
            // Extract ID field from JSON
            if (templateStr.contains("\"id\":\"")) {
                int start = templateStr.indexOf("\"id\":\"") + 6;
                int end = templateStr.indexOf("\"", start);
                template.id = templateStr.substring(start, end);
            }
            
            // Extract name field from JSON
            if (templateStr.contains("\"name\":\"")) {
                int start = templateStr.indexOf("\"name\":\"") + 8;
                int end = templateStr.indexOf("\"", start);
                template.name = templateStr.substring(start, end);
            }
            
            // Extract severity field from JSON
            if (templateStr.contains("\"severity\":\"")) {
                int start = templateStr.indexOf("\"severity\":\"") + 12;
                int end = templateStr.indexOf("\"", start);
                template.severity = templateStr.substring(start, end);
            }
            
            // Extract description field from JSON (may be null)
            if (templateStr.contains("\"description\":\"")) {
                int start = templateStr.indexOf("\"description\":\"") + 15;
                int end = templateStr.indexOf("\"", start);
                String desc = templateStr.substring(start, end);
                template.description = desc.equals("null") ? null : desc;
            }
            
            // Extract componentIds array from JSON
            if (templateStr.contains("\"componentIds\":[")) {
                int start = templateStr.indexOf("\"componentIds\":[") + 16;
                int end = templateStr.indexOf("]", start);
                String arrayContent = templateStr.substring(start, end);
                
                // Split the array by comma and add each component ID
                if (!arrayContent.isBlank()) {
                    for (String componentId : arrayContent.split(",")) {
                        // Remove quotes and whitespace from each ID
                        String cleanId = componentId.replace("\"", "").trim();
                        template.componentIds.add(cleanId);
                    }
                }
            }
            
            // Add parsed template to result list
            templates.add(template);
        }
        
        // DEBUG - print number of templates parsed
        System.out.println("Parsed " + templates.size() + " templates");
        
        return templates;
    }

    /**
     * Creates a new incident template on the server.
     * 
     * Constructs a JSON request body with the template data and sends it
     * to the server via POST request.
     * 
     * @param name Display name for the template
     * @param severity Severity level (LOW, MEDIUM, HIGH)
     * @param description Optional description text (can be null)
     * @param componentIds List of component UUIDs to pre-select
     * @throws Exception If the API call fails or template creation fails
     */
    public static void create(String name, String severity, String description, List<String> componentIds) throws Exception {
        // Build JSON array string for component IDs
        // Convert each ID to a quoted string and join with commas
        String componentIdsJson = "[" + String.join(",", componentIds.stream()
                .map(id -> "\"" + id + "\"").toList()) + "]";

        // Build complete JSON request body
        String json = String.format(
                "{\"name\":\"%s\",\"severity\":\"%s\",\"description\":\"%s\",\"componentIds\":%s}",
                name, severity, description == null ? "" : description, componentIdsJson
        );

        // Build POST request with JSON body
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/incident-templates"))
                .header("Authorization", SessionManager.getAuthHeader())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        // Send request
        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        // Check for successful creation
        if (response.statusCode() != 200) {
            throw new RuntimeException("Create template failed: " + response.body());
        }
    }

    /**
     * Deletes an incident template by its UUID.
     * 
     * Sends a DELETE request to remove the template from the database.
     * This operation cannot be undone.
     * 
     * @param templateId The UUID of the template to delete
     * @throws Exception If the API call fails or template deletion fails
     */
    public static void delete(String templateId) throws Exception {
        // Build DELETE request with template ID in URL
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/incident-templates/" + templateId))
                .header("Authorization", SessionManager.getAuthHeader())
                .DELETE()
                .build();

        // Send request
        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        // Check for successful deletion
        if (response.statusCode() != 200) {
            throw new RuntimeException("Delete template failed: " + response.body());
        }
    }
}