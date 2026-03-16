package com.sdsweather.ui;

import com.sdsweather.model.Incident;
import com.sdsweather.model.Unit;
import com.sdsweather.navigation.Navigator;
import com.sdsweather.repository.ComponentReplacementRepository;
import com.sdsweather.repository.ComponentRepository;
import com.sdsweather.repository.IncidentRepository;
import com.sdsweather.repository.IncidentComponentRepository;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.beans.property.SimpleStringProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * ComponentHistoryPage - Combined timeline view of incidents and component replacements.
 *
 * Displays a chronological timeline showing both when incidents occurred and when
 * components were replaced. This allows tracking the relationship between failures
 * and repairs. Supports adding, editing, and deleting replacement records, as well
 * as creating replacement records directly from incidents via "Mark as Replaced" button.
 *
 * Features:
 *   - Combined table showing incidents (red) and replacements (blue)
 *   - Chronological ordering by date
 *   - Shows linked incidents for replacements
 *   - Add new replacement record
 *   - Edit existing replacement (requires selection)
 *   - Delete replacement (requires selection)
 *   - "Mark as Replaced" creates replacement from selected incident
 *
 * @author Zachary Sneed
 * @version 1.0
 * @since 2026-03-16
 */
public class ComponentHistoryPage extends VBox {

    /**
     * TimelineEntry - Combined data structure for incidents and replacements.
     */
    public static class TimelineEntry {
        public String type;  // "INCIDENT" or "REPLACEMENT"
        public String date;
        public String summary;
        public String severity;  // Only for incidents
        public String componentNames;
        public String replacedBy;  // Only for replacements
        public String cost;  // Only for replacements
        public String linkedIncident;  // Summary of linked incident for replacements
        public Object originalData;  // Store original Incident or ComponentReplacement object
    }

