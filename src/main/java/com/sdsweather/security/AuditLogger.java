package com.sdsweather.security;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * AuditLogger - Asynchronous audit trail logging utility.
 *
 * Records user actions to the audit log API on a background thread so the UI
 * is never blocked. Fails silently — audit log failures never prevent normal
 * application operations.
 *
 * Usage:
 *   AuditLogger.log("CREATE_UNIT", "UNIT", unitId, "STOCK-0001");
 *
 * @author Zachary Sneed
 * @version 1.0
 * @since 2026-02-16
 */
public class AuditLogger {

    private static final String BASE = "https://192.168.0.237:3000";
    private static final HttpClient CLIENT = SSLConfig.createHttpClient();

    /**
     * Records an audit entry asynchronously. Returns immediately.
     *
     * @param action     The action performed (e.g., "CREATE_UNIT", "DELETE_INCIDENT")
     * @param entityType The type of entity affected (e.g., "UNIT", "INCIDENT")
     * @param entityId   The unique ID of the affected entity
     * @param details    Optional human-readable context (may be null)
     */
    public static void log(String action, String entityType, String entityId, String details) {
        // Run async to not block UI
        new Thread(() -> {
            try {
                String username = SessionManager.getUsername();
                if (username == null) username = "SYSTEM";

                String json = String.format("""
                        {
                          "userId": "%s",
                          "username": "%s",
                          "action": "%s",
                          "entityType": "%s",
                          "entityId": "%s",
                          "details": "%s"
                        }
                        """,
                        "audit-user", // placeholder userId
                        username,
                        action,
                        entityType,
                        entityId,
                        details != null ? details.replace("\"", "\\\"") : ""
                );

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(BASE + "/audit-logs"))
                        .header("Content-Type", "application/json")
                        .header("Authorization", SessionManager.getAuthHeader())
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();

                CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            } catch (Exception ex) {
                // Silent fail - don't block operations if audit logging fails
                ex.printStackTrace();
            }
        }).start();
    }
}