package com.sdsweather.ui;

import com.sdsweather.model.Incident;
import com.sdsweather.repository.ComponentReplacementRepository;
import com.sdsweather.repository.ComponentRepository;
import com.sdsweather.repository.IncidentRepository;
import com.sdsweather.security.SessionManager;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/**
 * AddReplacementDialog - Dialog for creating new component replacement records.
 *
 * Allows user to select MULTIPLE components (via checkboxes), optionally link to 
 * an incident, enter who replaced them, optional cost, and optional notes.
 * Creates separate replacement records for each selected component with identical
 * metadata (date, replaced by, cost, notes, incident link).
 *
 * Fields:
 *   - Components (required) - checkbox list for multiple selection
 *   - Related Incident (optional) - dropdown to link replacement to incident
 *   - Replaced By (required) - pre-filled with current username
 *   - Cost (optional) - numeric input (applied to each component)
 *   - Notes (optional) - text area for additional details
 *
 * @author Zachary Sneed
 * @version 1.0
 * @since 2026-03-16
 */
public class AddReplacementDialog extends Dialog<Void> {

    public AddReplacementDialog(String unitId) {

        setTitle("Add Component Replacement");
        setHeaderText("Record component replacement(s) for this unit");

        // Component checkbox list for multiple selection
        VBox componentCheckboxContainer = new VBox(5);
        Label componentLabel = new Label("Select Components to Replace:");
        ScrollPane componentScroll = new ScrollPane(componentCheckboxContainer);
        componentScroll.setFitToWidth(true);
        componentScroll.setPrefHeight(150);

        // Load all available components and create checkboxes
        List<CheckBox> componentCheckboxes = new ArrayList<>();
        try {
            List<String> componentNames = ComponentRepository.getAllNames();
            for (String name : componentNames) {
                CheckBox cb = new CheckBox(name);
                componentCheckboxes.add(cb);
                componentCheckboxContainer.getChildren().add(cb);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        // Incident selector dropdown - optional link to triggering incident
        ComboBox<String> incidentBox = new ComboBox<>();
        incidentBox.setPromptText("Link to Incident (optional)");
        incidentBox.getItems().add("-- No Incident --");

        // Load all incidents for this unit
        try {
            List<Incident> incidents = IncidentRepository.getByUnit(unitId);
            for (Incident incident : incidents) {
                String display = incident.reportedAt.substring(0, 10) + " - " + incident.summary;
                incidentBox.getItems().add(display);
                // Store incident ID in combobox properties for later retrieval
                incidentBox.getProperties().put(display, incident.incidentId);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        incidentBox.setValue("-- No Incident --");

        // Pre-fill "Replaced By" with current logged-in username
        TextField replacedBy = new TextField(SessionManager.getUsername());
        replacedBy.setPromptText("Replaced By");

        // Cost input field - optional (will be applied to EACH component)
        TextField costField = new TextField();
        costField.setPromptText("Cost per component (optional, e.g. 25.99)");

        // Notes text area - optional, allows multi-line input
        TextArea notesArea = new TextArea();
        notesArea.setPromptText("Additional notes (optional)");
        notesArea.setPrefRowCount(3);

        // Form layout using GridPane
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        // Add labels and fields to grid
        int row = 0;
        grid.add(componentLabel, 0, row++, 2, 1);
        grid.add(componentScroll, 0, row++, 2, 1);
        grid.add(new Label("Related Incident:"), 0, row);
        grid.add(incidentBox, 1, row++);
        grid.add(new Label("Replaced By:"), 0, row);
        grid.add(replacedBy, 1, row++);
        grid.add(new Label("Cost:"), 0, row);
        grid.add(costField, 1, row++);
        grid.add(new Label("Notes:"), 0, row);
        grid.add(notesArea, 1, row++);

        // Set dialog content
        getDialogPane().setContent(grid);

        // Add Save and Cancel buttons
        ButtonType saveButton = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(saveButton, ButtonType.CANCEL);

        // Handle Save button click
        setResultConverter(button -> {
            if (button == saveButton) {
                try {
                    // Collect selected components
                    List<String> selectedComponents = new ArrayList<>();
                    for (CheckBox cb : componentCheckboxes) {
                        if (cb.isSelected()) {
                            selectedComponents.add(cb.getText());
                        }
                    }

                    // Validate at least one component selected
                    if (selectedComponents.isEmpty()) {
                        showError("Please select at least one component");
                        return null;
                    }

                    String replacedByText = replacedBy.getText();
                    if (replacedByText == null || replacedByText.isBlank()) {
                        showError("Replaced By is required");
                        return null;
                    }

                    // Get optional incident ID
                    String incidentId = null;
                    String selectedIncident = incidentBox.getValue();
                    if (selectedIncident != null && !selectedIncident.equals("-- No Incident --")) {
                        incidentId = (String) incidentBox.getProperties().get(selectedIncident);
                    }

                    // Parse optional cost field
                    Double cost = null;
                    if (!costField.getText().isBlank()) {
                        try {
                            cost = Double.parseDouble(costField.getText());
                            if (cost < 0) {
                                showError("Cost cannot be negative");
                                return null;
                            }
                        } catch (NumberFormatException ex) {
                            showError("Invalid cost format. Use numbers like 25.99");
                            return null;
                        }
                    }

                    // Get optional notes text
                    String notes = notesArea.getText();
                    if (notes != null && notes.isBlank()) {
                        notes = null;  // Store as null if empty
                    }

                    // Create a separate replacement record for EACH selected component
                    for (String componentName : selectedComponents) {
                        String componentId = ComponentRepository.getIdByName(componentName);
                        if (componentId == null) {
                            showError("Component not found: " + componentName);
                            return null;
                        }

                        // Create replacement record
                        ComponentReplacementRepository.create(
                            unitId,
                            componentId,
                            incidentId,
                            replacedByText,
                            cost,
                            notes
                        );
                    }

                } catch (Exception ex) {
                    ex.printStackTrace();
                    showError("Failed to save replacement: " + ex.getMessage());
                    return null;
                }
            }
            return null;
        });
    }

    /**
     * Helper method to show error alerts to the user.
     * 
     * @param message The error message to display
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Cannot save replacement");
        alert.setContentText(message);
        alert.showAndWait();
    }
}