package com.sdsweather.ui;

import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;

/**
 * AppStyles - Centralized style constants and helper methods for the UI.
 *
 * Defines the application's color palette, button styles, input field styles,
 * card styles, and typography. All pages reference these constants to maintain
 * a consistent look and feel throughout the application.
 *
 * Usage:
 *   AppStyles.stylePrimaryButton(myButton);
 *   label.setStyle(AppStyles.PAGE_TITLE);
 *
 * @author Zachary Sneed
 * @version 1.0
 * @since 2026-02-16
 */
public class AppStyles {

    // Color Palette
    public static final String PRIMARY_COLOR = "#3498db";      // Blue
    public static final String PRIMARY_DARK = "#2980b9";       // Darker blue
    public static final String SUCCESS_COLOR = "#27ae60";      // Green
    public static final String WARNING_COLOR = "#f39c12";      // Orange
    public static final String DANGER_COLOR = "#e74c3c";       // Red
    public static final String DARK_TEXT = "#2c3e50";          // Dark gray
    public static final String LIGHT_TEXT = "#7f8c8d";         // Light gray
    public static final String BACKGROUND = "#ecf0f1";         // Light background
    public static final String CARD_BACKGROUND = "#ffffff";    // White cards

    // Button Styles
    public static final String PRIMARY_BUTTON = 
        "-fx-background-color: " + PRIMARY_COLOR + "; " +
        "-fx-text-fill: white; " +
        "-fx-font-size: 14px; " +
        "-fx-padding: 10 20; " +
        "-fx-background-radius: 5; " +
        "-fx-cursor: hand;";

    public static final String PRIMARY_BUTTON_HOVER = 
        "-fx-background-color: " + PRIMARY_DARK + "; " +
        "-fx-text-fill: white; " +
        "-fx-font-size: 14px; " +
        "-fx-padding: 10 20; " +
        "-fx-background-radius: 5; " +
        "-fx-cursor: hand;";

    public static final String DANGER_BUTTON = 
        "-fx-background-color: " + DANGER_COLOR + "; " +
        "-fx-text-fill: white; " +
        "-fx-font-size: 14px; " +
        "-fx-padding: 10 20; " +
        "-fx-background-radius: 5; " +
        "-fx-cursor: hand;";

    public static final String SUCCESS_BUTTON = 
        "-fx-background-color: " + SUCCESS_COLOR + "; " +
        "-fx-text-fill: white; " +
        "-fx-font-size: 14px; " +
        "-fx-padding: 10 20; " +
        "-fx-background-radius: 5; " +
        "-fx-cursor: hand;";

    public static final String SECONDARY_BUTTON = 
        "-fx-background-color: transparent; " +
        "-fx-text-fill: " + PRIMARY_COLOR + "; " +
        "-fx-font-size: 14px; " +
        "-fx-padding: 10 20; " +
        "-fx-border-color: " + PRIMARY_COLOR + "; " +
        "-fx-border-width: 2; " +
        "-fx-border-radius: 5; " +
        "-fx-background-radius: 5; " +
        "-fx-cursor: hand;";

    // Input Field Styles
    public static final String TEXT_FIELD = 
        "-fx-padding: 10; " +
        "-fx-font-size: 14px; " +
        "-fx-background-radius: 5; " +
        "-fx-border-color: #ddd; " +
        "-fx-border-radius: 5; " +
        "-fx-border-width: 1;";

    // Card/Container Styles
    public static final String CARD = 
        "-fx-background-color: " + CARD_BACKGROUND + "; " +
        "-fx-background-radius: 10; " +
        "-fx-padding: 20; " +
        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);";

    public static final String PAGE_BACKGROUND = 
        "-fx-background-color: " + BACKGROUND + ";";

    // Title Styles
    public static final String PAGE_TITLE = 
        "-fx-font-size: 24px; " +
        "-fx-font-weight: bold; " +
        "-fx-text-fill: " + DARK_TEXT + ";";

    public static final String SECTION_TITLE = 
        "-fx-font-size: 16px; " +
        "-fx-font-weight: bold; " +
        "-fx-text-fill: " + DARK_TEXT + ";";

    public static final String SUBTITLE = 
        "-fx-font-size: 14px; " +
        "-fx-text-fill: " + LIGHT_TEXT + ";";

    // Table Styles
    public static final String TABLE_HEADER = 
        "-fx-font-weight: bold; " +
        "-fx-background-color: " + BACKGROUND + ";";

    // Hover effects helper
    public static void addHoverEffect(Button button, String normalStyle, String hoverStyle) {
        button.setStyle(normalStyle);
        button.setOnMouseEntered(e -> button.setStyle(hoverStyle));
        button.setOnMouseExited(e -> button.setStyle(normalStyle));
    }

    public static void stylePrimaryButton(Button button) {
        addHoverEffect(button, PRIMARY_BUTTON, PRIMARY_BUTTON_HOVER);
    }

    public static void styleDangerButton(Button button) {
        button.setStyle(DANGER_BUTTON);
    }

    public static void styleSuccessButton(Button button) {
        button.setStyle(SUCCESS_BUTTON);
    }

    public static void styleSecondaryButton(Button button) {
        button.setStyle(SECONDARY_BUTTON);
    }

    public static void styleTextField(TextField field) {
        field.setStyle(TEXT_FIELD);
    }

    public static void stylePasswordField(PasswordField field) {
        field.setStyle(TEXT_FIELD);
    }
}