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

public class IncidentComponentRepository {

    private static final String BASE = "https://192.168.0.237:3000";
    private static final HttpClient CLIENT = SSLConfig.createHttpClient();

    public static void addComponentToIncident(String incidentId, String componentId) throws Exception {

        String linkId = UUID.randomUUID().toString();

        String json = """
                {
                  "linkId": "%s",
                  "incidentId": "%s",
                  "componentId": "%s"
                }
                """.formatted(linkId, incidentId, componentId);

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

    public static List<String> getComponentIdsForIncident(String incidentId) throws Exception {

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

        if (body == null || body.isBlank() || body.equals("[]")) return componentIds;

        String[] rows = body.split("\\},\\{");

        for (String row : rows) {

            row = row.replace("[", "")
                     .replace("]", "")
                     .replace("{", "")
                     .replace("}", "");

            for (String field : row.split(",")) {

                String[] kv = field.split(":", 2);
                if (kv.length != 2) continue;

                String key = kv[0].replace("\"", "").trim();
                String val = kv[1].replace("\"", "").trim();

                if (key.equals("componentId")) {
                    componentIds.add(val);
                    break;
                }
            }
        }

        return componentIds;
    }
}