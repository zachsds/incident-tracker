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
 * AuditLogRepository - Retrieves and manages audit log entries from the REST API.
 *
 * Supports date range and limit filtering for viewing logs, plus delete operations
 * for individual entries, multiple entries, or entire date ranges. Used exclusively
 * by AuditLogPage to display the system audit trail to administrators.
 *
 * API Endpoint: GET/DELETE https://its.zsneed.com/audit-logs
 *
 * @author Zachary Sneed
 * @version 1.0
 * @since 2026-02-16
 */
public class AuditLogRepository {

    private static final String BASE = com.sdsweather.config.ServerConfig.getBaseUrl();
    private static final HttpClient CLIENT = SSLConfig.createHttpClient();

    public static class AuditLog {
        public String id;
        public String userId;
        public String username;
        public String action;
        public String entityType;
        public String entityId;
        public String details;
        public String timestamp;
    }

    public static List<AuditLog> getAll(String startDate, String endDate, Integer limit) throws Exception {

        StringBuilder url = new StringBuilder(BASE + "/audit-logs?");
        if (startDate != null) url.append("startDate=").append(startDate).append("&");
        if (endDate != null) url.append("endDate=").append(endDate).append("&");
        if (limit != null) url.append("limit=").append(limit).append("&");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url.toString()))
                .header("Authorization", SessionManager.getAuthHeader())
                .GET()
                .build();

        HttpResponse<String> response =
                CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Get audit logs failed: " + response.body());
        }

        String body = response.body();
        List<AuditLog> logs = new ArrayList<>();

        if (body == null || body.isBlank() || body.equals("[]")) return logs;

        String[] rows = body.split("\\},\\{");

        for (String row : rows) {

            row = row.replace("[", "")
                     .replace("]", "")
                     .replace("{", "")
                     .replace("}", "");

            AuditLog log = new AuditLog();

            for (String field : row.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)")) {

                String[] kv = field.split(":", 2);
                if (kv.length != 2) continue;

                String key = kv[0].replace("\"", "").trim();
                String val = kv[1].replace("\"", "").trim();

                switch (key) {
                    case "id" -> log.id = val;
                    case "userId" -> log.userId = val;
                    case "username" -> log.username = val;
                    case "action" -> log.action = val;
                    case "entityType" -> log.entityType = val;
                    case "entityId" -> log.entityId = val;
                    case "details" -> log.details = val.equals("null") ? null : val;
                    case "timestamp" -> log.timestamp = val;
                }
            }

            logs.add(log);
        }

        return logs;
    }

    /**
     * Deletes a single audit log entry by ID.
     *
     * @param logId The audit log ID to delete
     * @throws Exception If the API request fails
     */
    public static void delete(String logId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/audit-logs/" + logId))
                .header("Authorization", SessionManager.getAuthHeader())
                .DELETE()
                .build();

        HttpResponse<String> response =
                CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Delete audit log failed: " + response.body());
        }
    }

    /**
     * Deletes all audit logs within a date range.
     *
     * @param startDate ISO date-time string for range start
     * @param endDate   ISO date-time string for range end
     * @throws Exception If the API request fails
     */
    public static void deleteByDateRange(String startDate, String endDate) throws Exception {
        StringBuilder url = new StringBuilder(BASE + "/audit-logs?");
        if (startDate != null) url.append("startDate=").append(startDate).append("&");
        if (endDate != null) url.append("endDate=").append(endDate).append("&");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url.toString()))
                .header("Authorization", SessionManager.getAuthHeader())
                .DELETE()
                .build();

        HttpResponse<String> response =
                CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Delete audit logs by date range failed: " + response.body());
        }
    }
}