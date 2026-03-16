package com.sdsweather.ui;

import com.sdsweather.model.Incident;
import com.sdsweather.repository.ComponentReplacementRepository;
import com.sdsweather.repository.ComponentRepository;
import com.sdsweather.repository.IncidentRepository;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.util.List;

/**
 * EditReplacementDialog - Dialog for editing existing component replacement records.
 *
 * Pre-fills form fields with existing replacement data and allows user to update
 * component, linked incident, replaced by, cost, and notes fields.
 *
 * Fields:
 *   - Component (required) - dropdown selector, pre-selected
 *   - Related Incident (optional) - dropdown to link/unlink incident
 *   - Replaced By (required) - pre-filled with existing value
 *   - Cost (optional) - pre-filled if exists
 *   - Notes (optional) - pre-filled if exists
 *
 * @author Zachary Sneed
 * @version 1.0
 * @since 2026-03-16
 */
public class EditReplacementDialog extends Dialog<Void> {

    public EditReplacementDialog(ComponentReplacementRepository.ComponentReplacement replacement) {

        setTitle("Edit Component Replacement");
        setHeaderText("Update replacement record details");

        // Component selector dropdown
        ComboBox<String> componentBox = new ComboBox<>();
        componentBox.setPromptText("Select Component");

        // Load all available components and pre-select current component
        try {
            List<String> componentNames = ComponentRepository.getAllNames();
            componentBox.getItems().addAll(componentNames);
            
            // Pre-select the current component
            String currentComponentName = ComponentRepository.getNameById(replacement.componentId);
            componentBox.setValue(currentComponentName);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        // Incident selector dropdown - optional link to triggering incident
        ComboBox<String> incidentBox = new ComboBox<>();
        incidentBox.setPromptText("Link to Incident (optional)");
        incidentBox.getItems().add("-- No Incident --");

        // Load all incidents for this unit
        String selectedIncidentDisplay = null;
        try {
            List<Incident> incidents = IncidentRepository.getByUnit(replacement.unitId);
            for (Incident incident : incidents) {
                String display = incident.reportedAt.substring(0, 10) + " - " + incident.summary;
                incidentBox.getItems().add(display);
                // Store incident ID in combobox properties for later retrieval
                incidentBox.getProperties().put(display, incident.incidentId);
                
                // Pre-select if this replacement is linked to this incident
                if (replacement.incidentId != null && replacement.incidentId.equals(incident.incidentId)) {
                    selectedIncidentDisplay = display;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        
        // Set the pre-selected incident or default to "No Incident"
        incidentBox.setValue(selectedIncidentDisplay != null ? selectedIncidentDisplay : "-- No Incident --");

        // Pre-fill "Replaced By" field with existing value
        TextField replacedBy = new TextField(replacement.replacedBy);
        replacedBy.setPromptText("Replaced By");

        // Pre-fill cost field with existing value if present
        TextField costField = new TextField();
        if (replacement.cost != null) {
            costField.setText(String.format("%.2f", replacement.cost));
        }
        costField.setPromptText("Cost (optional, e.g. 25.99)");

        // Pre-fill notes area with existing notes if present
        TextArea notesArea = new TextArea();
        if (replacement.notes != null) {
            notesArea.setText(replacement.notes);
        }
        notesArea.setPromptText("Additional notes (optional)");
        notesArea.setPrefRowCount(3);

        // Form layout using GridPane
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        // Add labels and fields to grid
        grid.add(new Label("Component:"), 0, 0);
        grid.add(componentBox, 1, 0);
        grid.add(new Label("Related Incident:"), 0, 1);
        grid.add(incidentBox, 1, 1);
        grid.add(new Label("Replaced By:"), 0, 2);
        grid.add(replacedBy, 1, 2);
        grid.add(new Label("Cost:"), 0, 3);
        grid.add(costField, 1, 3);
        grid.add(new Label("Notes:"), 0, 4);
        grid.add(notesArea, 1, 4);

        // Set dialog content
        getDialogPane().setContent(grid);

        // Add Save and Cancel buttons
        ButtonType saveButton = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(saveButton, ButtonType.CANCEL);

        // Handle Save button click
        setResultConverter(button -> {
            if (button == saveButton) {
                try {
                    // Validate required fields
                    String selectedComponent = componentBox.getValue();
                    if (selectedComponent == null || selectedComponent.isBlank()) {
                        showError("Component is required");
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

                    // Get component ID from selected component name
                    String componentId = ComponentRepository.getIdByName(selectedComponent);
                    if (componentId == null) {
                        showError("Selected component not found");
                        return null;
                    }

                    // Update the replacement record in database
                    ComponentReplacementRepository.update(
                        replacement.id,
                        componentId,
                        incidentId,
                        replacedByText,
                        cost,
                        notes
                    );

                } catch (Exception ex) {
                    ex.printStackTrace();
                    showError("Failed to update replacement: " + ex.getMessage());
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
        alert.setHeaderText("Cannot update replacement");
        alert.setContentText(message);
        alert.showAndWait();
    }
}