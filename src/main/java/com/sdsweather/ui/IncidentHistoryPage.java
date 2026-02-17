package com.sdsweather.ui;

import com.sdsweather.model.Incident;
import com.sdsweather.model.Unit;
import com.sdsweather.navigation.Navigator;
import com.sdsweather.repository.IncidentRepository;
import com.sdsweather.repository.IncidentComponentRepository;
import com.sdsweather.repository.ComponentRepository;
import com.sdsweather.repository.UnitRepository;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.beans.property.SimpleStringProperty;

import java.util.*;

/**
 * IncidentHistoryPage - Comprehensive incident management and viewing interface.
 * 
 * Displays all incidents across all units in a sortable, filterable table view.
 * Provides multi-select capabilities for bulk operations and real-time filtering
 * by severity, unit, and search terms.
 * 
 * Features:
 * - Unified view of all incidents across the system
 * - Multi-dimensional filtering (search, severity, unit)
 * - Bulk delete operations with multi-select mode
 * - Real-time statistics display
 * - Color-coded severity indicators
 * - Component association display
 * 
 * @author Zachary Sneed
 * @version 1.0
 * @since 2026-02-16
 */
public class IncidentHistoryPage extends VBox {

    private TableView<Incident> incidentTable;
    private List<Incident> allIncidents;
    private Map<String, String> unitMap;
    private TextField searchField;
    private ComboBox<String> severityFilter;
    private ComboBox<String> unitFilter;
    private Label statsLabel;
    private ToggleButton multiSelectBtn;

    /**
     * Constructs the Incident History page with filtering and management features.
     */
    public IncidentHistoryPage() {

        setStyle(AppStyles.PAGE_BACKGROUND);
        setPadding(new Insets(30));
        setSpacing(20);

        // Initialize data structures
        allIncidents = new ArrayList<>();
        unitMap = new HashMap<>();

        // ===== HEADER =====
        HBox header = createHeader();

        // ===== FILTER BAR =====
        HBox filterBar = createFilterBar();

        // ===== STATS BAR =====
        statsLabel = new Label("Loading...");
        statsLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: " + AppStyles.LIGHT_TEXT + ";");

        // ===== INCIDENT TABLE =====
        VBox tableContainer = createTableContainer();

        // ===== ACTION BAR =====
        HBox actionBar = createActionBar();

        // ===== BACK BUTTON =====
        Button back = new Button("← Back");
        AppStyles.styleSecondaryButton(back);
        back.setOnAction(e -> Navigator.show(new AnalysisPage()));

        getChildren().addAll(header, filterBar, statsLabel, tableContainer, actionBar, back);

        // Load data
        loadAllIncidents();
    }

    /**
     * Creates the page header with logo and title.
     * 
     * @return HBox containing header elements
     */
    private HBox createHeader() {
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);

        try {
            Image logo = new Image(getClass().getResourceAsStream("/logosmall.png"));
            ImageView logoView = new ImageView(logo);
            logoView.setFitHeight(35);
            logoView.setPreserveRatio(true);
            header.getChildren().add(logoView);
        } catch (Exception e) {
            System.out.println("Small logo not found");
        }

        Label title = new Label("Incident History");
        title.setStyle(AppStyles.PAGE_TITLE);

