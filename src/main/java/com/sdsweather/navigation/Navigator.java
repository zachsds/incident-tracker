package com.sdsweather.navigation;

import com.sdsweather.security.SessionManager;
import com.sdsweather.ui.LoginPage;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

public class Navigator {

    private static Stage stage;

    public static void init(Stage primaryStage) {
        stage = primaryStage;
        stage.setTitle("SDS Incident Tracker");
        stage.setMinWidth(1000);  // Set minimum width
        stage.setMinHeight(700);  // Set minimum height
    }

    public static void show(Pane root) {

        if (!(root instanceof LoginPage) && !SessionManager.isLoggedIn()) {
            root = new LoginPage();
        }

        stage.setScene(new Scene(root, 1200, 800));  // Larger default size
        stage.show();
    }
}