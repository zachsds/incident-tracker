package com.sdsweather.update;

import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

/**
 * UpdateDialog - Manages the update prompt UI and update installation process.
 * 
 * Displays a dialog informing the user of available updates. Tracks how many times
 * the user has skipped an update — after 3 skips, the update is forced. Handles
 * downloading the new JAR, replacing the old one, and restarting the application.
 * 
 * @author Zachary Sneed
 * @version 1.0
 * @since 2026-02-18
 */
public class UpdateDialog extends Dialog<ButtonType> {

    private static final String SKIP_COUNT_FILE = System.getProperty("user.home") + "/.sdsweather_update_skips";
    private static final HttpClient CLIENT = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.ALWAYS)
        .build();

    /**
     * Shows the update dialog with current skip count enforcement.
     * 
     * @param updateInfo Information about the available update
     */
    public UpdateDialog(UpdateChecker.UpdateInfo updateInfo) {

        setTitle("Update Available");
        setHeaderText("A new version is available!");

        int skipCount = getSkipCount(updateInfo.latestVersion);
        boolean forceUpdate = skipCount >= 3;

        VBox content = new VBox(15);
        content.setPrefWidth(400);

        Label currentLabel = new Label("Current version: " + updateInfo.currentVersion);
        Label latestLabel = new Label("Latest version: " + updateInfo.latestVersion);
        latestLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #27ae60;");

        content.getChildren().addAll(currentLabel, latestLabel);

        if (forceUpdate) {
            Label forceLabel = new Label("This update is required and will be installed now.");
            forceLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
            content.getChildren().add(forceLabel);
        } else {
            Label skipLabel = new Label("You have skipped this update " + skipCount + " time(s).");
            skipLabel.setStyle("-fx-text-fill: #7f8c8d;");
            Label skipWarning = new Label("After 3 skips, the update will be required.");
            skipWarning.setStyle("-fx-text-fill: #f39c12; -fx-font-size: 11px;");
            content.getChildren().addAll(skipLabel, skipWarning);
        }

        getDialogPane().setContent(content);

        ButtonType installButton = new ButtonType("Install Update", ButtonBar.ButtonData.OK_DONE);
        ButtonType skipButton = new ButtonType("Skip", ButtonBar.ButtonData.CANCEL_CLOSE);

        if (forceUpdate) {
            getDialogPane().getButtonTypes().setAll(installButton);
        } else {
            getDialogPane().getButtonTypes().setAll(installButton, skipButton);
        }

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
     * Gets the number of times the user has skipped this specific version.
     * 
     * @param version The version to check skip count for
     * @return Number of skips (0-3+)
     */
    private int getSkipCount(String version) {
        try {
            File skipFile = new File(SKIP_COUNT_FILE);
            if (!skipFile.exists()) {
                return 0;
            }

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

            if (skipFile.exists()) {
                try (FileInputStream input = new FileInputStream(skipFile)) {
                    props.load(input);
                }
            }

            int currentCount = Integer.parseInt(props.getProperty(version, "0"));
            props.setProperty(version, String.valueOf(currentCount + 1));

            try (FileOutputStream output = new FileOutputStream(skipFile)) {
                props.store(output, "SDS Weather Update Skip Tracker");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Downloads and installs the update, then restarts the application.
     * 
     * @param downloadUrl GitHub download URL for the new JAR
     */
    private void installUpdate(String downloadUrl) {
        System.out.println("=== UPDATE STARTED ===");
        System.out.println("Download URL: " + downloadUrl);
        
        // Show progress dialog
        Alert progress = new Alert(Alert.AlertType.INFORMATION);
        progress.setTitle("Installing Update");
        progress.setHeaderText("Downloading update...");
        progress.setContentText("Please wait, this may take a moment.");
        progress.show();

        // Download and install on background thread
        new Thread(() -> {
            System.out.println("=== DOWNLOAD THREAD STARTED ===");
            try {
                System.out.println("About to create HTTP request...");
                
                // Download the new JAR
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(downloadUrl))
                        .GET()
                        .build();

                HttpResponse<byte[]> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());

                System.out.println("Response status: " + response.statusCode());
                System.out.println("Content-Type: " + response.headers().firstValue("Content-Type").orElse("unknown"));
                System.out.println("Final URI: " + response.uri());

                if (response.statusCode() != 200) {
                    showError("Download failed with status: " + response.statusCode());
                    return;
                }

                // Get the current JAR location
                String jarPath = UpdateDialog.class.getProtectionDomain()
                        .getCodeSource()
                        .getLocation()
                        .toURI()
                        .getPath();

                File currentJar = new File(jarPath);
                File newJar = new File(currentJar.getParent(), "incident-tracker-new.jar");
                File backupJar = new File(currentJar.getParent(), "incident-tracker-backup.jar");

                // Save the downloaded JAR
                Files.write(newJar.toPath(), response.body());

                // Backup the current JAR
                if (currentJar.exists()) {
                    Files.copy(currentJar.toPath(), backupJar.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }

                // Replace current JAR with new JAR
                Files.move(newJar.toPath(), currentJar.toPath(), StandardCopyOption.REPLACE_EXISTING);

                // Clear skip count for this version
                File skipFile = new File(SKIP_COUNT_FILE);
                if (skipFile.exists()) {
                    skipFile.delete();
                }

                Platform.runLater(() -> {
                    progress.close();

                    Alert success = new Alert(Alert.AlertType.INFORMATION);
                    success.setTitle("Update Complete");
                    success.setHeaderText("Update installed successfully!");
                    success.setContentText("The application will now restart.");
                    success.showAndWait();

                    // Restart the application
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
     * Restarts the application by launching a new instance and exiting the current one.
     * Handles macOS .app bundles, Windows .exe installers, and standalone JAR files.
     * 
     * @param jarFile The JAR file to execute
     */
    private void restartApplication(File jarFile) {
        try {
            String jarPath = jarFile.getAbsolutePath();
            String os = System.getProperty("os.name").toLowerCase();
            
            ProcessBuilder builder;
            
            if (os.contains("mac") && jarPath.contains(".app/Contents/")) {
                // Running from macOS .app bundle - use 'open' command to restart the app
                String appPath = jarPath.substring(0, jarPath.indexOf(".app/") + 4);
                builder = new ProcessBuilder("open", appPath);
            } else if (os.contains("win") && jarPath.contains("\\app\\")) {
                // Running from Windows jpackage .exe - find and restart the .exe
                String exePath = jarPath.substring(0, jarPath.indexOf("\\app\\")) + ".exe";
                builder = new ProcessBuilder(exePath);
            } else {
                // Running from standalone JAR - use java -jar
                String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
                builder = new ProcessBuilder(javaBin, "-jar", jarFile.getAbsolutePath());
            }
            
            builder.start();
            System.exit(0);

        } catch (Exception e) {
            e.printStackTrace();
            showError("Failed to restart application: " + e.getMessage());
        }
    }

    /**
     * Displays an error alert to the user.
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