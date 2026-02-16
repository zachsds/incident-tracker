package com.sdsweather.repository;

import com.sdsweather.model.Incident;
import com.sdsweather.security.SessionManager;
import com.sdsweather.security.SSLConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class IncidentRepository {

    private static final String BASE = "https://192.168.0.237:3000";
    private static final HttpClient CLIENT = SSLConfig.createHttpClient();

    public static void create(String unitId, String summary, String severity) throws Exception {
        createAndReturnId(unitId, summary, severity);
    }

    public static String createAndReturnId(String unitId, String summary, String severity) throws Exception {

        String incidentId = UUID.randomUUID().toString();
        String reportedAt = Instant.now().toString();

        String json = """
                {
                  "incidentId": "%s",
                  "unitId": "%s",
                  "summary": "%s",
                  "severity": "%s",
                  "reportedAt": "%s"
                }
                """.formatted(incidentId, unitId, summary, severity, reportedAt);

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

    public static List<Incident> getByUnit(String unitId) throws Exception {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/incidents/" + unitId))
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

        if (body == null || body.isBlank() || body.equals("[]")) return list;

        String[] rows = body.split("\\},\\{");

        for (String row : rows) {

            row = row.replace("[", "")
                     .replace("]", "")
                     .replace("{", "")
                     .replace("}", "");

            Incident incident = new Incident();

            for (String field : row.split(",")) {

                String[] kv = field.split(":", 2);
                if (kv.length != 2) continue;

                String key = kv[0].replace("\"", "").trim();
                String val = kv[1].replace("\"", "").trim();

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

    public static void delete(String incidentId) throws Exception {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/incidents/" + incidentId))
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