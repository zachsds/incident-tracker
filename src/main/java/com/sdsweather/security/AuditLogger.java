package com.sdsweather.security;

import com.sdsweather.security.SSLConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class AuditLogger {

    private static final String BASE = "https://192.168.0.237:3000";
    private static final HttpClient CLIENT = SSLConfig.createHttpClient();

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