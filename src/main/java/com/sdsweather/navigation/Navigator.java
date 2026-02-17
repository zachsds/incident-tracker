package com.sdsweather.navigation;

import com.sdsweather.security.SessionManager;
import com.sdsweather.ui.LoginPage;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

/**
 * Navigator - Centralized page navigation controller for the application.
 *
 * Manages all page transitions by holding a reference to the primary Stage and
 * replacing its Scene on each navigation call. Also enforces authentication —
 * if a non-login page is requested while no session is active, the Navigator
 * automatically redirects to LoginPage.
 *
 * Usage:
 *   Navigator.show(new SomePage());
 *
 * @author Zachary Sneed
 * @version 1.0
 * @since 2026-02-16
 */
public class Navigator {

    /** The primary JavaFX stage used for all page rendering */
    private static Stage stage;

    /**
     * Initializes the Navigator with the primary application stage.
     * Must be called once at startup before any navigation occurs.
     *
     * @param primaryStage The JavaFX primary stage from App.start()
     */
    public static void init(Stage primaryStage) {
        stage = primaryStage;
        stage.setTitle("SDS Incident Tracker");
        stage.setMinWidth(1000);  // Set minimum width
        stage.setMinHeight(700);  // Set minimum height
    }

    /**
     * Navigates to the given page by replacing the current scene.
     * Redirects to LoginPage if the user is not authenticated.
     *
     * @param root The page to display
     */
    public static void show(Pane root) {

        if (!(root instanceof LoginPage) && !SessionManager.isLoggedIn()) {
            root = new LoginPage();
        }

        stage.setScene(new Scene(root, 1200, 800));  // Larger default size
        stage.show();
    }
}