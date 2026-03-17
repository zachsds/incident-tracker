package com.sdsweather.repository;

import com.sdsweather.model.Unit;
import com.sdsweather.security.SessionManager;
import com.sdsweather.security.SSLConfig;
import com.sdsweather.security.AuditLogger;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * UnitRepository - CRUD operations for units via REST API.
 *
 * Manages both STOCK and DEPLOYED unit records. All mutating operations
 * (create, delete) are logged to the audit trail via AuditLogger.
 *
 * API Base: https://its.zsneed.com/units
 *
 * @author Zachary Sneed
 * @version 1.0
 * @since 2026-02-16
 */
public class UnitRepository {

    private static final String BASE = com.sdsweather.config.ServerConfig.getBaseUrl();
    private static final HttpClient CLIENT = SSLConfig.createHttpClient();

    /**
     * Creates a new STOCK type unit.
     * Logs the creation to the audit trail.
     * 
     * @param uuid The unique identifier for this unit
     * @param stock The stock number
     * @throws Exception If the API call fails
     */
    public static void createStock(String uuid, String stock) throws Exception {

        String json = """
                {
                  "unitId": "%s",
                  "unitType": "STOCK",
                  "stockNumber": "%s"
                }
                """.formatted(uuid, stock);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/units"))
                .header("Content-Type", "application/json")
                .header("Authorization", SessionManager.getAuthHeader())
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response =
                CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("CreateStock failed: " + response.body());
        }

        // Audit log
        AuditLogger.log("CREATE_UNIT", "UNIT", uuid, "STOCK-" + stock);
    }

    /**
     * Creates a new DEPLOYED type unit.
     * Logs the creation to the audit trail.
     * 
     * @param uuid The unique identifier for this unit
     * @param title The deployment title/location
     * @throws Exception If the API call fails
     */
    public static void createDeployed(String uuid, String title) throws Exception {

        String json = """
                {
                  "unitId": "%s",
                  "unitType": "DEPLOYED",
                  "title": "%s"
                }
                """.formatted(uuid, title);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/units"))
                .header("Content-Type", "application/json")
                .header("Authorization", SessionManager.getAuthHeader())
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response =
                CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("CreateDeployed failed: " + response.body());
        }

        // Audit log
        AuditLogger.log("CREATE_UNIT", "UNIT", uuid, "DEPLOYED-" + title);
    }

    /**
     * Retrieves all units from the database.
     * 
     * @return List of all units (both STOCK and DEPLOYED)
     * @throws Exception If the API call fails
     */
    public static List<Unit> getAll() throws Exception {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/units"))
                .header("Authorization", SessionManager.getAuthHeader())
                .GET()
                .build();

        HttpResponse<String> response =
                CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("GetAll failed: " + response.body());
        }

        String body = response.body();
        List<Unit> list = new ArrayList<>();

        if (body == null || body.isBlank()) return list;

        // Parse JSON array manually (simple string parsing approach)
        String[] rows = body.split("\\},\\{");

        for (String row : rows) {

            // Clean up JSON formatting characters
            row = row.replace("[", "")
                     .replace("]", "")
                     .replace("{", "")
                     .replace("}", "");

            Unit u = new Unit();

            // Parse each field in the row
            for (String field : row.split(",")) {

                String[] kv = field.split(":", 2);
                if (kv.length != 2) continue;

                String key = kv[0].replace("\"", "").trim();
                String val = kv[1].replace("\"", "").trim();

                // Map JSON fields to Unit object properties
                switch (key) {
                    case "id" -> u.unitId = val;  // API returns "id" not "unitId"
                    case "unitId" -> u.unitId = val;  // Keep this for backwards compatibility
                    case "unitType" -> u.unitType = val;
                    case "stockNumber" -> u.stockNumber = val;  // API returns "stockNumber"
                    case "stockUnitNumber" -> u.stockNumber = val;  // Keep for backwards compat
                    case "title" -> u.title = val;
                    case "createdAt" -> u.createdAt = val;
                }
            }

            list.add(u);
        }

        return list;
    }

    /**
     * Deletes a unit by its ID.
     * URL-encodes the unitId to handle spaces and special characters.
     * Logs the deletion to the audit trail.
     * 
     * @param unitId The UUID of the unit to delete
     * @throws Exception If the API call fails
     */
    public static void delete(String unitId) throws Exception {

        // URL-encode the unitId to handle spaces and special characters
        String encodedUnitId = URLEncoder.encode(unitId, StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/units/" + encodedUnitId))
                .header("Authorization", SessionManager.getAuthHeader())
                .DELETE()
                .build();

        HttpResponse<String> response =
                CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Delete failed: " + response.body());
        }

        // Audit log
        AuditLogger.log("DELETE_UNIT", "UNIT", unitId, null);
    }
}