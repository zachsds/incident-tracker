package com.sdsweather.config;

import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * ServerConfig - Automatically detects and provides the optimal server URL.
 *
 * Attempts to connect to the local server first (192.168.0.237:3000).
 * If reachable, uses the local URL for faster performance.
 * If unreachable, falls back to the remote URL (its.zsneed.com).
 *
 * The detection happens once at startup and is cached for the session.
 *
 * @author Zachary Sneed
 * @version 1.0
 * @since 2026-03-17
 */
public class ServerConfig {
    
    private static final String LOCAL_URL = "https://192.168.0.237:3000";
    private static final String REMOTE_URL = "https://its.zsneed.com";
    private static String baseUrl = null;
    
    public static String getBaseUrl() {
        if (baseUrl == null) {
            baseUrl = detectServerUrl();
        }
        return baseUrl;
    }
    
    private static String detectServerUrl() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("192.168.0.237", 3000), 2000);
            System.out.println("Local server detected - using " + LOCAL_URL);
            return LOCAL_URL;
        } catch (Exception e) {
            System.out.println("Local server not reachable - using " + REMOTE_URL);
            return REMOTE_URL;
        }
    }
    
    public static void refresh() {
        baseUrl = null;
    }
    
    public static boolean isLocal() {
        return getBaseUrl().equals(LOCAL_URL);
    }
}
