package com.sdsweather.ui;

import com.sdsweather.navigation.Navigator;
import com.sdsweather.repository.AuditLogRepository;
import com.sdsweather.security.SessionManager;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.beans.property.SimpleStringProperty;

import java.time.LocalDate;
import java.util.List;

/**
 * AuditLogPage - Admin-only view of the system audit trail.
 *
 * Displays a filterable log of all user actions with date range and action
 * type filters. Access is restricted to administrators. Accessible via
 * Settings → View Audit Log.
 *
 * @author Zachary Sneed
 * @version 1.0
 * @since 2026-02-16
 */
public class AuditLogPage extends VBox {

    public AuditLogPage() {

        setPadding(new Insets(20));
        setSpacing(15);

        // Admin only
        if (!SessionManager.isAdmin()) {
            Label accessDenied = new Label("Access Denied: Admin Only");
            accessDenied.setStyle("-fx-font-size: 16px; -fx-text-fill: red;");
            Button back = new Button("Back");
            back.setOnAction(e -> Navigator.show(new LandingPage()));
            getChildren().addAll(accessDenied, back);
            return;
        }

        Label title = new Label("Audit Log");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        // Date filters
        DatePicker startDate = new DatePicker(LocalDate.now().minusDays(30));
        DatePicker endDate = new DatePicker(LocalDate.now());
        
        ComboBox<String> actionFilter = new ComboBox<>();
        actionFilter.getItems().addAll("All Actions", "CREATE_UNIT", "DELETE_UNIT", 
            "CREATE_INCIDENT", "DELETE_INCIDENT", "CREATE_USER", "DELETE_USER");
        actionFilter.setValue("All Actions");

        Button refresh = new Button("Refresh");

        HBox filterBox = new HBox(10, 
            new Label("From:"), startDate,
            new Label("To:"), endDate,
            actionFilter,
            refresh
        );

        // Audit log table
        TableView<AuditLogRepository.AuditLog> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<AuditLogRepository.AuditLog, String> timestampCol = new TableColumn<>("Timestamp");
        timestampCol.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().timestamp != null ? data.getValue().timestamp.substring(0, 19).replace("T", " ") : ""));
        timestampCol.setPrefWidth(150);

        TableColumn<AuditLogRepository.AuditLog, String> userCol = new TableColumn<>("User");
        userCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().username));
        userCol.setPrefWidth(100);

        TableColumn<AuditLogRepository.AuditLog, String> actionCol = new TableColumn<>("Action");
        actionCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().action));
        actionCol.setPrefWidth(150);

        TableColumn<AuditLogRepository.AuditLog, String> entityTypeCol = new TableColumn<>("Entity Type");
        entityTypeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().entityType));
        entityTypeCol.setPrefWidth(100);

        TableColumn<AuditLogRepository.AuditLog, String> entityIdCol = new TableColumn<>("Entity ID");
        entityIdCol.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().entityId != null ? data.getValue().entityId.substring(0, Math.min(8, data.getValue().entityId.length())) : ""));
        entityIdCol.setPrefWidth(80);

        TableColumn<AuditLogRepository.AuditLog, String> detailsCol = new TableColumn<>("Details");
        detailsCol.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().details != null ? data.getValue().details : ""));
        detailsCol.setPrefWidth(200);

        table.getColumns().addAll(timestampCol, userCol, actionCol, entityTypeCol, entityIdCol, detailsCol);

        Runnable loadLogs = () -> {
            try {
                String start = startDate.getValue().toString() + "T00:00:00Z";
                String end = endDate.getValue().toString() + "T23:59:59Z";

                List<AuditLogRepository.AuditLog> logs = AuditLogRepository.getAll(start, end, 500);

                // Filter by action if needed
                if (!actionFilter.getValue().equals("All Actions")) {
                    String selectedAction = actionFilter.getValue();
                    logs.removeIf(log -> !log.action.equals(selectedAction));
                }

                table.getItems().setAll(logs);

            } catch (Exception ex) {
                ex.printStackTrace();
                Alert error = new Alert(Alert.AlertType.ERROR);
                error.setTitle("Error");
                error.setHeaderText("Failed to load audit logs");
                error.setContentText(ex.getMessage());
                error.showAndWait();
            }
        };

        refresh.setOnAction(e -> loadLogs.run());
        loadLogs.run();

        Button back = new Button("Back");
        back.setOnAction(e -> Navigator.show(new SettingsPage()));

        ScrollPane scrollPane = new ScrollPane(table);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(500);

        getChildren().addAll(
                title,
                filterBox,
                scrollPane,
                back
        );
    }
}