package com.sdsweather.ui;

import com.sdsweather.navigation.Navigator;
import com.sdsweather.security.SessionManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;

/**
 * LandingPage - Main navigation hub for the SDS Weather Incident Tracker application.
 * 
 * This page serves as the primary entry point after user authentication, providing
 * quick access to all major features including unit management, incident tracking,
 * analytics, and system settings.
 * 
 * Features:
 * - Company branding with logo display
 * - Role-based access to administrative features
 * - Clean, card-based navigation layout
 * - User identification and logout functionality
 * 
 * @author Zachary Sneed
 * @version 1.0
 * @since 2026-02-16
 */
public class LandingPage extends VBox {

    /**
     * Constructs the main landing page with navigation options.
     * 
     * The page layout includes:
     * - Header with logo and user information
     * - Grid of navigation cards for primary features
     * - Logout button for session termination
     */
    public LandingPage() {

        // Set page background and spacing
        setStyle(AppStyles.PAGE_BACKGROUND);
        setPadding(new Insets(30));
        setSpacing(30);
        setAlignment(Pos.CENTER); // Center everything in the page
        setAlignment(Pos.TOP_CENTER);  // Center everything
        setAlignment(Pos.TOP_CENTER);  // Center all content in the page

        // ===== HEADER SECTION =====
        HBox header = createHeader();
        
        // ===== WELCOME SECTION =====
        VBox welcomeSection = new VBox(10);
        welcomeSection.setAlignment(Pos.CENTER);
        
        Label welcomeLabel = new Label("Welcome, " + SessionManager.getUsername());
        welcomeLabel.setStyle(AppStyles.PAGE_TITLE);
        
        Label subtitle = new Label("Incident Tracker");
        subtitle.setStyle(AppStyles.SUBTITLE);
        
        welcomeSection.getChildren().addAll(welcomeLabel, subtitle);

        // ===== NAVIGATION CARDS =====
        VBox navCards = createNavigationCards();

        getChildren().addAll(header, welcomeSection, navCards);
    }

    /**
     * Creates the header bar with logo and user info.
     * 
     * @return HBox containing the header elements
     */
    private HBox createHeader() {
        HBox header = new HBox(20);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setMaxWidth(1200);  // Constrain width to match content
        
        // Add small logo if available
        try {
            Image logo = new Image(getClass().getResourceAsStream("/logosmall.png"));
            ImageView logoView = new ImageView(logo);
            logoView.setFitHeight(40);
            logoView.setPreserveRatio(true);
            header.getChildren().add(logoView);
        } catch (Exception e) {
            // Logo not found - continue without it
            System.out.println("Small logo not found - continuing without header logo");
        }
        
        Label appName = new Label("Incident Tracker");
        appName.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + AppStyles.DARK_TEXT + ";");
        
        // Spacer to push logout to the right
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Label userLabel = new Label("Logged in as: " + SessionManager.getUsername());
        userLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + AppStyles.LIGHT_TEXT + ";");
        
        header.getChildren().addAll(appName, spacer, userLabel);
        return header;
    }

    /**
     * Creates the main navigation card grid.
     * 
     * Navigation cards are organized in a responsive grid layout, with each
     * card representing a major feature of the application. Cards include
     * icons (represented by emoji), titles, and descriptions.
     * 
     * @return VBox containing all navigation cards
     */
    private VBox createNavigationCards() {
        VBox container = new VBox(20);
        container.setAlignment(Pos.CENTER);
        container.setMaxWidth(800);
        
        // Create rows of cards
        HBox row1 = new HBox(20);
        row1.setAlignment(Pos.CENTER);
        
        HBox row2 = new HBox(20);
        row2.setAlignment(Pos.CENTER);
        
        // First row: Primary operations
        VBox addUnitCard = createNavCard(
            "➕", 
            "Add Unit", 
            "Register new stock or deployed units",
            () -> Navigator.show(new AddUnitPage())
        );
        
        VBox viewUnitsCard = createNavCard(
            "📋", 
            "View Units", 
            "Browse and manage all units",
            () -> Navigator.show(new ViewUnitsPage())
        );
        
        VBox analysisCard = createNavCard(
            "📊", 
            "Analysis", 
            "View incidents and analytics",
            () -> Navigator.show(new AnalysisPage())
        );
        
        row1.getChildren().addAll(addUnitCard, viewUnitsCard, analysisCard);
        
        // Second row: Settings and logout
        VBox settingsCard = createNavCard(
            "⚙️", 
            "Settings", 
            "Manage components and users",
            () -> Navigator.show(new SettingsPage())
        );
        
        VBox logoutCard = createNavCard(
            "🚪", 
            "Logout", 
            "End your session",
            () -> {
                SessionManager.clear();
                Navigator.show(new LoginPage());
            }
        );
        
        row2.getChildren().addAll(settingsCard, logoutCard);
        
        container.getChildren().addAll(row1, row2);
        return container;
    }

    /**
     * Creates an individual navigation card with icon, title, and action.
     * 
     * Each card is a clickable white container with hover effects that
     * navigates to a specific feature when clicked.
     * 
     * @param icon The emoji icon to display
     * @param title The card title
     * @param description Brief description of the feature
     * @param action The action to perform when clicked
     * @return VBox styled as a navigation card
     */
    private VBox createNavCard(String icon, String title, String description, Runnable action) {
        VBox card = new VBox(15);
        card.setAlignment(Pos.CENTER);
        card.setPrefWidth(230);
        card.setPrefHeight(180);
        card.setStyle(
            "-fx-background-color: white; " +
            "-fx-background-radius: 10; " +
            "-fx-padding: 25; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2); " +
            "-fx-cursor: hand;"
        );
        
        // Icon label
        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 48px;");
        
        // Title label
        Label titleLabel = new Label(title);
        titleLabel.setStyle(
            "-fx-font-size: 18px; " +
            "-fx-font-weight: bold; " +
            "-fx-text-fill: " + AppStyles.DARK_TEXT + ";"
        );
        
        // Description label
        Label descLabel = new Label(description);
        descLabel.setStyle(
            "-fx-font-size: 12px; " +
            "-fx-text-fill: " + AppStyles.LIGHT_TEXT + "; " +
            "-fx-text-alignment: center;"
        );
        descLabel.setWrapText(true);
        descLabel.setMaxWidth(200);
        
        card.getChildren().addAll(iconLabel, titleLabel, descLabel);
        
        // Hover effects
        card.setOnMouseEntered(e -> card.setStyle(
            "-fx-background-color: white; " +
            "-fx-background-radius: 10; " +
            "-fx-padding: 25; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 15, 0, 0, 3); " +
            "-fx-cursor: hand; " +
            "-fx-scale-x: 1.02; " +
            "-fx-scale-y: 1.02;"
        ));
        
        card.setOnMouseExited(e -> card.setStyle(
            "-fx-background-color: white; " +
            "-fx-background-radius: 10; " +
            "-fx-padding: 25; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2); " +
            "-fx-cursor: hand;"
        ));
        
        // Click action
        card.setOnMouseClicked(e -> action.run());
        
        return card;
    }
}