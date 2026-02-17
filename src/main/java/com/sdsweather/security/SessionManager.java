package com.sdsweather.security;

/**
 * SessionManager - Manages the authenticated user's session state.
 *
 * Stores session data in static fields after a successful login, making
 * user information globally accessible without passing it between pages.
 * Session data is cleared on logout.
 *
 * @author Zachary Sneed
 * @version 1.0
 * @since 2026-02-16
 */
public class SessionManager {

    /** The authenticated user's username */
    private static String username;

    /** The user's role: "USER" or "ADMIN" */
    private static String role;

    /** JWT token for API auth (reserved for future implementation) */
    private static String token;

    /** Starts a new session after successful login. */
    public static void startSession(String u, String r, String t) {
        username = u;
        role = r;
        token = t;
    }

    /** Clears all session data, effectively logging the user out. */
    public static void clear() {
        username = null;
        role = null;
        token = null;
    }

    /** Returns true if a user session is currently active. */
    public static boolean isLoggedIn() {
        return username != null;
    }

    /** Returns true if the current user has administrator privileges. */
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
