package com.sdsweather;

/**
 * Launcher - Non-modular entry point for the SDS Weather Incident Tracker.
 *
 * Wraps App to work around JavaFX module system restrictions when packaging
 * with jpackage. When JavaFX is loaded from the classpath rather than the
 * module path, jpackage requires the main class not to extend Application
 * directly. This class resolves that constraint.
 *
 * @author Zachary Sneed
 * @version 1.0
 * @since 2026-02-16
 */
public class Launcher {

    /**
     * Entry point used by native installers created with jpackage.
     * Delegates immediately to App.main() to start the JavaFX application.
     *
     * @param args Command-line arguments passed through to App
     */
    public static void main(String[] args) {
        // Launch the JavaFX app without requiring module system
        App.main(args);
    }
}