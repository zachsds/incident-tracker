package com.sdsweather.ui;

import com.sdsweather.model.Incident;
import com.sdsweather.model.Unit;
import com.sdsweather.navigation.Navigator;
import com.sdsweather.repository.IncidentRepository;
import com.sdsweather.repository.IncidentComponentRepository;
import com.sdsweather.repository.ComponentRepository;
import com.sdsweather.repository.UnitRepository;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.beans.property.SimpleStringProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IncidentHistoryPage extends VBox {

    public IncidentHistoryPage() {

        setPadding(new Insets(20));
        setSpacing(10);

        Label title = new Label("Incident History - All Units");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        // Create unit ID to unit info mapping
        Map<String, String> unitMap = new HashMap<>();
        try {
            List<Unit> allUnits = UnitRepository.getAll();
            for (Unit unit : allUnits) {
                String display = unit.unitType.equals("STOCK") 
                    ? "STOCK-" + unit.stockNumber 
                    : unit.title;
                unitMap.put(unit.unitId, display);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        // Create TableView
        TableView<Incident> incidentTable = new TableView<>();
        incidentTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Checkbox column for selection
        TableColumn<Incident, Boolean> selectCol = new TableColumn<>("");
        selectCol.setPrefWidth(40);
        selectCol.setVisible(false); // Hidden by default
        selectCol.setCellFactory(col -> new TableCell<Incident, Boolean>() {
            private final CheckBox checkBox = new CheckBox();

            {
                checkBox.setOnAction(e -> {
                    Incident incident = getTableRow().getItem();
                    if (incident != null) {
                        if (checkBox.isSelected()) {
                            incidentTable.getSelectionModel().select(incident);
                        } else {
                            incidentTable.getSelectionModel().clearSelection(getIndex());
                        }
                    }
                });
            }

            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Incident incident = getTableRow().getItem();
                    checkBox.setSelected(incident != null && 
                        incidentTable.getSelectionModel().getSelectedItems().contains(incident));
                    setGraphic(checkBox);
                }
            }
        });

        // Unit column
        TableColumn<Incident, String> unitCol = new TableColumn<>("Unit");
        unitCol.setCellValueFactory(data -> new SimpleStringProperty(
            unitMap.getOrDefault(data.getValue().unitId, "Unknown")
        ));
        unitCol.setPrefWidth(120);

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

        incidentTable.getColumns().addAll(selectCol, unitCol, dateCol, severityCol, summaryCol, componentsCol);

        // Enable multiple selection
        incidentTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // Multi-select toggle button
        ToggleButton multiSelectBtn = new ToggleButton("Multi-Select");
        multiSelectBtn.setOnAction(e -> {
            boolean multiSelectMode = multiSelectBtn.isSelected();
            selectCol.setVisible(multiSelectMode);
            if (!multiSelectMode) {
                incidentTable.getSelectionModel().clearSelection();
            }
        });

        // Search/Filter controls
        TextField searchField = new TextField();
        searchField.setPromptText("Search incidents...");
        searchField.setPrefWidth(200);

        ComboBox<String> severityFilter = new ComboBox<>();
        severityFilter.getItems().addAll("All Severities", "HIGH", "MEDIUM", "LOW");
        severityFilter.setValue("All Severities");

        ComboBox<String> unitFilter = new ComboBox<>();
        unitFilter.getItems().add("All Units");
        unitFilter.getItems().addAll(unitMap.values());
        unitFilter.setValue("All Units");

        HBox filterBox = new HBox(10, searchField, severityFilter, unitFilter);

        // Load all incidents from all units
        List<Incident> allIncidents = new ArrayList<>();
        Runnable loadAllIncidents = () -> {
            allIncidents.clear();
            try {
                List<Unit> allUnits = UnitRepository.getAll();
                for (Unit unit : allUnits) {
                    List<Incident> unitIncidents = IncidentRepository.getByUnit(unit.unitId);
                    allIncidents.addAll(unitIncidents);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        };

        loadAllIncidents.run();

        // Filter functionality
        Runnable applyFilters = () -> {
            List<Incident> filtered = new ArrayList<>();
            
            String searchText = searchField.getText().toLowerCase();
            String severityFilterValue = severityFilter.getValue();
            String unitFilterValue = unitFilter.getValue();

            for (Incident incident : allIncidents) {
                // Apply severity filter
                if (!severityFilterValue.equals("All Severities") 
                    && !incident.severity.equals(severityFilterValue)) {
                    continue;
                }

                // Apply unit filter
                if (!unitFilterValue.equals("All Units")) {
                    String incidentUnit = unitMap.getOrDefault(incident.unitId, "Unknown");
                    if (!incidentUnit.equals(unitFilterValue)) {
                        continue;
                    }
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
        };

        // Initial load
        applyFilters.run();

        searchField.textProperty().addListener((obs, old, newVal) -> applyFilters.run());
        severityFilter.setOnAction(e -> applyFilters.run());
        unitFilter.setOnAction(e -> applyFilters.run());

        Button refresh = new Button("Refresh");
        refresh.setOnAction(e -> {
            loadAllIncidents.run();
            applyFilters.run();
        });

        Button deleteIncident = new Button("Delete Selected");
        deleteIncident.setDisable(true);
        deleteIncident.setVisible(false); // Hidden until multi-select mode

        Button selectAll = new Button("Select All");
        selectAll.setVisible(false); // Hidden until multi-select mode
        selectAll.setOnAction(e -> incidentTable.getSelectionModel().selectAll());

        Button deselectAll = new Button("Deselect All");
        deselectAll.setVisible(false); // Hidden until multi-select mode
        deselectAll.setOnAction(e -> incidentTable.getSelectionModel().clearSelection());

        // Update multi-select button to show/hide bulk action buttons
        multiSelectBtn.selectedProperty().addListener((obs, oldVal, newVal) -> {
            deleteIncident.setVisible(newVal);
            selectAll.setVisible(newVal);
            deselectAll.setVisible(newVal);
        });

        incidentTable.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            deleteIncident.setDisable(incidentTable.getSelectionModel().getSelectedItems().isEmpty());
        });

        deleteIncident.setOnAction(e -> {
            List<Incident> selected = new ArrayList<>(incidentTable.getSelectionModel().getSelectedItems());
            if (selected.isEmpty()) return;

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Delete Incidents");
            confirm.setHeaderText("Delete " + selected.size() + " incident(s)?");
            confirm.setContentText("This action cannot be undone.");

            if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                int successCount = 0;
                int failCount = 0;

                for (Incident incident : selected) {
                    try {
                        IncidentRepository.delete(incident.incidentId);
                        successCount++;
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        failCount++;
                    }
                }

                loadAllIncidents.run();
                applyFilters.run();

                Alert result = new Alert(Alert.AlertType.INFORMATION);
                result.setTitle("Bulk Delete Complete");
                result.setHeaderText(successCount + " deleted, " + failCount + " failed");
                result.showAndWait();
            }
        });

        HBox selectionActions = new HBox(10, selectAll, deselectAll);
        HBox actions = new HBox(10, multiSelectBtn, refresh, deleteIncident);

        Button back = new Button("Back");
        back.setOnAction(e -> Navigator.show(new LandingPage()));

        Label statsLabel = new Label();
        incidentTable.getItems().addListener((javafx.collections.ListChangeListener.Change<? extends Incident> c) -> {
            int total = incidentTable.getItems().size();
            long high = incidentTable.getItems().stream().filter(i -> "HIGH".equals(i.severity)).count();
            long medium = incidentTable.getItems().stream().filter(i -> "MEDIUM".equals(i.severity)).count();
            long low = incidentTable.getItems().stream().filter(i -> "LOW".equals(i.severity)).count();
            
            statsLabel.setText(String.format("Showing %d incidents | HIGH: %d | MEDIUM: %d | LOW: %d", 
                total, high, medium, low));
        });
        statsLabel.setText("Loading...");

        getChildren().addAll(
                title,
                filterBox,
                statsLabel,
                incidentTable,
                selectionActions,
                actions,
                back
        );
    }
}