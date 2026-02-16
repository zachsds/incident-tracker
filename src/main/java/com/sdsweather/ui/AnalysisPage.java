package com.sdsweather.ui;

import com.sdsweather.navigation.Navigator;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class AnalysisPage extends VBox {

    public AnalysisPage() {

        setPadding(new Insets(20));
        setSpacing(15);
        setAlignment(Pos.TOP_CENTER);

        Label title = new Label("Analysis");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Button incidentHistory = new Button("Incident History");
        incidentHistory.setPrefWidth(200);
        incidentHistory.setOnAction(e -> Navigator.show(new IncidentHistoryPage()));

        Button analytics = new Button("Analytics Dashboard");
        analytics.setPrefWidth(200);
        analytics.setOnAction(e -> Navigator.show(new AnalyticsPage()));

        Label incidentDesc = new Label("View, filter, and manage all incidents");
        incidentDesc.setStyle("-fx-font-size: 11px; -fx-text-fill: gray;");

        Label analyticsDesc = new Label("Component failure trends and pattern analysis");
        analyticsDesc.setStyle("-fx-font-size: 11px; -fx-text-fill: gray;");

        Button back = new Button("Back to Main Menu");
        back.setOnAction(e -> Navigator.show(new LandingPage()));

        VBox incidentBox = new VBox(5, incidentHistory, incidentDesc);
        incidentBox.setAlignment(Pos.CENTER);

        VBox analyticsBox = new VBox(5, analytics, analyticsDesc);
        analyticsBox.setAlignment(Pos.CENTER);

        getChildren().addAll(
                title,
                incidentBox,
                analyticsBox,
                back
        );
    }
}