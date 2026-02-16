package com.sdsweather.security;

public class SessionManager {

    private static String username;
    private static String role;
    private static String token;

    public static void startSession(String u, String r, String t) {
        username = u;
        role = r;
        token = t;
    }

    public static void clear() {
        username = null;
        role = null;
        token = null;
    }

    public static boolean isLoggedIn() {
        return username != null;
    }

    public static boolean isAdmin() {
        return "ADMIN".equals(role);
    }

    public static String getUsername() {
        return username;
    }

    public static String getRole() {
        return role;
    }

    public static String getToken() {
        return token;
    }

    public static String getAuthHeader() {

        if (token == null || token.isBlank()) {
            return "";
        }

        return "Bearer " + token;
    }

}
