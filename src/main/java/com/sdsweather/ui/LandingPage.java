package com.sdsweather.ui;

import com.sdsweather.navigation.Navigator;
import com.sdsweather.security.SessionManager;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class LandingPage extends VBox {

    public LandingPage() {

        setPadding(new Insets(20));
        setSpacing(10);

        Label title = new Label("SDS Weather Incident Tracker");

        Button addUnit = new Button("Add Unit");
        Button viewUnits = new Button("View Units");
        Button analysis = new Button("Analysis");
        Button settings = new Button("Settings");
        Button logout = new Button("Logout");

        addUnit.setOnAction(e -> Navigator.show(new AddUnitPage()));
        viewUnits.setOnAction(e -> Navigator.show(new ViewUnitsPage()));
        analysis.setOnAction(e -> Navigator.show(new AnalysisPage()));
        settings.setOnAction(e -> Navigator.show(new SettingsPage()));

        logout.setOnAction(e -> {
            SessionManager.clear();
            Navigator.show(new LoginPage());
        });

        getChildren().addAll(title, addUnit, viewUnits, analysis, settings, logout);
    }
}