package com.sdsweather.repository;

import com.sdsweather.security.SessionManager;
import com.sdsweather.security.SSLConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * AuthRepository - Handles user authentication against the REST API.
 *
 * Posts credentials to /auth/login, parses the role from the response,
 * and initializes a session via SessionManager on success.
 *
 * API Endpoint: POST https://192.168.0.237:3000/auth/login
 *
 * @author Zachary Sneed
 * @version 1.0
 * @since 2026-02-16
 */
public class AuthRepository {

    private static final HttpClient CLIENT = SSLConfig.createHttpClient();

    /**
     * Authenticates a user with the given credentials.
     * On success, starts a session in SessionManager.
     *
     * @param username The username to authenticate
     * @param password The plaintext password
     * @return true if authentication succeeded, false otherwise
     * @throws Exception If a network or parsing error occurs
     */
    public static boolean login(String username, String password) throws Exception {

        try {

            String json = """
                    {
                      "username": "%s",
                      "password": "%s"
                    }
                    """.formatted(username, password);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://192.168.0.237:3000/auth/login"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response =
                    CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                return false;
            }

            String body = response.body();

            if (!body.contains("\"success\":true")) {
                return false;
            }

            // Extract role (simple parse, no external libs)
            String role = "USER";
            int roleIndex = body.indexOf("\"role\"");
            if (roleIndex != -1) {
                int start = body.indexOf("\"", roleIndex + 6) + 1;
                int end = body.indexOf("\"", start);
                role = body.substring(start, end);
            }

            // DEBUG: Print what we got
            System.out.println("Login successful for: " + username);
            System.out.println("Role: " + role);
            System.out.println("Is Admin: " + "ADMIN".equals(role));

            // If you later add JWT token support, extract it here
            String token = null;

            SessionManager.startSession(username, role, token);

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}