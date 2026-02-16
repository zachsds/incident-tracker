package com.sdsweather.repository;

import com.sdsweather.model.Unit;
import com.sdsweather.security.SessionManager;
import com.sdsweather.security.SSLConfig;
import com.sdsweather.security.AuditLogger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class UnitRepository {

    private static final String BASE = "https://192.168.0.237:3000";
    private static final HttpClient CLIENT = SSLConfig.createHttpClient();

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

        String[] rows = body.split("\\},\\{");

        for (String row : rows) {

            row = row.replace("[", "")
                     .replace("]", "")
                     .replace("{", "")
                     .replace("}", "");

            Unit u = new Unit();

            for (String field : row.split(",")) {

                String[] kv = field.split(":", 2);
                if (kv.length != 2) continue;

                String key = kv[0].replace("\"", "").trim();
                String val = kv[1].replace("\"", "").trim();

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

    public static void delete(String unitId) throws Exception {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/units/" + unitId))
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