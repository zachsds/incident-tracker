package com.sdsweather.update;

import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

/**
 * UpdateDialog - Manages the update prompt UI and update installation process.
 * 
 * Displays a dialog informing the user of available updates. Tracks how many times
 * the user has skipped an update — after 3 skips, the update is forced. Handles
 * downloading the new JAR from GitHub, replacing the old one, and restarting the
 * application with platform-specific restart logic.
 * 
 * Update Skip Tracking:
 *   - Stores skip count in ~/.sdsweather_update_skips
 *   - After 3 skips, update becomes mandatory
 *   - Skip count resets after successful update
 * 
 * Platform Support:
 *   - macOS: Uses 'open -n' to restart .app bundle
 *   - Windows: Creates batch script with delay to restart .exe
 *   - Standalone JAR: Launches new Java process
 * 
 * @author Zachary Sneed
 * @version 1.0
 * @since 2026-02-18
 */
public class UpdateDialog extends Dialog<ButtonType> {

    private static final String SKIP_COUNT_FILE = System.getProperty("user.home") + "/.sdsweather_update_skips";
    
    // HTTP client configured to follow redirects (GitHub assets redirect to Azure blob storage)
    private static final HttpClient CLIENT = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.ALWAYS)
        .build();

    /**
     * Constructs the update dialog with skip tracking and forced update logic.
     * 
     * @param updateInfo Information about the available update from UpdateChecker
     */
    public UpdateDialog(UpdateChecker.UpdateInfo updateInfo) {

        setTitle("Update Available");
        setHeaderText("A new version is available!");

        // Check how many times user has skipped this version
        int skipCount = getSkipCount(updateInfo.latestVersion);
        boolean forceUpdate = skipCount >= 3;

        // Build dialog content
        VBox content = new VBox(15);
        content.setPrefWidth(400);

        Label currentLabel = new Label("Current version: " + updateInfo.currentVersion);
        Label latestLabel = new Label("Latest version: " + updateInfo.latestVersion);
        latestLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #27ae60;");

        content.getChildren().addAll(currentLabel, latestLabel);

        if (forceUpdate) {
            // After 3 skips, update is mandatory
            Label forceLabel = new Label("This update is required and will be installed now.");
            forceLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
            content.getChildren().add(forceLabel);
        } else {
            // Show skip count and warning
            Label skipLabel = new Label("You have skipped this update " + skipCount + " time(s).");
            skipLabel.setStyle("-fx-text-fill: #7f8c8d;");
            Label skipWarning = new Label("After 3 skips, the update will be required.");
            skipWarning.setStyle("-fx-text-fill: #f39c12; -fx-font-size: 11px;");
            content.getChildren().addAll(skipLabel, skipWarning);
        }

        getDialogPane().setContent(content);

        // Configure dialog buttons
        ButtonType installButton = new ButtonType("Install Update", ButtonBar.ButtonData.OK_DONE);
        ButtonType skipButton = new ButtonType("Skip", ButtonBar.ButtonData.CANCEL_CLOSE);

        if (forceUpdate) {
            // Only show install button when update is forced
            getDialogPane().getButtonTypes().setAll(installButton);
        } else {
            // Show both install and skip buttons
            getDialogPane().getButtonTypes().setAll(installButton, skipButton);
        }

        // Handle button clicks
        setResultConverter(buttonType -> {
            if (buttonType == installButton) {
                installUpdate(updateInfo.downloadUrl);
            } else if (buttonType == skipButton) {
                incrementSkipCount(updateInfo.latestVersion);
            }
            return buttonType;
        });
    }

    /**
     * Retrieves the number of times the user has skipped a specific version.
     * 
     * @param version The version string to check (e.g., "0.0.9")
     * @return Number of times this version has been skipped (0 if never skipped)
     */
    private int getSkipCount(String version) {
        try {
            File skipFile = new File(SKIP_COUNT_FILE);
            if (!skipFile.exists()) {
                return 0;
            }

            // Load skip count properties file
            Properties props = new Properties();
            try (FileInputStream input = new FileInputStream(skipFile)) {
                props.load(input);
            }

            String countStr = props.getProperty(version, "0");
            return Integer.parseInt(countStr);

        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * Increments the skip count for a specific version.
     * 
     * @param version The version that was skipped
     */
    private void incrementSkipCount(String version) {
        try {
            File skipFile = new File(SKIP_COUNT_FILE);
            Properties props = new Properties();

            // Load existing skip counts if file exists
            if (skipFile.exists()) {
                try (FileInputStream input = new FileInputStream(skipFile)) {
                    props.load(input);
                }
            }

            // Increment skip count for this version
            int currentCount = Integer.parseInt(props.getProperty(version, "0"));
            props.setProperty(version, String.valueOf(currentCount + 1));

            // Save updated skip counts
            try (FileOutputStream output = new FileOutputStream(skipFile)) {
                props.store(output, "SDS Weather Update Skip Tracker");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Downloads and installs the update on a background thread.
     * Shows progress dialog during download and handles errors gracefully.
     * 
     * @param downloadUrl GitHub asset download URL for the new JAR
     */
    private void installUpdate(String downloadUrl) {
        // Show progress dialog during download
        Alert progress = new Alert(Alert.AlertType.INFORMATION);
        progress.setTitle("Installing Update");
        progress.setHeaderText("Downloading update...");
        progress.setContentText("Please wait, this may take a moment.");
        progress.show();

        // Perform download and installation on background thread to avoid blocking UI
        new Thread(() -> {
            try {
                // Build HTTP GET request for JAR download
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(downloadUrl))
                        .GET()
                        .build();

                // Download JAR file (HTTP client follows redirects automatically)
                HttpResponse<byte[]> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());

                // Verify successful download
                if (response.statusCode() != 200) {
                    showError("Download failed with status: " + response.statusCode());
                    return;
                }

                // Get current JAR file location
                String jarPath = UpdateDialog.class.getProtectionDomain()
                        .getCodeSource()
                        .getLocation()
                        .toURI()
                        .getPath();

                File currentJar = new File(jarPath);
                File newJar = new File(currentJar.getParent(), "incident-tracker-new.jar");
                File backupJar = new File(currentJar.getParent(), "incident-tracker-backup.jar");

                // Write downloaded JAR to temporary file
                Files.write(newJar.toPath(), response.body());

                // Backup current JAR before replacing
                if (currentJar.exists()) {
                    Files.copy(currentJar.toPath(), backupJar.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }

                // Replace current JAR with new JAR
                Files.move(newJar.toPath(), currentJar.toPath(), StandardCopyOption.REPLACE_EXISTING);

                // Clear skip count after successful update
                File skipFile = new File(SKIP_COUNT_FILE);
                if (skipFile.exists()) {
                    skipFile.delete();
                }

                // Show success dialog and restart application on UI thread
                Platform.runLater(() -> {
                    progress.close();

                    Alert success = new Alert(Alert.AlertType.INFORMATION);
                    success.setTitle("Update Complete");
                    success.setHeaderText("Update installed successfully!");
                    success.setContentText("The application will now restart.");
                    success.showAndWait();

                    // Restart application with platform-specific logic
                    restartApplication(currentJar);
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    progress.close();
                    showError("Update failed: " + e.getMessage());
                });
            }
        }).start();
    }

    /**
     * Restarts the application using platform-specific logic.
     * 
     * macOS: Uses 'open -n' to launch a new instance of the .app bundle
     * Windows: Creates batch script with 5-second delay before restarting .exe
     * Other: Launches new Java process with same JAR
     * 
     * @param jarFile The JAR file location (used to determine platform and restart method)
     */
    private void restartApplication(File jarFile) {
        try {
            String jarPath = jarFile.getAbsolutePath();
            String os = System.getProperty("os.name").toLowerCase();
            
            ProcessBuilder builder;
            
            if (os.contains("mac") && jarPath.contains(".app/Contents/")) {
                // macOS .app bundle - use 'open -n' to start new instance
                String appPath = jarPath.substring(0, jarPath.indexOf(".app/") + 4);
                builder = new ProcessBuilder("open", "-n", appPath);
                builder.start();
                
                // Wait for new process to start before exiting
                Thread.sleep(2000);
                System.exit(0);
                
            } else if (os.contains("win") && jarPath.contains("\\app\\")) {
                // Windows jpackage .exe - create batch script with delay
                String appDir = jarPath.substring(0, jarPath.indexOf("\\app\\"));
                String exePath = appDir + ".exe";
                
                // Create temporary batch file to restart after delay
                File batchFile = new File(System.getProperty("java.io.tmpdir"), "restart_sds.bat");
                try (FileWriter writer = new FileWriter(batchFile)) {
                    writer.write("@echo off\n");
                    writer.write("timeout /t 5 /nobreak > nul\n");  // Wait 5 seconds for app to exit
                    writer.write("start \"\" \"" + exePath + "\"\n");  // Launch .exe
                    writer.write("del \"%~f0\"\n");  // Delete batch file after execution
                }
                
                // Execute batch script
                builder = new ProcessBuilder("cmd.exe", "/c", batchFile.getAbsolutePath());
                builder.start();
                
                // Wait briefly then exit to allow batch script to take over
                Thread.sleep(2000);
                System.exit(0);
                
            } else {
                // Standalone JAR - launch new Java process
                String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
                builder = new ProcessBuilder(javaBin, "-jar", jarFile.getAbsolutePath());
                builder.start();
                
                // Wait for new process to start before exiting
                Thread.sleep(500);
                System.exit(0);
            }

        } catch (Exception e) {
            e.printStackTrace();
            showError("Failed to restart application: " + e.getMessage());
        }
    }

    /**
     * Displays an error alert dialog to the user.
     * Must be called on the JavaFX Application Thread.
     * 
     * @param message Error message to display
     */
    private void showError(String message) {
        Platform.runLater(() -> {
            Alert error = new Alert(Alert.AlertType.ERROR);
            error.setTitle("Update Error");
            error.setHeaderText("Update failed");
            error.setContentText(message);
            error.showAndWait();
        });
    }
}