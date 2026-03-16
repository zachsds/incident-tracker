package com.sdsweather.ui;

import com.sdsweather.repository.ComponentCategoryRepository;
import com.sdsweather.repository.ComponentRepository;
import com.sdsweather.repository.IncidentComponentRepository;
import com.sdsweather.repository.IncidentRepository;
import com.sdsweather.repository.IncidentTemplateRepository;
import com.sdsweather.model.Incident;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * EditIncidentDialog - Modal dialog for editing an existing incident.
 *
 * Pre-fills form fields with the current incident data including summary,
 * severity, and linked components. Allows user to modify any field and
 * save changes back to the database.
 *
 * Component Selection Features:
 *   - Shows currently linked components in selected list
 *   - Add/remove components across multiple categories
 *   - Clear all button for quick reset
 *   - Two-column interface: Available components | Selected components
 *
 * On save, updates the incident record and component links via the API by:
 *   1. Updating incident summary and severity
 *   2. Removing all existing component links
 *   3. Adding new component links based on current selection
 *
 * Usage:
 *   new EditIncidentDialog(incident).showAndWait();
 *
 * @author Zachary Sneed
 * @version 1.0
 * @since 2026-03-05
 */
public class EditIncidentDialog extends Dialog<Void> {

    public EditIncidentDialog(Incident incident) {

        setTitle("Edit Incident");

        // Template selector at top of dialog
        ComboBox<String> templateBox = new ComboBox<>();
        templateBox.setPromptText("Apply Template (optional)");
        templateBox.getItems().add("-- Keep Current --");
        
        // Load available templates from database
        try {
            List<IncidentTemplateRepository.Template> templates = IncidentTemplateRepository.getAll();
            for (IncidentTemplateRepository.Template t : templates) {
                templateBox.getItems().add(t.name);
                // Store template object in combobox properties for retrieval
                templateBox.getProperties().put(t.name, t);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        
        templateBox.setValue("-- Keep Current --");

        // Pre-fill summary field with existing incident description
        TextField summary = new TextField(incident.summary);
        summary.setPromptText("Incident Description");

        // Pre-fill severity dropdown with existing severity level
        ComboBox<String> severity = new ComboBox<>();
        severity.getItems().addAll("LOW","MEDIUM","HIGH");
        severity.setValue(incident.severity);

        // Category selector for browsing available components
        ComboBox<String> categoryBox = new ComboBox<>();
        categoryBox.setPromptText("Select Category");

        // Load component categories from database
        try {
            categoryBox.getItems().addAll(
                    ComponentCategoryRepository.getAllActiveNames()
            );
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        // List showing available components from selected category
        ListView<String> availableComponents = new ListView<>();
        availableComponents.setPrefHeight(150);

        // Map to store selected components (componentName -> componentId)
        Map<String, String> selectedComponentMap = new HashMap<>();
        
        // Observable list for selected components display
        ObservableList<String> selectedComponentsList = FXCollections.observableArrayList();
        
        // ListView showing currently selected components
        ListView<String> selectedComponentsView = new ListView<>(selectedComponentsList);
        selectedComponentsView.setPrefHeight(150);

        // Pre-load existing components linked to this incident
        try {
            List<String> componentIds = IncidentComponentRepository.getComponentIdsForIncident(incident.incidentId);
            for (String componentId : componentIds) {
                String componentName = ComponentRepository.getNameById(componentId);
                if (componentName != null) {
                    selectedComponentMap.put(componentName, componentId);
                    selectedComponentsList.add(componentName);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        // Load available components when category is selected
        categoryBox.setOnAction(e -> {
            availableComponents.getItems().clear();
            try {
                String categoryId = ComponentCategoryRepository.getIdByName(categoryBox.getValue());
                availableComponents.getItems().addAll(
                        ComponentRepository.getActiveNamesByCategory(categoryId)
                );
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        // Button to add selected component from available list to selected list
        Button addComponentToIncident = new Button("Add Selected →");
        addComponentToIncident.setOnAction(e -> {
            String selected = availableComponents.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            
            // Don't add duplicates
            if (selectedComponentMap.containsKey(selected)) return;
            
            try {
                // Get component ID and store in map
                String componentId = ComponentRepository.getIdByName(selected);
                if (componentId != null) {
                    selectedComponentMap.put(selected, componentId);
                    selectedComponentsList.add(selected);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        // Button to remove selected component from selected list
        Button removeComponentFromIncident = new Button("← Remove Selected");
        removeComponentFromIncident.setOnAction(e -> {
            String selected = selectedComponentsView.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            
            // Remove from both map and display list
            selectedComponentMap.remove(selected);
            selectedComponentsList.remove(selected);
        });

        // Button to clear all selected components at once
        Button clearAllComponents = new Button("Clear All");
        clearAllComponents.setOnAction(e -> {
            // Clear both the map and display list
            selectedComponentMap.clear();
            selectedComponentsList.clear();
        });

        // Template selection handler - applies template to current incident
        templateBox.setOnAction(e -> {
            String selected = templateBox.getValue();
            if (selected == null || selected.equals("-- Keep Current --")) {
                // Don't change anything
                return;
            }

            // Retrieve selected template from combobox properties
            IncidentTemplateRepository.Template template = 
                (IncidentTemplateRepository.Template) templateBox.getProperties().get(selected);
            
            if (template != null) {
                // Apply template severity
                severity.setValue(template.severity);
                
                // Apply template description if it has one
                if (template.description != null && !template.description.isBlank()) {
                    summary.setText(template.description);
                }
                
                // Replace selected components with template components
                try {
                    // Clear current selections
                    selectedComponentMap.clear();
                    selectedComponentsList.clear();
                    
                    // Add each component from template to selected list
                    for (String componentId : template.componentIds) {
                        String componentName = ComponentRepository.getNameById(componentId);
                        if (componentName != null) {
                            selectedComponentMap.put(componentName, componentId);
                            selectedComponentsList.add(componentName);
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        // Layout for component selection - two columns side by side
        VBox availableColumn = new VBox(5);
        availableColumn.setPadding(new Insets(5));
        availableColumn.getChildren().addAll(
            new Label("Available Components:"),
            categoryBox,
            availableComponents,
            addComponentToIncident
        );
        
        VBox selectedColumn = new VBox(5);
        selectedColumn.setPadding(new Insets(5));
        selectedColumn.getChildren().addAll(
            new Label("Selected Components:"),
            selectedComponentsView,
            removeComponentFromIncident,
            clearAllComponents
        );
        
        HBox componentSelectionRow = new HBox(10, availableColumn, selectedColumn);

        // Main dialog content layout
        VBox root = new VBox(10,
                templateBox,
                new Separator(),
                new Label("Incident Description:"),
                summary,
                new Label("Severity:"),
                severity,
                new Separator(),
                new Label("Component Selection:"),
                componentSelectionRow
        );
        root.setPadding(new Insets(10));

        getDialogPane().setContent(root);

        // Dialog buttons
        ButtonType save = new ButtonType("Save Changes", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);

        // Handle save button - update incident and component links
        setResultConverter(btn -> {
            if (btn == save) {
                try {
                    // Validate that at least one component is selected
                    if (selectedComponentMap.isEmpty()) {
                        Alert alert = new Alert(Alert.AlertType.WARNING);
                        alert.setTitle("No Components");
                        alert.setHeaderText("Please select at least one component");
                        alert.showAndWait();
                        return null;
                    }

                    // Update incident summary and severity in database
                    IncidentRepository.update(
                        incident.incidentId,
                        summary.getText(),
                        severity.getValue()
                    );
                    
                    // Get all existing component links for this incident
                    List<String> existingComponentIds = IncidentComponentRepository
                        .getComponentIdsForIncident(incident.incidentId);
                    
                    // Remove all existing component links
                    for (String componentId : existingComponentIds) {
                        IncidentComponentRepository.removeComponentFromIncident(
                            incident.incidentId,
                            componentId
                        );
                    }

                    // Add new component links based on current selection
                    for (String componentId : selectedComponentMap.values()) {
                        IncidentComponentRepository.addComponentToIncident(
                                incident.incidentId,
                                componentId
                        );
                    }

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            return null;
        });
    }
}