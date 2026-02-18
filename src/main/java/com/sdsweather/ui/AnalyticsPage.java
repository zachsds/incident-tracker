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
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AnalyticsPage - Comprehensive analytics dashboard for incident trends and patterns.
 *
 * Provides visual analysis of component failures, unit performance, and incident
 * patterns over a configurable date range. Helps identify problematic components
 * and units requiring preventive maintenance.
 *
 * Features:
 *   - Date range filtering (default: last 3 months)
 *   - Summary statistics (total, HIGH, MEDIUM, LOW counts)
 *   - Component failure frequency ranking table
 *   - Most problematic units table (top 10)
 *   - Component co-occurrence analysis (which parts fail together)
 *   - Auto-generated key insights
 *
 * @author Zachary Sneed
 * @version 1.0
 * @since 2026-02-16
 */

public class AnalyticsPage extends VBox {

    public AnalyticsPage() {

        setPadding(new Insets(20));
        setSpacing(15);

        // Create a VBox for all content
        VBox content = new VBox(15);
        content.setPadding(new Insets(10));

        Label title = new Label("Analytics Dashboard");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        // Date range selector
        DatePicker startDate = new DatePicker(LocalDate.now().minusMonths(3));
        DatePicker endDate = new DatePicker(LocalDate.now());
        Button refresh = new Button("Refresh");

        HBox dateRangeBox = new HBox(10,
                new Label("From:"), startDate,
                new Label("To:"), endDate,
                refresh
        );

        // Summary stats
        Label totalIncidentsLabel = new Label("Total Incidents: -");
        Label highSeverityLabel = new Label("HIGH Severity: -");
        highSeverityLabel.setTextFill(Color.RED);
        Label mediumSeverityLabel = new Label("MEDIUM Severity: -");
        mediumSeverityLabel.setTextFill(Color.ORANGE);
        Label lowSeverityLabel = new Label("LOW Severity: -");

        GridPane statsGrid = new GridPane();
        statsGrid.setHgap(20);
        statsGrid.setVgap(10);
        statsGrid.add(totalIncidentsLabel, 0, 0);
        statsGrid.add(highSeverityLabel, 1, 0);
        statsGrid.add(mediumSeverityLabel, 2, 0);
        statsGrid.add(lowSeverityLabel, 3, 0);

        // Component failure ranking table
        Label componentRankingTitle = new Label("Component Failure Frequency");
        componentRankingTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        TableView<ComponentFailureData> componentTable = new TableView<>();
        componentTable.setPrefHeight(300);

        TableColumn<ComponentFailureData, String> rankCol = new TableColumn<>("#");
        rankCol.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty(String.valueOf(data.getValue().rank)));
        rankCol.setPrefWidth(40);