    public ComponentHistoryPage(Unit unit) {

        setPadding(new Insets(20));
        setSpacing(10);

        // Page title
        Label title = new Label("Component History - Incidents & Replacements");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        // Display unit identification info
        Label unitInfo = new Label(
                unit.unitType + " : " +
                (unit.unitType.equals("STOCK") ? unit.stockNumber : unit.title)
        );

        // Create TableView for timeline entries
        TableView<TimelineEntry> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        // Type column - shows INCIDENT or REPLACEMENT
        TableColumn<TimelineEntry, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().type));
        typeCol.setPrefWidth(120);
        
        // Color code the type: RED for incidents, BLUE for replacements
        typeCol.setCellFactory(col -> new TableCell<TimelineEntry, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.equals("INCIDENT")) {
                        setTextFill(Color.RED);
                    } else {
                        setTextFill(Color.BLUE);
                    }
                }
            }
        });

        // Date column
        TableColumn<TimelineEntry, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().date));
        dateCol.setPrefWidth(100);

        // Summary/Description column
        TableColumn<TimelineEntry, String> summaryCol = new TableColumn<>("Description");
        summaryCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().summary));
        summaryCol.setPrefWidth(250);

        // Components column
        TableColumn<TimelineEntry, String> componentsCol = new TableColumn<>("Components");
        componentsCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().componentNames));
        componentsCol.setPrefWidth(200);

        // Replaced By column (only for replacements)
        TableColumn<TimelineEntry, String> replacedByCol = new TableColumn<>("Replaced By");
        replacedByCol.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().replacedBy != null ? data.getValue().replacedBy : ""
        ));
        replacedByCol.setPrefWidth(120);

        // Cost column (only for replacements)
        TableColumn<TimelineEntry, String> costCol = new TableColumn<>("Cost");
        costCol.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().cost != null ? data.getValue().cost : ""
        ));
        costCol.setPrefWidth(80);

        // Linked Incident column (shows which incident triggered this replacement)
        TableColumn<TimelineEntry, String> linkedCol = new TableColumn<>("Linked Incident");
        linkedCol.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().linkedIncident != null ? data.getValue().linkedIncident : ""
        ));
        linkedCol.setPrefWidth(200);

        // Add all columns to table
        table.getColumns().addAll(typeCol, dateCol, summaryCol, componentsCol, replacedByCol, costCol, linkedCol);

        // Runnable to refresh table data from database
        Runnable refreshTable = () -> {
            try {
                table.getItems().clear();
                List<TimelineEntry> entries = new ArrayList<>();

                // Load all incidents for this unit
                List<Incident> incidents = IncidentRepository.getByUnit(unit.unitId);
                for (Incident incident : incidents) {
                    TimelineEntry entry = new TimelineEntry();
                    entry.type = "INCIDENT";
                    entry.date = incident.reportedAt.substring(0, 10);
                    entry.summary = incident.summary;
                    entry.severity = incident.severity;
                    entry.originalData = incident;
                    
                    // Get component names for this incident
                    List<String> componentIds = IncidentComponentRepository.getComponentIdsForIncident(incident.incidentId);
                    List<String> componentNames = new ArrayList<>();
                    for (String id : componentIds) {
                        String name = ComponentRepository.getNameById(id);
                        if (name != null) componentNames.add(name);
                    }
                    entry.componentNames = String.join(", ", componentNames);
                    
                    entries.add(entry);
                }

                // Load all replacements for this unit
                List<ComponentReplacementRepository.ComponentReplacement> replacements = 
                    ComponentReplacementRepository.getByUnit(unit.unitId);
                for (ComponentReplacementRepository.ComponentReplacement replacement : replacements) {
                    TimelineEntry entry = new TimelineEntry();
                    entry.type = "REPLACEMENT";
                    entry.date = replacement.replacedAt.substring(0, 10);
                    entry.originalData = replacement;
                    
                    // Get component name
                    String componentName = ComponentRepository.getNameById(replacement.componentId);
                    entry.componentNames = componentName != null ? componentName : "Unknown";
                    entry.summary = "Replaced " + entry.componentNames;
                    
                    entry.replacedBy = replacement.replacedBy;
                    entry.cost = replacement.cost != null ? String.format("$%.2f", replacement.cost) : null;
                    
                    // Get linked incident summary if exists
                    if (replacement.incidentId != null) {
                        for (Incident inc : incidents) {
                            if (inc.incidentId.equals(replacement.incidentId)) {
                                entry.linkedIncident = inc.reportedAt.substring(0, 10) + " - " + inc.summary;
                                break;
                            }
                        }
                    }
                    
                    entries.add(entry);
                }

                // Sort by date descending (newest first)
                entries.sort((a, b) -> b.date.compareTo(a.date));
                
                table.getItems().addAll(entries);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        };

        // Load timeline on page open
        refreshTable.run();

        // Add Replacement button - opens dialog to create new record
        Button addReplacement = new Button("Add Replacement");
        addReplacement.setOnAction(e -> {
            new AddReplacementDialog(unit.unitId).showAndWait();
            refreshTable.run();  // Refresh table after adding
        });

        // Mark as Replaced button - creates replacement from selected incident
        Button markReplaced = new Button("Mark as Replaced");
        markReplaced.setDisable(true);

        // Edit Replacement button - opens dialog to modify selected replacement
        Button editReplacement = new Button("Edit Replacement");
        editReplacement.setDisable(true);  // Disabled until replacement is selected

        // Delete Replacement button - deletes selected replacement with confirmation
        Button deleteReplacement = new Button("Delete Replacement");
        deleteReplacement.setDisable(true);  // Disabled until replacement is selected

        // Enable/disable buttons based on table selection
        table.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n == null) {
                markReplaced.setDisable(true);
                editReplacement.setDisable(true);
                deleteReplacement.setDisable(true);
            } else if (n.type.equals("INCIDENT")) {
                markReplaced.setDisable(false);
                editReplacement.setDisable(true);
                deleteReplacement.setDisable(true);
            } else {  // REPLACEMENT
                markReplaced.setDisable(true);
                editReplacement.setDisable(false);
                deleteReplacement.setDisable(false);
            }
        });

        // Handle Mark as Replaced - creates replacement record from selected incident
        markReplaced.setOnAction(e -> {
            TimelineEntry selected = table.getSelectionModel().getSelectedItem();
            if (selected == null || !selected.type.equals("INCIDENT")) return;

            Incident incident = (Incident) selected.originalData;
            new MarkAsReplacedDialog(unit.unitId, incident).showAndWait();
            refreshTable.run();  // Refresh table after adding replacement
        });

        // Handle Edit button - open edit dialog for selected replacement
        editReplacement.setOnAction(e -> {
            TimelineEntry selected = table.getSelectionModel().getSelectedItem();
            if (selected == null || !selected.type.equals("REPLACEMENT")) return;

            ComponentReplacementRepository.ComponentReplacement replacement = 
                (ComponentReplacementRepository.ComponentReplacement) selected.originalData;
            new EditReplacementDialog(replacement).showAndWait();
            refreshTable.run();  // Refresh table after editing
        });

        // Handle Delete button - delete selected replacement with confirmation
        deleteReplacement.setOnAction(e -> {
            TimelineEntry selected = table.getSelectionModel().getSelectedItem();
            if (selected == null || !selected.type.equals("REPLACEMENT")) return;

            ComponentReplacementRepository.ComponentReplacement replacement = 
                (ComponentReplacementRepository.ComponentReplacement) selected.originalData;

            // Show confirmation dialog
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Delete Replacement");
            confirm.setHeaderText("Delete this replacement record?");
            confirm.setContentText(selected.componentNames + " - " + selected.date);

            // Only delete if user confirms
            if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                try {
                    ComponentReplacementRepository.delete(replacement.id);
                    refreshTable.run();  // Refresh table after deletion
                } catch (Exception ex) {
                    ex.printStackTrace();
                    Alert error = new Alert(Alert.AlertType.ERROR);
                    error.setTitle("Error");
                    error.setHeaderText("Failed to delete replacement");
                    error.setContentText(ex.getMessage());
                    error.showAndWait();
                }
            }
        });

        // Action buttons row
        HBox actions = new HBox(10, addReplacement, markReplaced, editReplacement, deleteReplacement);

        // Back button - return to unit detail page
        Button back = new Button("Back");
        back.setOnAction(e -> Navigator.show(new UnitDetailPage(unit)));

        // Add all UI elements to page
        getChildren().addAll(
                title,
                unitInfo,
                table,
                actions,
                back
        );
    }
}