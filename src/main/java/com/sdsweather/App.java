package com.sdsweather;

import com.sdsweather.navigation.Navigator;
import com.sdsweather.ui.LoginPage;
import com.sdsweather.update.UpdateChecker;
import com.sdsweather.update.UpdateDialog;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

/**
 * App - Main entry point for the SDS Weather Incident Tracker application.
 *
 * Extends JavaFX's Application class and bootstraps the application by
 * initializing the Navigator and displaying the LoginPage on startup.
 * Checks for updates from GitHub on launch.
 *
 * The application communicates with a PostgreSQL-backed REST API hosted on
 * the Rock Pi server at 192.168.0.237:3000, secured with a bundled
 * self-signed SSL certificate.
 *
 * @author Zachary Sneed
 * @version 1.0
 * @since 2026-02-16
 */
public class App extends Application {

    /**
     * JavaFX entry point. Called by the runtime after launch().
     * Initializes the Navigator, checks for updates, and shows the login page.
     *
     * @param stage The primary stage provided by the JavaFX runtime
     * @throws Exception If initialization fails
     */
    @Override
    public void start(Stage stage) throws Exception {

        // Initialize the Navigator with the primary stage
        Navigator.init(stage);

        // Display the login page as the first view
        Navigator.show(new LoginPage());

        // Check for updates in the background (non-blocking)
        checkForUpdates();
    }

    /**
     * Checks for application updates from GitHub in a background thread.
     * If an update is available, displays the UpdateDialog to the user.
     */
    private void checkForUpdates() {
        new Thread(() -> {
            try {
                UpdateChecker.UpdateInfo updateInfo = UpdateChecker.checkForUpdates();

                if (updateInfo != null && updateInfo.updateAvailable) {
                    Platform.runLater(() -> {
                        UpdateDialog dialog = new UpdateDialog(updateInfo);
                        dialog.showAndWait();
                    });
                }

            } catch (Exception e) {
                // Silently fail - don't block app startup if update check fails
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * Application main method. Calls JavaFX launch() which invokes start().
     *
     * @param args Command-line arguments (not currently used)
     */
    public static void main(String[] args) {
        launch();
    }
}