        header.getChildren().add(title);
        return header;
    }

    /**
     * Creates the filter control bar with search and dropdown filters.
     * 
     * @return HBox containing filter controls
     */
    private HBox createFilterBar() {
        HBox filterBar = new HBox(15);
        filterBar.setAlignment(Pos.CENTER_LEFT);

        // Search field
        searchField = new TextField();
        searchField.setPromptText("🔍 Search incidents...");
        searchField.setPrefWidth(300);
        AppStyles.styleTextField(searchField);
        searchField.textProperty().addListener((obs, old, newVal) -> applyFilters());

        // Severity filter
        severityFilter = new ComboBox<>();
        severityFilter.getItems().addAll("All Severities", "HIGH", "MEDIUM", "LOW");
        severityFilter.setValue("All Severities");
        severityFilter.setStyle(AppStyles.TEXT_FIELD);
        severityFilter.setOnAction(e -> applyFilters());

        // Unit filter
        unitFilter = new ComboBox<>();
        unitFilter.getItems().add("All Units");
        unitFilter.setValue("All Units");
        unitFilter.setStyle(AppStyles.TEXT_FIELD);
        unitFilter.setOnAction(e -> applyFilters());

        filterBar.getChildren().addAll(searchField, severityFilter, unitFilter);
        return filterBar;
    }

    /**
     * Creates the main incident table with sortable columns.
     * 
     * @return VBox containing the table
     */
    private VBox createTableContainer() {
        VBox container = new VBox(10);
        VBox.setVgrow(container, Priority.ALWAYS);

        incidentTable = new TableView<>();
        incidentTable.setStyle("-fx-background-color: white; -fx-background-radius: 10;");
        VBox.setVgrow(incidentTable, Priority.ALWAYS);
        incidentTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // Checkbox column (hidden by default)
        TableColumn<Incident, Boolean> selectCol = new TableColumn<>("");
        selectCol.setPrefWidth(50);
        selectCol.setVisible(false);
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
        unitCol.setPrefWidth(150);

        // Date column
        TableColumn<Incident, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().reportedAt != null ? data.getValue().reportedAt.substring(0, 10) : ""
        ));
        dateCol.setPrefWidth(100);

        // Severity column with color coding
        TableColumn<Incident, String> severityCol = new TableColumn<>("Severity");
        severityCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().severity));
        severityCol.setPrefWidth(100);
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
                        case "HIGH" -> setTextFill(Color.web(AppStyles.DANGER_COLOR));
                        case "MEDIUM" -> setTextFill(Color.web(AppStyles.WARNING_COLOR));
                        default -> setTextFill(Color.web(AppStyles.DARK_TEXT));
                    }
                }
            }
        });

        // Summary column
        TableColumn<Incident, String> summaryCol = new TableColumn<>("Summary");
        summaryCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().summary));
        summaryCol.setPrefWidth(300);

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
        componentsCol.setPrefWidth(250);

        incidentTable.getColumns().addAll(selectCol, unitCol, dateCol, severityCol, summaryCol, componentsCol);

        // Store selectCol reference
        incidentTable.getProperties().put("selectCol", selectCol);

        container.getChildren().add(incidentTable);
        return container;
    }

    /**
     * Creates the action button bar.
     * 
     * @return HBox containing action buttons
     */
    private HBox createActionBar() {
        HBox actionBar = new HBox(10);
        actionBar.setAlignment(Pos.CENTER_LEFT);

        // Multi-select toggle
        multiSelectBtn = new ToggleButton("Multi-Select");
        multiSelectBtn.setStyle(
            "-fx-background-color: transparent; " +
            "-fx-text-fill: " + AppStyles.PRIMARY_COLOR + "; " +
            "-fx-font-size: 14px; " +
            "-fx-padding: 10 20; " +
            "-fx-border-color: " + AppStyles.PRIMARY_COLOR + "; " +
            "-fx-border-width: 2; " +
            "-fx-border-radius: 5; " +
            "-fx-background-radius: 5; " +
            "-fx-cursor: hand;"
        );
        multiSelectBtn.setOnAction(e -> {
            TableColumn<Incident, ?> selectCol = (TableColumn<Incident, ?>) incidentTable.getProperties().get("selectCol");
            boolean multiSelectMode = multiSelectBtn.isSelected();
            selectCol.setVisible(multiSelectMode);
            if (!multiSelectMode) {
                incidentTable.getSelectionModel().clearSelection();
            }
        });

        Button refresh = new Button("Refresh");
        AppStyles.stylePrimaryButton(refresh);
        refresh.setOnAction(e -> loadAllIncidents());

        Button deleteSelected = new Button("Delete Selected");
        AppStyles.styleDangerButton(deleteSelected);
        deleteSelected.setDisable(true);

        incidentTable.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            deleteSelected.setDisable(incidentTable.getSelectionModel().getSelectedItems().isEmpty());
        });

        deleteSelected.setOnAction(e -> handleBulkDelete());

        actionBar.getChildren().addAll(multiSelectBtn, refresh, deleteSelected);
        return actionBar;
    }

    /**
     * Loads all incidents from all units in the system.
     */
    private void loadAllIncidents() {
        allIncidents.clear();
        unitMap.clear();
        unitFilter.getItems().clear();
        unitFilter.getItems().add("All Units");
        unitFilter.setValue("All Units");

        try {
            List<Unit> allUnits = UnitRepository.getAll();
            for (Unit unit : allUnits) {
                String display = unit.unitType.equals("STOCK")
                    ? "STOCK-" + unit.stockNumber
                    : unit.title;
                unitMap.put(unit.unitId, display);
                unitFilter.getItems().add(display);

                List<Incident> unitIncidents = IncidentRepository.getByUnit(unit.unitId);
                allIncidents.addAll(unitIncidents);
            }
            applyFilters();
        } catch (Exception ex) {
            ex.printStackTrace();
            Alert error = new Alert(Alert.AlertType.ERROR);
            error.setTitle("Error");
            error.setHeaderText("Failed to load incidents");
            error.setContentText(ex.getMessage());
            error.showAndWait();
        }
    }

    /**
     * Applies all active filters to the incident table.
     */
    private void applyFilters() {
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
            if (!"All Units".equals(unitFilterValue)) {
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
        updateStats();
    }

    /**
     * Updates the statistics label with current filter results.
     */
    private void updateStats() {
        int total = incidentTable.getItems().size();
        long high = incidentTable.getItems().stream().filter(i -> "HIGH".equals(i.severity)).count();
        long medium = incidentTable.getItems().stream().filter(i -> "MEDIUM".equals(i.severity)).count();
        long low = incidentTable.getItems().stream().filter(i -> "LOW".equals(i.severity)).count();

        statsLabel.setText(String.format("Showing %d incidents | HIGH: %d | MEDIUM: %d | LOW: %d",
            total, high, medium, low));
    }

    /**
     * Handles bulk deletion of selected incidents.
     */
    private void handleBulkDelete() {
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

            loadAllIncidents();

            Alert result = new Alert(Alert.AlertType.INFORMATION);
            result.setTitle("Bulk Delete Complete");
            result.setHeaderText(successCount + " deleted, " + failCount + " failed");
            result.showAndWait();
        }
    }
}