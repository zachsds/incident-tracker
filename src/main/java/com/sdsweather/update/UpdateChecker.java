package com.sdsweather.update;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Properties;

/**
 * UpdateChecker - Checks GitHub Releases for application updates.
 * 
 * Compares the current application version (from version.properties) against
 * the latest release on GitHub. Uses the GitHub Releases API to fetch version
 * information without requiring authentication.
 * 
 * @author Zachary Sneed
 * @version 1.0
 * @since 2026-02-18
 */
public class UpdateChecker {

    private static final String GITHUB_API_URL = "https://api.github.com/repos/zachsds/incident-tracker/releases/latest";
    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    /**
     * Checks if a newer version is available on GitHub.
     * 
     * @return UpdateInfo object with version comparison results, or null if check fails
     */
    public static UpdateInfo checkForUpdates() {
        try {
            String currentVersion = getCurrentVersion();
            String latestVersion = getLatestVersionFromGitHub();
            String downloadUrl = getDownloadUrlFromGitHub();

            if (currentVersion == null || latestVersion == null) {
                return null;
            }

            boolean updateAvailable = isNewerVersion(latestVersion, currentVersion);

            return new UpdateInfo(currentVersion, latestVersion, updateAvailable, downloadUrl);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Reads the current version from version.properties in resources.
     * 
     * @return Current version string (e.g., "1.0.0")
     */
    private static String getCurrentVersion() {
        try (InputStream input = UpdateChecker.class.getResourceAsStream("/version.properties")) {
            if (input == null) {
                System.err.println("version.properties not found");
                return null;
            }

            Properties props = new Properties();
            props.load(input);
            return props.getProperty("version");

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Fetches the latest release version from GitHub Releases API.
     * 
     * @return Latest version string from GitHub (e.g., "1.0.1")
     */
    private static String getLatestVersionFromGitHub() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GITHUB_API_URL))
                    .header("Accept", "application/vnd.github.v3+json")
                    .GET()
                    .build();

            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.err.println("GitHub API returned: " + response.statusCode());
                return null;
            }

            String body = response.body();

            // Simple JSON parsing without external libraries
            // Extract "tag_name":"v1.0.1"
            int tagIndex = body.indexOf("\"tag_name\"");
            if (tagIndex == -1) return null;

            int startQuote = body.indexOf("\"", tagIndex + 11);
            int endQuote = body.indexOf("\"", startQuote + 1);

            String tagName = body.substring(startQuote + 1, endQuote);

            // Remove 'v' prefix if present (v1.0.1 -> 1.0.1)
            return tagName.startsWith("v") ? tagName.substring(1) : tagName;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Fetches the download URL for the JAR from the latest GitHub Release.
     * 
     * @return Download URL for the JAR file
     */
    private static String getDownloadUrlFromGitHub() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GITHUB_API_URL))
                    .header("Accept", "application/vnd.github.v3+json")
                    .GET()
                    .build();

            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                return null;
            }

            String body = response.body();

            // Find the browser_download_url for the .jar file
            int assetsIndex = body.indexOf("\"assets\"");
            if (assetsIndex == -1) return null;

            // Look for browser_download_url after assets
            int urlIndex = body.indexOf("\"browser_download_url\"", assetsIndex);
            if (urlIndex == -1) return null;

            int startQuote = body.indexOf("\"", urlIndex + 22);
            int endQuote = body.indexOf("\"", startQuote + 1);

            return body.substring(startQuote + 1, endQuote);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Compares two version strings to determine if an update is available.
     * Uses semantic versioning comparison (major.minor.patch).
     * 
     * @param latest  The latest version from GitHub
     * @param current The current installed version
     * @return true if latest is newer than current
     */
    private static boolean isNewerVersion(String latest, String current) {
        try {
            String[] latestParts = latest.split("\\.");
            String[] currentParts = current.split("\\.");

            for (int i = 0; i < Math.min(latestParts.length, currentParts.length); i++) {
                int latestNum = Integer.parseInt(latestParts[i]);
                int currentNum = Integer.parseInt(currentParts[i]);

                if (latestNum > currentNum) {
                    return true;
                } else if (latestNum < currentNum) {
                    return false;
                }
            }

            // If all parts are equal, check if latest has more parts (e.g., 1.0.0.1 > 1.0.0)
            return latestParts.length > currentParts.length;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Data class holding update check results.
     */
    public static class UpdateInfo {
        public final String currentVersion;
        public final String latestVersion;
        public final boolean updateAvailable;
        public final String downloadUrl;

        public UpdateInfo(String currentVersion, String latestVersion, boolean updateAvailable, String downloadUrl) {
            this.currentVersion = currentVersion;
            this.latestVersion = latestVersion;
            this.updateAvailable = updateAvailable;
            this.downloadUrl = downloadUrl;
        }
    }
}