package com.sdsweather.ui;

import com.sdsweather.navigation.Navigator;
import com.sdsweather.repository.UnitRepository;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

/**
 * AddUnitPage - Form for registering new units in the system.
 *
 * Supports both STOCK and DEPLOYED unit types. The form dynamically
 * shows the stock number field for STOCK units or the title field for
 * DEPLOYED units. After saving, offers options to add another or return
 * to the main menu.
 *
 * UUID Validation: Prevents spaces and special characters to ensure
 * compatibility with URL encoding and API calls.
 *
 * @author Zachary Sneed
 * @version 1.0
 * @since 2026-02-16
 */
public class AddUnitPage extends VBox {

    public AddUnitPage() {

        setPadding(new Insets(20));
        setSpacing(10);

        // Unit type selector: STOCK or DEPLOYED
        ComboBox<String> type = new ComboBox<>();
        type.getItems().addAll("STOCK","DEPLOYED");
        type.setValue("STOCK");

        // UUID field - validated to prevent spaces and special characters
        TextField uuid = new TextField();
        uuid.setPromptText("UUID (letters, numbers, hyphens, underscores only)");

        // Stock number field - visible only for STOCK type
        TextField stock = new TextField();
        stock.setPromptText("Stock Number");

        // Title field - visible only for DEPLOYED type
        TextField title = new TextField();
        title.setPromptText("Title");

        // Initially hide title field since STOCK is default
        title.setVisible(false);
        title.setManaged(false);

        // Toggle field visibility based on unit type selection
        type.setOnAction(e -> {
            boolean isStock = type.getValue().equals("STOCK");
            stock.setVisible(isStock);
            stock.setManaged(isStock);
            title.setVisible(!isStock);
            title.setManaged(!isStock);
        });

        // Result label for displaying errors or success messages
        Label result = new Label();

        Button save = new Button("Save");
        Button back = new Button("Back");

        // Save button handler with UUID validation
        save.setOnAction(e -> {
            try {
                
                // Validate UUID - no spaces or special characters allowed
                String uuidText = uuid.getText().trim();
                if (uuidText.isEmpty()) {
                    result.setText("UUID cannot be empty");
                    return;
                }
                
                // Check for spaces or special characters
                // Only allow: letters, numbers, hyphens, and underscores
                if (!uuidText.matches("^[a-zA-Z0-9_-]+$")) {
                    result.setText("UUID can only contain letters, numbers, hyphens, and underscores (no spaces)");
                    return;
                }

                // Create unit based on type
                if (type.getValue().equals("STOCK")) {
                    UnitRepository.createStock(uuidText, stock.getText());
                } else {
                    UnitRepository.createDeployed(uuidText, title.getText());
                }

                // Show success dialog with options
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Unit Saved");
                alert.setHeaderText("Unit saved successfully");

                ButtonType addAnother = new ButtonType("Add Another Unit");
                ButtonType mainMenu = new ButtonType("Return To Main Menu");

                alert.getButtonTypes().setAll(addAnother, mainMenu);

                ButtonType choice = alert.showAndWait().orElse(mainMenu);

                // Handle user choice
                if (choice == addAnother) {
                    // Clear fields for next unit
                    uuid.clear();
                    stock.clear();
                    title.clear();
                    result.setText("");
                } else {
                    // Return to main menu
                    Navigator.show(new LandingPage());
                }

            } catch (Exception ex) {
                result.setText(ex.getMessage());
                ex.printStackTrace();
            }
        });

        // Back button returns to main menu without saving
        back.setOnAction(e -> Navigator.show(new LandingPage()));

        // Add all UI elements to page
        getChildren().addAll(
                type,
                uuid,
                stock,
                title,
                save,
                result,
                back
        );
    }
}