package com.sdsweather.ui;

import com.sdsweather.navigation.Navigator;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * AnalysisPage - Navigation hub for incident analysis and analytics features.
 * 
 * This page serves as an intermediate navigation point, directing users to either
 * the Incident History view (for browsing and managing incidents) or the Analytics
 * Dashboard (for viewing trends and patterns).
 * 
 * Features:
 * - Clean two-option navigation layout
 * - Card-based interface with hover effects
 * - Quick access to both analysis tools
 * - Consistent branding with logo display
 * 
 * @author SDS Weather Development Team
 * @version 1.0
 * @since 2026-02-16
 */
public class AnalysisPage extends VBox {

    /**
     * Constructs the Analysis navigation page.
     * 
     * Creates a simple, focused interface for choosing between incident
     * history viewing and analytics dashboards.
     */
    public AnalysisPage() {

        setStyle(AppStyles.PAGE_BACKGROUND);
        setPadding(new Insets(30));
        setSpacing(30);
        setAlignment(Pos.CENTER);

        // ===== HEADER =====
        HBox header = createHeader();

        // ===== TITLE SECTION =====
        VBox titleSection = new VBox(10);
        titleSection.setAlignment(Pos.CENTER);
        
        Label title = new Label("Analysis");
        title.setStyle(AppStyles.PAGE_TITLE);
        
        Label subtitle = new Label("Choose your analysis tool");
        subtitle.setStyle(AppStyles.SUBTITLE);
        
        titleSection.getChildren().addAll(title, subtitle);

        // ===== NAVIGATION CARDS =====
        HBox cardRow = new HBox(30);
        cardRow.setAlignment(Pos.CENTER);
        
        VBox incidentHistoryCard = createAnalysisCard(
            "📋",
            "Incident History",
            "View, filter, and manage all incidents across all units",
            () -> Navigator.show(new IncidentHistoryPage())
        );
        
        VBox analyticsCard = createAnalysisCard(
            "📊",
            "Analytics Dashboard",
            "View component failure trends and pattern analysis",
            () -> Navigator.show(new AnalyticsPage())
        );
        
        cardRow.getChildren().addAll(incidentHistoryCard, analyticsCard);

        // ===== BACK BUTTON =====
        Button back = new Button("← Back to Main Menu");
        AppStyles.styleSecondaryButton(back);
        back.setOnAction(e -> Navigator.show(new LandingPage()));

        getChildren().addAll(header, titleSection, cardRow, back);
    }

    /**
     * Creates the page header with logo and title.
     * 
     * @return HBox containing header elements
     */
    private HBox createHeader() {
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);

        try {
            Image logo = new Image(getClass().getResourceAsStream("/logosmall.png"));
            ImageView logoView = new ImageView(logo);
            logoView.setFitHeight(35);
            logoView.setPreserveRatio(true);
            header.getChildren().add(logoView);
        } catch (Exception e) {
            System.out.println("Small logo not found");
        }

        Label headerTitle = new Label("Analysis Tools");
        headerTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + AppStyles.DARK_TEXT + ";");

        header.getChildren().add(headerTitle);
        return header;
    }

    /**
     * Creates an individual analysis navigation card.
     * 
     * @param icon The emoji icon to display
     * @param title The card title
     * @param description Brief description of the feature
     * @param action The action to perform when clicked
     * @return VBox styled as a navigation card
     */
    private VBox createAnalysisCard(String icon, String title, String description, Runnable action) {
        VBox card = new VBox(20);
        card.setAlignment(Pos.CENTER);
        card.setPrefWidth(300);
        card.setPrefHeight(250);
        card.setStyle(
            "-fx-background-color: white; " +
            "-fx-background-radius: 10; " +
            "-fx-padding: 30; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2); " +
            "-fx-cursor: hand;"
        );
        
        // Icon
        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 64px;");
        
        // Title
        Label titleLabel = new Label(title);
        titleLabel.setStyle(
            "-fx-font-size: 20px; " +
            "-fx-font-weight: bold; " +
            "-fx-text-fill: " + AppStyles.DARK_TEXT + ";"
        );
        
        // Description
        Label descLabel = new Label(description);
        descLabel.setStyle(
            "-fx-font-size: 13px; " +
            "-fx-text-fill: " + AppStyles.LIGHT_TEXT + "; " +
            "-fx-text-alignment: center;"
        );
        descLabel.setWrapText(true);
        descLabel.setMaxWidth(250);
        
        card.getChildren().addAll(iconLabel, titleLabel, descLabel);
        
        // Hover effects
        card.setOnMouseEntered(e -> card.setStyle(
            "-fx-background-color: white; " +
            "-fx-background-radius: 10; " +
            "-fx-padding: 30; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 15, 0, 0, 3); " +
            "-fx-cursor: hand; " +
            "-fx-scale-x: 1.03; " +
            "-fx-scale-y: 1.03;"
        ));
        
        card.setOnMouseExited(e -> card.setStyle(
            "-fx-background-color: white; " +
            "-fx-background-radius: 10; " +
            "-fx-padding: 30; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2); " +
            "-fx-cursor: hand;"
        ));
        
        card.setOnMouseClicked(e -> action.run());
        
        return card;
    }
}