package com.sdsweather.ui;

import com.sdsweather.model.Unit;
import com.sdsweather.navigation.Navigator;
import com.sdsweather.repository.UnitRepository;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.beans.property.SimpleStringProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewUnitsPage - Displays and manages all units in the system.
 * 
 * This page provides a comprehensive view of all registered units (both STOCK and DEPLOYED)
 * with capabilities for searching, filtering, bulk operations, and individual unit management.
 * 
 * Features:
 * - Real-time search functionality across unit IDs, titles, and stock numbers
 * - TableView with sortable columns
 * - Multi-select mode with checkboxes for bulk operations
 * - Quick actions for viewing details, adding incidents, and deleting units
 * - Responsive layout with header navigation
 * 
 * @author Zachary Sneed
 * @version 1.0
 * @since 2026-02-16
 */
public class ViewUnitsPage extends VBox {

    private TableView<Unit> unitTable;
    private TextField searchField;
    private List<Unit> allUnits;
    private ToggleButton multiSelectBtn;

    /**
     * Constructs the View Units page with table and search functionality.
     * 
     * Initializes the page layout including header, search bar, unit table,
     * and action buttons. Loads all units from the repository on creation.
     */
    public ViewUnitsPage() {

        setStyle(AppStyles.PAGE_BACKGROUND);
        setPadding(new Insets(30));
        setSpacing(20);

        // ===== HEADER =====
        HBox header = createHeader();

        // ===== SEARCH BAR =====
        HBox searchBar = createSearchBar();

        // ===== UNIT TABLE =====
        VBox tableContainer = createTableContainer();

        // ===== ACTION BUTTONS =====
        HBox actionBar = createActionBar();

        // ===== BACK BUTTON =====
        Button back = new Button("← Back to Main Menu");
        AppStyles.styleSecondaryButton(back);
        back.setOnAction(e -> Navigator.show(new LandingPage()));

        getChildren().addAll(header, searchBar, tableContainer, actionBar, back);

        // Load initial data
        loadUnits();
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

        Label title = new Label("View Units");
        title.setStyle(AppStyles.PAGE_TITLE);

        header.getChildren().add(title);
        return header;
    }

    /**
     * Creates the search bar with real-time filtering.
     * 
     * @return HBox containing search field
     */
    private HBox createSearchBar() {
        HBox searchBar = new HBox(10);
        searchBar.setAlignment(Pos.CENTER_LEFT);

        searchField = new TextField();
        searchField.setPromptText("🔍 Search units by ID, title, or stock number...");
        searchField.setPrefWidth(400);
        AppStyles.styleTextField(searchField);

        // Real-time search filtering
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterUnits());

