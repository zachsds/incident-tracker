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

public class UnitDetailPage extends VBox {

    public UnitDetailPage(Unit unit) {

        setPadding(new Insets(20));
        setSpacing(10);

        Label title = new Label("Unit Details");

        Label unitInfo = new Label(
                unit.unitType + " : " +
                (unit.unitType.equals("STOCK") ? unit.stockNumber : unit.title)
        );

        // Create TableView with columns
        TableView<Incident> incidentTable = new TableView<>();
        incidentTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Date column
        TableColumn<Incident, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().reportedAt != null ? data.getValue().reportedAt.substring(0, 10) : ""
        ));
        dateCol.setPrefWidth(100);

        // Severity column
        TableColumn<Incident, String> severityCol = new TableColumn<>("Severity");
        severityCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().severity));
        severityCol.setPrefWidth(80);
        
        // Color code severity cells
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

        // Summary column
        TableColumn<Incident, String> summaryCol = new TableColumn<>("Summary");
        summaryCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().summary));
        summaryCol.setPrefWidth(250);

        // Components column
        TableColumn<Incident, String> componentsCol = new TableColumn<>("Components");
        componentsCol.setCellValueFactory(data -> {
            try {
                List<String> componentIds = IncidentComponentRepository.getComponentIdsForIncident(
                    data.getValue().incidentId
                );
                List<String> componentNames = new ArrayList<>();
                for (String id : componentIds) {
                    String name = ComponentRepository.getNameById(id);
                    if (name != null) {
                        componentNames.add(name);
                    }
                }
                return new SimpleStringProperty(String.join(", ", componentNames));
            } catch (Exception ex) {
                ex.printStackTrace();
                return new SimpleStringProperty("");
            }
        });
        componentsCol.setPrefWidth(200);

        incidentTable.getColumns().addAll(dateCol, severityCol, summaryCol, componentsCol);

        // Load incidents
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

        // Filter functionality
        Runnable applyFilters = () -> {
            try {
                List<Incident> allIncidents = IncidentRepository.getByUnit(unit.unitId);
                List<Incident> filtered = new ArrayList<>();
                
                String searchText = searchField.getText().toLowerCase();
                String severityFilterValue = severityFilter.getValue();

                for (Incident incident : allIncidents) {
                    // Apply severity filter
                    if (!severityFilterValue.equals("All Severities") 
                        && !incident.severity.equals(severityFilterValue)) {
                        continue;
                    }

                    // Apply search filter
                    if (!searchText.isEmpty()) {
                        boolean matches = incident.summary.toLowerCase().contains(searchText);
                        if (!matches) {
                            // Also search in components
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
                        if (!matches) continue;
                    }

                    filtered.add(incident);
                }

                incidentTable.getItems().setAll(filtered);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        };

        searchField.textProperty().addListener((obs, old, newVal) -> applyFilters.run());
        severityFilter.setOnAction(e -> applyFilters.run());

        Button addIncident = new Button("Add Incident");
        addIncident.setOnAction(e -> {
            new AddIncidentDialog(unit.unitId).showAndWait();
            applyFilters.run();
        });

        Button deleteIncident = new Button("Delete Incident");
        deleteIncident.setDisable(true);

        incidentTable.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            deleteIncident.setDisable(n == null);
        });

        deleteIncident.setOnAction(e -> {
            Incident selected = incidentTable.getSelectionModel().getSelectedItem();
            if (selected == null) return;

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Delete Incident");
            confirm.setHeaderText("Delete selected incident?");
            confirm.setContentText(selected.summary);

            if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                try {
                    IncidentRepository.delete(selected.incidentId);
                    applyFilters.run();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        HBox actions = new HBox(10, addIncident, deleteIncident);

        Button back = new Button("Back");
        back.setOnAction(e -> Navigator.show(new ViewUnitsPage()));

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