        TableColumn<ComponentFailureData, String> componentCol = new TableColumn<>("Component");
        componentCol.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty(data.getValue().componentName));
        componentCol.setPrefWidth(250);

        TableColumn<ComponentFailureData, String> failureCountCol = new TableColumn<>("Failures");
        failureCountCol.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty(String.valueOf(data.getValue().failureCount)));
        failureCountCol.setPrefWidth(80);

        TableColumn<ComponentFailureData, String> percentCol = new TableColumn<>("% of Total");
        percentCol.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty(
                String.format("%.1f%%", data.getValue().percentOfTotal)));
        percentCol.setPrefWidth(100);

        componentTable.getColumns().addAll(rankCol, componentCol, failureCountCol, percentCol);

        // Most problematic units
        Label unitsTitle = new Label("Units with Most Incidents");
        unitsTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        TableView<UnitIncidentData> unitsTable = new TableView<>();
        unitsTable.setPrefHeight(250);

        TableColumn<UnitIncidentData, String> unitRankCol = new TableColumn<>("#");
        unitRankCol.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty(String.valueOf(data.getValue().rank)));
        unitRankCol.setPrefWidth(40);

        TableColumn<UnitIncidentData, String> unitNameCol = new TableColumn<>("Unit");
        unitNameCol.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty(data.getValue().unitName));
        unitNameCol.setPrefWidth(200);

        TableColumn<UnitIncidentData, String> incidentCountCol = new TableColumn<>("Incidents");
        incidentCountCol.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty(String.valueOf(data.getValue().incidentCount)));
        incidentCountCol.setPrefWidth(80);

        TableColumn<UnitIncidentData, String> highCountCol = new TableColumn<>("HIGH");
        highCountCol.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty(String.valueOf(data.getValue().highCount)));
        highCountCol.setPrefWidth(60);

        unitsTable.getColumns().addAll(unitRankCol, unitNameCol, incidentCountCol, highCountCol);

        // Component co-occurrence (which components fail together)
        Label coOccurrenceTitle = new Label("Components That Fail Together");
        coOccurrenceTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        TextArea coOccurrenceText = new TextArea();
        coOccurrenceText.setEditable(false);
        coOccurrenceText.setPrefHeight(150);
        coOccurrenceText.setWrapText(true);

        // Insights section
        Label insightsTitle = new Label("Key Insights");
        insightsTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        TextArea insightsText = new TextArea();
        insightsText.setEditable(false);
        insightsText.setPrefHeight(250);  // Increased height for better readability
        insightsText.setWrapText(true);

        // Load data function
        Runnable loadAnalytics = () -> {
            try {
                LocalDate start = startDate.getValue();
                LocalDate end = endDate.getValue();

                // Collect all incidents in date range
                List<Incident> allIncidents = new ArrayList<>();
                List<Unit> allUnits = UnitRepository.getAll();
                Map<String, String> unitMap = new HashMap<>();

                for (Unit unit : allUnits) {
                    String display = unit.unitType.equals("STOCK") 
                        ? "STOCK-" + unit.stockNumber 
                        : unit.title;
                    unitMap.put(unit.unitId, display);

                    List<Incident> unitIncidents = IncidentRepository.getByUnit(unit.unitId);
                    for (Incident incident : unitIncidents) {
                        LocalDate incidentDate = LocalDate.parse(
                            incident.reportedAt.substring(0, 10));
                        if (!incidentDate.isBefore(start) && !incidentDate.isAfter(end)) {
                            allIncidents.add(incident);
                        }
                    }
                }

                // Calculate summary stats
                int total = allIncidents.size();
                long high = allIncidents.stream().filter(i -> "HIGH".equals(i.severity)).count();
                long medium = allIncidents.stream().filter(i -> "MEDIUM".equals(i.severity)).count();
                long low = allIncidents.stream().filter(i -> "LOW".equals(i.severity)).count();

                totalIncidentsLabel.setText("Total Incidents: " + total);
                highSeverityLabel.setText("HIGH Severity: " + high);
                mediumSeverityLabel.setText("MEDIUM Severity: " + medium);
                lowSeverityLabel.setText("LOW Severity: " + low);

                // Component failure frequency
                Map<String, Integer> componentFailures = new HashMap<>();
                Map<String, String> componentIdToName = new HashMap<>();

                for (Incident incident : allIncidents) {
                    List<String> componentIds = IncidentComponentRepository
                        .getComponentIdsForIncident(incident.incidentId);
                    
                    for (String componentId : componentIds) {
                        String componentName = ComponentRepository.getNameById(componentId);
                        if (componentName != null) {
                            componentIdToName.put(componentId, componentName);
                            componentFailures.put(componentName, 
                                componentFailures.getOrDefault(componentName, 0) + 1);
                        }
                    }
                }

                // Sort and rank components
                List<ComponentFailureData> componentData = new ArrayList<>();
                int rank = 1;
                for (Map.Entry<String, Integer> entry : componentFailures.entrySet()
                        .stream()
                        .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                        .collect(Collectors.toList())) {
                    
                    ComponentFailureData data = new ComponentFailureData();
                    data.rank = rank++;
                    data.componentName = entry.getKey();
                    data.failureCount = entry.getValue();
                    data.percentOfTotal = total > 0 ? (entry.getValue() * 100.0 / total) : 0;
                    componentData.add(data);
                }

                componentTable.getItems().setAll(componentData);

                // Unit incident ranking
                Map<String, Integer> unitIncidentCount = new HashMap<>();
                Map<String, Integer> unitHighCount = new HashMap<>();

                for (Incident incident : allIncidents) {
                    unitIncidentCount.put(incident.unitId, 
                        unitIncidentCount.getOrDefault(incident.unitId, 0) + 1);
                    
                    if ("HIGH".equals(incident.severity)) {
                        unitHighCount.put(incident.unitId,
                            unitHighCount.getOrDefault(incident.unitId, 0) + 1);
                    }
                }

                List<UnitIncidentData> unitData = new ArrayList<>();
                rank = 1;
                for (Map.Entry<String, Integer> entry : unitIncidentCount.entrySet()
                        .stream()
                        .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                        .limit(10)
                        .collect(Collectors.toList())) {
                    
                    UnitIncidentData data = new UnitIncidentData();
                    data.rank = rank++;
                    data.unitName = unitMap.getOrDefault(entry.getKey(), "Unknown");
                    data.incidentCount = entry.getValue();
                    data.highCount = unitHighCount.getOrDefault(entry.getKey(), 0);
                    unitData.add(data);
                }

                unitsTable.getItems().setAll(unitData);

                // Component co-occurrence analysis
                Map<String, Integer> pairCounts = new HashMap<>();
                for (Incident incident : allIncidents) {
                    List<String> componentIds = IncidentComponentRepository
                        .getComponentIdsForIncident(incident.incidentId);
                    
                    for (int i = 0; i < componentIds.size(); i++) {
                        for (int j = i + 1; j < componentIds.size(); j++) {
                            String comp1 = ComponentRepository.getNameById(componentIds.get(i));
                            String comp2 = ComponentRepository.getNameById(componentIds.get(j));
                            if (comp1 != null && comp2 != null) {
                                String pair = comp1.compareTo(comp2) < 0 
                                    ? comp1 + " + " + comp2 
                                    : comp2 + " + " + comp1;
                                pairCounts.put(pair, pairCounts.getOrDefault(pair, 0) + 1);
                            }
                        }
                    }
                }

                StringBuilder coOccurrence = new StringBuilder();
                pairCounts.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(10)
                    .forEach(entry -> {
                        coOccurrence.append(String.format("%s: %d times\n", 
                            entry.getKey(), entry.getValue()));
                    });

                if (coOccurrence.length() == 0) {
                    coOccurrence.append("No component pairs found in the selected date range.");
                }

                coOccurrenceText.setText(coOccurrence.toString());

                // Generate insights
                StringBuilder insights = new StringBuilder();
                
                if (!componentData.isEmpty()) {
                    ComponentFailureData topComponent = componentData.get(0);
                    insights.append(String.format("• %s is the most problematic component with %d failures (%.1f%% of all incidents)\n\n",
                        topComponent.componentName, topComponent.failureCount, topComponent.percentOfTotal));
                }

                if (componentData.size() >= 3) {
                    int top3Total = componentData.stream().limit(3)
                        .mapToInt(d -> d.failureCount).sum();
                    double top3Percent = total > 0 ? (top3Total * 100.0 / total) : 0;
                    insights.append(String.format("• Top 3 components account for %.1f%% of all failures\n\n", top3Percent));
                }

                if (high > 0) {
                    double highPercent = (high * 100.0 / total);
                    insights.append(String.format("• %.1f%% of incidents are HIGH severity - consider preventive maintenance\n\n", highPercent));
                }

                if (!unitData.isEmpty()) {
                    UnitIncidentData topUnit = unitData.get(0);
                    insights.append(String.format("• %s has the most incidents (%d total, %d HIGH severity)\n\n",
                        topUnit.unitName, topUnit.incidentCount, topUnit.highCount));
                }

                if (insights.length() == 0) {
                    insights.append("No significant patterns found in the selected date range.");
                }

                insightsText.setText(insights.toString());

            } catch (Exception ex) {
                ex.printStackTrace();
                Alert error = new Alert(Alert.AlertType.ERROR);
                error.setTitle("Error");
                error.setHeaderText("Failed to load analytics");
                error.setContentText(ex.getMessage());
                error.showAndWait();
            }
        };

        refresh.setOnAction(e -> loadAnalytics.run());

        // Initial load
        loadAnalytics.run();

        Button back = new Button("Back");
        back.setOnAction(e -> Navigator.show(new LandingPage()));

        content.getChildren().addAll(
                title,
                dateRangeBox,
                new Separator(),
                statsGrid,
                new Separator(),
                componentRankingTitle,
                componentTable,
                new Separator(),
                unitsTitle,
                unitsTable,
                new Separator(),
                coOccurrenceTitle,
                coOccurrenceText,
                new Separator(),
                insightsTitle,
                insightsText,
                new Separator(),
                back
        );

        // Wrap content in ScrollPane
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);

        getChildren().add(scrollPane);
    }

    // Helper classes for table data
    public static class ComponentFailureData {
        int rank;
        String componentName;
        int failureCount;
        double percentOfTotal;
    }

    public static class UnitIncidentData {
        int rank;
        String unitName;
        int incidentCount;
        int highCount;
    }
}