        searchBar.getChildren().add(searchField);
        return searchBar;
    }

    /**
     * Creates the main table container with unit data.
     * 
     * The table displays units with the following columns:
     * - Type (STOCK or DEPLOYED)
     * - Identifier (stock number or title)
     * - Created date
     * 
     * Includes checkbox column for multi-select mode.
     * 
     * @return VBox containing the table
     */
    private VBox createTableContainer() {
        VBox container = new VBox(10);
        VBox.setVgrow(container, Priority.ALWAYS);

        unitTable = new TableView<>();
        unitTable.setStyle("-fx-background-color: white; -fx-background-radius: 10;");
        VBox.setVgrow(unitTable, Priority.ALWAYS);

        // Checkbox column (hidden by default)
        TableColumn<Unit, Boolean> selectCol = new TableColumn<>("");
        selectCol.setPrefWidth(50);
        selectCol.setVisible(false);
        selectCol.setCellFactory(col -> new TableCell<Unit, Boolean>() {
            private final CheckBox checkBox = new CheckBox();

            {
                checkBox.setOnAction(e -> {
                    Unit unit = getTableRow().getItem();
                    if (unit != null) {
                        if (checkBox.isSelected()) {
                            unitTable.getSelectionModel().select(unit);
                        } else {
                            unitTable.getSelectionModel().clearSelection(getIndex());
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
                    Unit unit = getTableRow().getItem();
                    checkBox.setSelected(unit != null &&
                        unitTable.getSelectionModel().getSelectedItems().contains(unit));
                    setGraphic(checkBox);
                }
            }
        });

        // Type column
        TableColumn<Unit, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().unitType));
        typeCol.setPrefWidth(120);

        // Identifier column (stock number or title)
        TableColumn<Unit, String> identifierCol = new TableColumn<>("Identifier");
        identifierCol.setCellValueFactory(data -> {
            Unit unit = data.getValue();
            String display = "STOCK".equals(unit.unitType)
                ? "STOCK-" + unit.stockNumber
                : unit.title;
            return new SimpleStringProperty(display);
        });
        identifierCol.setPrefWidth(300);

        // Created date column
        TableColumn<Unit, String> createdCol = new TableColumn<>("Created");
        createdCol.setCellValueFactory(data -> {
            String date = data.getValue().createdAt;
            return new SimpleStringProperty(date != null ? date.substring(0, 10) : "");
        });
        createdCol.setPrefWidth(120);

        // UUID column
        TableColumn<Unit, String> uuidCol = new TableColumn<>("UUID");
        uuidCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().unitId));
        uuidCol.setPrefWidth(280);

        unitTable.getColumns().addAll(selectCol, typeCol, identifierCol, createdCol, uuidCol);
        unitTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // Double-click to view details
        unitTable.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && !multiSelectBtn.isSelected()) {
                Unit selected = unitTable.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    Navigator.show(new UnitDetailPage(selected));
                }
            }
        });

        // Store reference to selectCol for multi-select toggle
        unitTable.getProperties().put("selectCol", selectCol);

        container.getChildren().add(unitTable);
        return container;
    }

    /**
     * Creates the action button bar with multi-select and operation buttons.
     * 
     * @return HBox containing all action buttons
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
            TableColumn<Unit, ?> selectCol = (TableColumn<Unit, ?>) unitTable.getProperties().get("selectCol");
            boolean multiSelectMode = multiSelectBtn.isSelected();
            selectCol.setVisible(multiSelectMode);
            if (!multiSelectMode) {
                unitTable.getSelectionModel().clearSelection();
            }
        });

        Button viewDetails = new Button("View Details");
        AppStyles.stylePrimaryButton(viewDetails);
        viewDetails.setDisable(true);

        Button addIncident = new Button("Add Incident");
        AppStyles.styleSuccessButton(addIncident);
        addIncident.setDisable(true);

        Button deleteSelected = new Button("Delete Selected");
        AppStyles.styleDangerButton(deleteSelected);
        deleteSelected.setDisable(true);

        // Enable/disable buttons based on selection
        unitTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean hasSelection = !unitTable.getSelectionModel().getSelectedItems().isEmpty();
            boolean singleSelection = unitTable.getSelectionModel().getSelectedItems().size() == 1;
            viewDetails.setDisable(!singleSelection || multiSelectBtn.isSelected());
            addIncident.setDisable(!singleSelection || multiSelectBtn.isSelected());
            deleteSelected.setDisable(!hasSelection);
        });

        // Button actions
        viewDetails.setOnAction(e -> {
            Unit selected = unitTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                Navigator.show(new UnitDetailPage(selected));
            }
        });

        addIncident.setOnAction(e -> {
            Unit selected = unitTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                new AddIncidentDialog(selected.unitId).showAndWait();
            }
        });

        deleteSelected.setOnAction(e -> handleBulkDelete());

        actionBar.getChildren().addAll(multiSelectBtn, viewDetails, addIncident, deleteSelected);
        return actionBar;
    }

    /**
     * Loads all units from the repository.
     * 
     * Fetches units from the API and populates the table. Handles
     * exceptions gracefully with error dialogs.
     */
    private void loadUnits() {
        try {
            allUnits = UnitRepository.getAll();
            unitTable.getItems().setAll(allUnits);
        } catch (Exception ex) {
            ex.printStackTrace();
            Alert error = new Alert(Alert.AlertType.ERROR);
            error.setTitle("Error");
            error.setHeaderText("Failed to load units");
            error.setContentText(ex.getMessage());
            error.showAndWait();
        }
    }

    /**
     * Filters the unit table based on search text.
     * 
     * Searches across unit IDs, titles, and stock numbers in real-time
     * as the user types in the search field.
     */
    private void filterUnits() {
        if (allUnits == null) return;

        String searchText = searchField.getText().toLowerCase();
        if (searchText.isEmpty()) {
            unitTable.getItems().setAll(allUnits);
            return;
        }

        List<Unit> filtered = new ArrayList<>();
        for (Unit unit : allUnits) {
            boolean matches = unit.unitId.toLowerCase().contains(searchText) ||
                            (unit.title != null && unit.title.toLowerCase().contains(searchText)) ||
                            (unit.stockNumber != null && unit.stockNumber.toLowerCase().contains(searchText));
            if (matches) {
                filtered.add(unit);
            }
        }

        unitTable.getItems().setAll(filtered);
    }

    /**
     * Handles bulk deletion of selected units.
     * 
     * Prompts for confirmation before deleting, then removes all selected
     * units and displays a summary of the operation results.
     */
    private void handleBulkDelete() {
        var selected = unitTable.getSelectionModel().getSelectedItems();
        if (selected.isEmpty()) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Units");
        confirm.setHeaderText("Delete " + selected.size() + " unit(s)?");
        confirm.setContentText("This will also delete all incidents for these units. This action cannot be undone.");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            int successCount = 0;
            int failCount = 0;

            for (Unit unit : List.copyOf(selected)) {
                try {
                    UnitRepository.delete(unit.unitId);
                    successCount++;
                } catch (Exception ex) {
                    ex.printStackTrace();
                    failCount++;
                }
            }

            // Reload data
            loadUnits();

            // Show result
            Alert result = new Alert(Alert.AlertType.INFORMATION);
            result.setTitle("Bulk Delete Complete");
            result.setHeaderText(successCount + " deleted, " + failCount + " failed");
            result.showAndWait();
        }
    }
}