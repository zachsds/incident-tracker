package com.sdsweather.ui;

import com.sdsweather.repository.ComponentCategoryRepository;
import com.sdsweather.repository.ComponentRepository;
import com.sdsweather.repository.IncidentComponentRepository;
import com.sdsweather.repository.IncidentRepository;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

/**
 * AddIncidentDialog - Modal dialog for recording a new incident on a unit.
 *
 * Two-step component selection: choose a category, then pick components from
 * that category (multi-select). On save, creates the incident and all component
 * links via the API.
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

        TextField summary = new TextField();
        summary.setPromptText("Incident Description");

        ComboBox<String> severity = new ComboBox<>();
        severity.getItems().addAll("LOW","MEDIUM","HIGH");
        severity.setValue("LOW");

        ComboBox<String> categoryBox = new ComboBox<>();
        categoryBox.setPromptText("Component Category");

        ListView<String> componentList = new ListView<>();
        componentList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        try {
            categoryBox.getItems().addAll(
                    ComponentCategoryRepository.getAllActiveNames()
            );
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        categoryBox.setOnAction(e -> {
            componentList.getItems().clear();
            try {
                String categoryId = ComponentCategoryRepository.getIdByName(categoryBox.getValue());
                componentList.getItems().addAll(
                        ComponentRepository.getActiveNamesByCategory(categoryId)
                );
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        VBox root = new VBox(10,
                summary,
                severity,
                categoryBox,
                componentList
        );

        getDialogPane().setContent(root);

        ButtonType save = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);

        setResultConverter(btn -> {
            if (btn == save) {
                try {

                    String incidentId = IncidentRepository.createAndReturnId(
                            unitId,
                            summary.getText(),
                            severity.getValue()
                    );

                    for (String componentName : componentList.getSelectionModel().getSelectedItems()) {

                        String componentId = ComponentRepository.getIdByName(componentName);

                        if (componentId != null) {
                            IncidentComponentRepository.addComponentToIncident(
                                    incidentId,
                                    componentId
                            );
                        }
                    }

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            return null;
        });
    }
}
