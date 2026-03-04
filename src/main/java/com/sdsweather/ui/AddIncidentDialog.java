package com.sdsweather.ui;

import com.sdsweather.repository.ComponentCategoryRepository;
import com.sdsweather.repository.ComponentRepository;
import com.sdsweather.repository.IncidentComponentRepository;
import com.sdsweather.repository.IncidentRepository;
import com.sdsweather.repository.IncidentTemplateRepository;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

/**
 * AddIncidentDialog - Modal dialog for recording a new incident on a unit.
 *
 * Provides two methods for creating incidents:
 *   1. Template-based: Select a pre-configured template to auto-fill fields
 *   2. Manual: Build incident from scratch with custom component selection
 *
 * Component Selection Features:
 *   - Select components from multiple categories (cumulative selection)
 *   - View all selected components in a dedicated list
 *   - Remove individual components from selection
 *   - Two-column interface: Available components | Selected components
 *
 * On save, creates the incident record and all component links via the API.
 *
 * Usage:
 *   new AddIncidentDialog(unit.unitId).showAndWait();
 *
 * @author Zachary Sneed
 * @version 1.0
 * @since 2026-02-16
 */
public class AddIncidentDialog extends Dialog<Void> {

    public AddIncidentDialog(String unitId) {

        setTitle("Add Incident");

        // Template selector at top of dialog
        ComboBox<String> templateBox = new ComboBox<>();
        templateBox.setPromptText("Use Template (optional)");
        templateBox.getItems().add("-- Blank Incident --");
        
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
        
        templateBox.setValue("-- Blank Incident --");

        // Incident summary/description field
        TextField summary = new TextField();
        summary.setPromptText("Incident Description");

        // Severity level dropdown
        ComboBox<String> severity = new ComboBox<>();
        severity.getItems().addAll("LOW","MEDIUM","HIGH");
        severity.setValue("LOW");

        // Category selector for browsing available components
        ComboBox<String> categoryBox = new ComboBox<>();
        categoryBox.setPromptText("Select Category");

        // Load component categories
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
        
        // ListView showing currently selected components with remove capability
        ListView<String> selectedComponentsView = new ListView<>(selectedComponentsList);
        selectedComponentsView.setPrefHeight(150);

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

        // Template selection handler - pre-fills fields when template is chosen
        templateBox.setOnAction(e -> {
            String selected = templateBox.getValue();
            if (selected == null || selected.equals("-- Blank Incident --")) {
                // Reset to blank form
                summary.clear();
                severity.setValue("LOW");
                selectedComponentMap.clear();
                selectedComponentsList.clear();
                return;
            }

            // Retrieve selected template from combobox properties
            IncidentTemplateRepository.Template template = 
                (IncidentTemplateRepository.Template) templateBox.getProperties().get(selected);
            
            if (template != null) {
                // Pre-fill severity from template
                severity.setValue(template.severity);
                
                // Pre-fill description if template has one
                if (template.description != null && !template.description.isBlank()) {
                    summary.setText(template.description);
                }
                
                // Pre-select components from template
                try {
                    // Clear any existing selections
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
            removeComponentFromIncident
        );
        
        HBox componentSelectionRow = new HBox(10, availableColumn, selectedColumn);

        // Main dialog content layout
        VBox root = new VBox(10,
                templateBox,
                new Separator(),
                summary,
                severity,
                new Separator(),
                new Label("Component Selection:"),
                componentSelectionRow
        );
        root.setPadding(new Insets(10));

        getDialogPane().setContent(root);

        // Dialog buttons
        ButtonType save = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);

        // Handle save button - create incident and link components
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

                    // Create the incident and get its generated ID
                    String incidentId = IncidentRepository.createAndReturnId(
                            unitId,
                            summary.getText(),
                            severity.getValue()
                    );

                    // Link each selected component to the incident
                    for (String componentId : selectedComponentMap.values()) {
                        IncidentComponentRepository.addComponentToIncident(
                                incidentId,
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