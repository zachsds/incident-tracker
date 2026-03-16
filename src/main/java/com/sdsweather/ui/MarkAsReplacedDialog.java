package com.sdsweather.ui;

import com.sdsweather.model.Incident;
import com.sdsweather.repository.ComponentReplacementRepository;
import com.sdsweather.repository.ComponentRepository;
import com.sdsweather.repository.IncidentComponentRepository;
import com.sdsweather.security.SessionManager;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/**
 * MarkAsReplacedDialog - Quick dialog for creating replacement records from an incident.
 *
 * Pre-fills component checkboxes with components from the selected incident and
 * automatically links the replacements to that incident. Allows selecting multiple
 * components that were replaced as a result of this incident.
 *
 * Fields:
 *   - Components (required) - checkbox list pre-filled with incident components
 *   - Replaced By (required) - pre-filled with current username
 *   - Cost (optional) - numeric input (per component)
 *   - Notes (optional) - text area for additional details
 *
 * The incident link is automatic and not shown in the dialog.
 *
 * @author Zachary Sneed
 * @version 1.0
 * @since 2026-03-16
 */
public class MarkAsReplacedDialog extends Dialog<Void> {

    public MarkAsReplacedDialog(String unitId, Incident incident) {

        setTitle("Mark Component(s) as Replaced");
        setHeaderText("Record replacement for incident: " + incident.summary);

        // Component checkbox list - pre-filled with components from this incident
        VBox componentCheckboxContainer = new VBox(5);
        Label componentLabel = new Label("Select Components Replaced:");
        ScrollPane componentScroll = new ScrollPane(componentCheckboxContainer);
        componentScroll.setFitToWidth(true);
        componentScroll.setPrefHeight(120);

        // Load components that were affected by this incident
        List<CheckBox> componentCheckboxes = new ArrayList<>();
        try {
            List<String> componentIds = IncidentComponentRepository.getComponentIdsForIncident(incident.incidentId);
            for (String id : componentIds) {
                String name = ComponentRepository.getNameById(id);
                if (name != null) {
                    CheckBox cb = new CheckBox(name);
                    // Auto-select all components if only one incident component
                    if (componentIds.size() == 1) {
                        cb.setSelected(true);
                    }
                    componentCheckboxes.add(cb);
                    componentCheckboxContainer.getChildren().add(cb);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        // Pre-fill "Replaced By" with current logged-in username
        TextField replacedBy = new TextField(SessionManager.getUsername());
        replacedBy.setPromptText("Replaced By");

        // Cost input field - optional (per component)
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

                        // Create replacement record with automatic incident link
                        ComponentReplacementRepository.create(
                            unitId,
                            componentId,
                            incident.incidentId,  // Automatically link to this incident
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