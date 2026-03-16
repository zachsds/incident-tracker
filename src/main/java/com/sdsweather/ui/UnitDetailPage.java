package com.sdsweather.ui;

import com.sdsweather.model.Incident;
import com.sdsweather.model.Unit;
import com.sdsweather.navigation.Navigator;
import com.sdsweather.repository.IncidentRepository;
import com.sdsweather.repository.IncidentComponentRepository;
import com.sdsweather.repository.ComponentRepository;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.beans.property.SimpleStringProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * UnitDetailPage - Detailed incident history view for a specific unit.
 *
 * Shows unit identification info and a sortable, filterable table of all
 * incidents associated with that unit. Supports adding, editing, and deleting
 * incidents with real-time search and severity filtering.
 *
 * Features:
 *   - Incident table with date, severity, summary, and components columns
 *   - Color-coded severity (RED for HIGH, ORANGE for MEDIUM)
 *   - Real-time text search across summary and component names
 *   - Severity filter dropdown
 *   - Add new incidents
 *   - Edit existing incidents (requires selection)
 *   - Delete incidents with confirmation (requires selection)
 *
 * @author Zachary Sneed
 * @version 1.0
 * @since 2026-02-16
 */
public class UnitDetailPage extends VBox {

    public UnitDetailPage(Unit unit) {

        setPadding(new Insets(20));
        setSpacing(10);

        // Page title
        Label title = new Label("Unit Details");

        // Display unit identification info
        Label unitInfo = new Label(
                unit.unitType + " : " +
                (unit.unitType.equals("STOCK") ? unit.stockNumber : unit.title)
        );

        // Create TableView with columns for incident display
        TableView<Incident> incidentTable = new TableView<>();
        incidentTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        // Date column - shows when incident was reported
        TableColumn<Incident, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().reportedAt != null ? data.getValue().reportedAt.substring(0, 10) : ""
        ));
        dateCol.setPrefWidth(100);

        // Severity column with color coding
        TableColumn<Incident, String> severityCol = new TableColumn<>("Severity");
        severityCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().severity));
        severityCol.setPrefWidth(80);
        
        // Apply color coding to severity cells: RED=HIGH, ORANGE=MEDIUM, BLACK=LOW
        severityCol.setCellFactory(col -> new TableCell<Incident, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    switch (item) {
                        case "HIGH" -> setTextFill(Color.RED);
                        case "MEDIUM" -> setTextFill(Color.ORANGE);
                        default -> setTextFill(Color.BLACK);
                    }
                }
            }
        });

        // Summary column - incident description
        TableColumn<Incident, String> summaryCol = new TableColumn<>("Summary");
        summaryCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().summary));
        summaryCol.setPrefWidth(250);

        // Components column - displays all linked component names
        TableColumn<Incident, String> componentsCol = new TableColumn<>("Components");
        componentsCol.setCellValueFactory(data -> {
            try {
                // Fetch component IDs linked to this incident
                List<String> componentIds = IncidentComponentRepository.getComponentIdsForIncident(
                    data.getValue().incidentId
                );
                
                // Convert component IDs to names
                List<String> componentNames = new ArrayList<>();
                for (String id : componentIds) {
                    String name = ComponentRepository.getNameById(id);
                    if (name != null) {
                        componentNames.add(name);
                    }
                }
                
                // Join names with commas for display
                return new SimpleStringProperty(String.join(", ", componentNames));
            } catch (Exception ex) {
                ex.printStackTrace();
                return new SimpleStringProperty("");
            }
        });
        componentsCol.setPrefWidth(200);

        // Add all columns to table
        incidentTable.getColumns().addAll(dateCol, severityCol, summaryCol, componentsCol);

        // Load all incidents for this unit from database
        try {
            incidentTable.getItems().addAll(
                    IncidentRepository.getByUnit(unit.unitId)
            );
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        // Search/Filter controls
        TextField searchField = new TextField();
        searchField.setPromptText("Search incidents...");
        searchField.setPrefWidth(200);

        ComboBox<String> severityFilter = new ComboBox<>();
        severityFilter.getItems().addAll("All Severities", "HIGH", "MEDIUM", "LOW");
        severityFilter.setValue("All Severities");

        HBox filterBox = new HBox(10, searchField, severityFilter);

        // Filter functionality - applies both search and severity filters
        Runnable applyFilters = () -> {
            try {
                // Get all incidents for this unit
                List<Incident> allIncidents = IncidentRepository.getByUnit(unit.unitId);
                List<Incident> filtered = new ArrayList<>();
                
                String searchText = searchField.getText().toLowerCase();
                String severityFilterValue = severityFilter.getValue();

                for (Incident incident : allIncidents) {
                    // Apply severity filter if not "All Severities"
                    if (!severityFilterValue.equals("All Severities") 
                        && !incident.severity.equals(severityFilterValue)) {
                        continue;
                    }

                    // Apply search filter if search text is not empty
                    if (!searchText.isEmpty()) {
                        // Check if summary contains search text
                        boolean matches = incident.summary.toLowerCase().contains(searchText);
                        
                        if (!matches) {
                            // Also search in component names
                            try {
                                List<String> componentIds = IncidentComponentRepository
                                    .getComponentIdsForIncident(incident.incidentId);
                                for (String id : componentIds) {
                                    String name = ComponentRepository.getNameById(id);
                                    if (name != null && name.toLowerCase().contains(searchText)) {
                                        matches = true;
                                        break;
                                    }
                                }
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                        
                        // Skip this incident if no match found
                        if (!matches) continue;
                    }

                    // Incident passed all filters, add to results
                    filtered.add(incident);
                }

                // Update table with filtered results
                incidentTable.getItems().setAll(filtered);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        };

        // Apply filters whenever search text changes
        searchField.textProperty().addListener((obs, old, newVal) -> applyFilters.run());
        
        // Apply filters whenever severity filter changes
        severityFilter.setOnAction(e -> applyFilters.run());

        // Add Incident button - opens dialog to create new incident
        Button addIncident = new Button("Add Incident");
        addIncident.setOnAction(e -> {
            new AddIncidentDialog(unit.unitId).showAndWait();
            applyFilters.run();  // Refresh table after adding
        });

        // Edit Incident button - opens dialog to modify selected incident
        Button editIncident = new Button("Edit Incident");
        editIncident.setDisable(true);  // Disabled until incident is selected

        // Delete Incident button - deletes selected incident with confirmation
        Button deleteIncident = new Button("Delete Incident");
        deleteIncident.setDisable(true);  // Disabled until incident is selected

        // Enable/disable Edit and Delete buttons based on table selection
        incidentTable.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            boolean hasSelection = n != null;
            editIncident.setDisable(!hasSelection);
            deleteIncident.setDisable(!hasSelection);
        });

        // Handle Edit button - open edit dialog for selected incident
        editIncident.setOnAction(e -> {
            Incident selected = incidentTable.getSelectionModel().getSelectedItem();
            if (selected == null) return;

            new EditIncidentDialog(selected).showAndWait();
            applyFilters.run();  // Refresh table after editing
        });

        // Handle Delete button - delete selected incident with confirmation
        deleteIncident.setOnAction(e -> {
            Incident selected = incidentTable.getSelectionModel().getSelectedItem();
            if (selected == null) return;

            // Show confirmation dialog
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Delete Incident");
            confirm.setHeaderText("Delete selected incident?");
            confirm.setContentText(selected.summary);

            // Only delete if user confirms
            if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                try {
                    IncidentRepository.delete(selected.incidentId);
                    applyFilters.run();  // Refresh table after deletion
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        // Action buttons row
        HBox actions = new HBox(10, addIncident, editIncident, deleteIncident);

        // Back button - return to units list page
        Button back = new Button("Back");
        back.setOnAction(e -> Navigator.show(new ViewUnitsPage()));

        // Add all UI elements to page
        getChildren().addAll(
                title,
                unitInfo,
                filterBox,
                incidentTable,
                actions,
                back
        );
    }
}