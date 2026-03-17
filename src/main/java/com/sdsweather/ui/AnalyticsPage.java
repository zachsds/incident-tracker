package com.sdsweather.ui;

import com.sdsweather.model.Incident;
import com.sdsweather.model.Unit;
import com.sdsweather.navigation.Navigator;
import com.sdsweather.repository.IncidentRepository;
import com.sdsweather.repository.IncidentComponentRepository;
import com.sdsweather.repository.ComponentRepository;
import com.sdsweather.repository.ComponentReplacementRepository;
import com.sdsweather.repository.UnitRepository;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;

import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.io.font.constants.StandardFonts;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AnalyticsPage - Comprehensive analytics dashboard for incidents, replacements, and trends.
 *
 * Provides visual analysis of component failures, unit performance, replacement costs,
 * and incident patterns over a configurable date range. Helps identify problematic 
 * components and units requiring preventive maintenance.
 *
 * Features:
 *   - Date range filtering (default: last 3 months)
 *   - Summary statistics (incidents + replacements + costs)
 *   - Component failure frequency ranking table
 *   - Component replacement frequency ranking table with cost analysis
 *   - Most problematic units table (top 10)
 *   - Component co-occurrence analysis (which parts fail together)
 *   - Incident-to-replacement resolution rate tracking
 *   - Auto-generated key insights
 *   - PDF report export with all analytics sections
 *
 * @author Zachary Sneed
 * @version 1.0
 * @since 2026-03-16
 */
public class AnalyticsPage extends VBox {

    public AnalyticsPage() {

        setPadding(new Insets(20));
        setSpacing(15);

        VBox content = new VBox(15);
        content.setPadding(new Insets(10));

        // Title with FontAwesome chart icon
        FontAwesomeIconView chartIcon = new FontAwesomeIconView(FontAwesomeIcon.BAR_CHART);
        chartIcon.setGlyphSize(24);
        chartIcon.setFill(javafx.scene.paint.Color.web("#3498db"));
        
        Label title = new Label("Analytics Dashboard");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        title.setGraphic(chartIcon);

        // Date range selectors - default to last 3 months
        DatePicker startDate = new DatePicker(LocalDate.now().minusMonths(3));
        DatePicker endDate = new DatePicker(LocalDate.now());
        Button refresh = new Button("Refresh");

        HBox dateRangeBox = new HBox(10,
                new Label("From:"), startDate,
                new Label("To:"), endDate,
                refresh
        );

        // Summary statistics labels - Row 1: Incidents
        Label totalIncidentsLabel = new Label("Total Incidents: -");
        Label highSeverityLabel = new Label("HIGH Severity: -");
        highSeverityLabel.setTextFill(Color.RED);
        Label mediumSeverityLabel = new Label("MEDIUM Severity: -");
        mediumSeverityLabel.setTextFill(Color.ORANGE);
        Label lowSeverityLabel = new Label("LOW Severity: -");

        // Summary statistics labels - Row 2: Replacements
        Label totalReplacementsLabel = new Label("Total Replacements: -");
        Label totalReplacementCostLabel = new Label("Total Cost: -");
        totalReplacementCostLabel.setTextFill(Color.web("#27ae60"));
        Label avgReplacementCostLabel = new Label("Average Cost: -");
        Label resolutionRateLabel = new Label("Incident Resolution Rate: -");

        // Grid layout for summary stats (2 rows)
        GridPane statsGrid = new GridPane();
        statsGrid.setHgap(20);
        statsGrid.setVgap(10);
        statsGrid.add(totalIncidentsLabel, 0, 0);
        statsGrid.add(highSeverityLabel, 1, 0);
        statsGrid.add(mediumSeverityLabel, 2, 0);
        statsGrid.add(lowSeverityLabel, 3, 0);
        statsGrid.add(totalReplacementsLabel, 0, 1);
        statsGrid.add(totalReplacementCostLabel, 1, 1);
        statsGrid.add(avgReplacementCostLabel, 2, 1);
        statsGrid.add(resolutionRateLabel, 3, 1);

        // Component failure ranking table
        Label componentRankingTitle = new Label("Component Failure Frequency");
        componentRankingTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        TableView<ComponentFailureData> componentTable = new TableView<>();
        componentTable.setPrefHeight(400);

        TableColumn<ComponentFailureData, String> rankCol = new TableColumn<>("#");
        rankCol.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty(String.valueOf(data.getValue().rank)));
        rankCol.setPrefWidth(40);

        TableColumn<ComponentFailureData, String> componentCol = new TableColumn<>("Component");
        componentCol.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty(data.getValue().componentName));
        componentCol.setPrefWidth(200);

        TableColumn<ComponentFailureData, String> failureCountCol = new TableColumn<>("Failures");
        failureCountCol.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty(String.valueOf(data.getValue().failureCount)));
        failureCountCol.setPrefWidth(80);

        TableColumn<ComponentFailureData, String> percentCol = new TableColumn<>("% Total");
        percentCol.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty(
                String.format("%.1f%%", data.getValue().percentOfTotal)));
        percentCol.setPrefWidth(80);

        componentTable.getColumns().addAll(rankCol, componentCol, failureCountCol, percentCol);
        VBox componentBox = new VBox(5, componentRankingTitle, componentTable);
        HBox.setHgrow(componentBox, Priority.ALWAYS);

        // Replacement frequency table
        Label replacementRankingTitle = new Label("Component Replacement Frequency");
        replacementRankingTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        TableView<ComponentReplacementData> replacementTable = new TableView<>();
        replacementTable.setPrefHeight(400);

        TableColumn<ComponentReplacementData, String> replComponentCol = new TableColumn<>("Component");
        replComponentCol.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty(data.getValue().componentName));
        replComponentCol.setPrefWidth(200);

        TableColumn<ComponentReplacementData, String> replCountCol = new TableColumn<>("Count");
        replCountCol.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty(String.valueOf(data.getValue().replacementCount)));
        replCountCol.setPrefWidth(60);

        TableColumn<ComponentReplacementData, String> replPercentCol = new TableColumn<>("% Total");
        replPercentCol.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty(
                String.format("%.1f%%", data.getValue().percentOfTotal)));
        replPercentCol.setPrefWidth(70);

        TableColumn<ComponentReplacementData, String> replCostCol = new TableColumn<>("Avg Cost");
        replCostCol.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty(
                data.getValue().averageCost > 0 ? String.format("$%.2f", data.getValue().averageCost) : "-"));
        replCostCol.setPrefWidth(80);

        replacementTable.getColumns().addAll(replComponentCol, replCountCol, replPercentCol, replCostCol);
        VBox replacementBox = new VBox(5, replacementRankingTitle, replacementTable);
        HBox.setHgrow(replacementBox, Priority.ALWAYS);

        // Units ranking table
        Label unitsTitle = new Label("Units with Most Incidents");
        unitsTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        TableView<UnitIncidentData> unitsTable = new TableView<>();
        unitsTable.setPrefHeight(400);

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
        VBox unitsBox = new VBox(5, unitsTitle, unitsTable);
        HBox.setHgrow(unitsBox, Priority.ALWAYS);

        // Horizontal row containing all three tables side-by-side
        HBox tablesRow = new HBox(15, componentBox, replacementBox, unitsBox);

        // Three text sections displayed side-by-side with equal width
        
        // Key insights text area
        Label insightsTitle = new Label("Key Insights");
        insightsTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        TextArea insightsText = new TextArea();
        insightsText.setEditable(false);
        insightsText.setPrefHeight(300);
        insightsText.setWrapText(true);
        VBox insightsBox = new VBox(5, insightsTitle, insightsText);
        HBox.setHgrow(insightsBox, Priority.ALWAYS);

        // Component co-occurrence text area
        Label coOccurrenceTitle = new Label("Components That Fail Together");
        coOccurrenceTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        TextArea coOccurrenceText = new TextArea();
        coOccurrenceText.setEditable(false);
        coOccurrenceText.setPrefHeight(300);
        coOccurrenceText.setWrapText(true);
        VBox coOccurrenceBox = new VBox(5, coOccurrenceTitle, coOccurrenceText);
        HBox.setHgrow(coOccurrenceBox, Priority.ALWAYS);

        // Top 10 problematic units text area
        Label unitsTextTitle = new Label("Top 10 Problematic Units");
        unitsTextTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        TextArea unitsTextArea = new TextArea();
        unitsTextArea.setEditable(false);
        unitsTextArea.setPrefHeight(300);
        unitsTextArea.setWrapText(true);
        VBox unitsTextBox = new VBox(5, unitsTextTitle, unitsTextArea);
        HBox.setHgrow(unitsTextBox, Priority.ALWAYS);

        // Horizontal row containing all three text sections
        HBox textSectionsRow = new HBox(15, insightsBox, coOccurrenceBox, unitsTextBox);

        // Main analytics loading logic
        Runnable loadAnalytics = () -> {
            try {
                LocalDate start = startDate.getValue();
                LocalDate end = endDate.getValue();

                // Collect all incidents within date range across all units
                List<Incident> allIncidents = new ArrayList<>();
                List<Unit> allUnits = UnitRepository.getAll();
                Map<String, String> unitMap = new HashMap<>();

                for (Unit unit : allUnits) {
                    // Create display name for unit (STOCK units show number, others show title)
                    String display = unit.unitType.equals("STOCK") 
                        ? "STOCK-" + unit.stockNumber 
                        : unit.title;
                    unitMap.put(unit.unitId, display);

                    // Get incidents for this unit and filter by date range
                    List<Incident> unitIncidents = IncidentRepository.getByUnit(unit.unitId);
                    for (Incident incident : unitIncidents) {
                        LocalDate incidentDate = LocalDate.parse(
                            incident.reportedAt.substring(0, 10));
                        if (!incidentDate.isBefore(start) && !incidentDate.isAfter(end)) {
                            allIncidents.add(incident);
                        }
                    }
                }

                // Load all replacement records
                List<ComponentReplacementRepository.ComponentReplacement> allReplacements = 
                    ComponentReplacementRepository.getAll();
                
                // Filter replacements by date range
                List<ComponentReplacementRepository.ComponentReplacement> filteredReplacements = new ArrayList<>();
                for (ComponentReplacementRepository.ComponentReplacement repl : allReplacements) {
                    LocalDate replDate = LocalDate.parse(repl.replacedAt.substring(0, 10));
                    if (!replDate.isBefore(start) && !replDate.isAfter(end)) {
                        filteredReplacements.add(repl);
                    }
                }

                // Calculate incident summary statistics
                int total = allIncidents.size();
                long high = allIncidents.stream().filter(i -> "HIGH".equals(i.severity)).count();
                long medium = allIncidents.stream().filter(i -> "MEDIUM".equals(i.severity)).count();
                long low = allIncidents.stream().filter(i -> "LOW".equals(i.severity)).count();

                // Calculate replacement statistics
                int totalReplacements = filteredReplacements.size();
                double totalCost = filteredReplacements.stream()
                    .filter(r -> r.cost != null)
                    .mapToDouble(r -> r.cost)
                    .sum();
                long replacementsWithCost = filteredReplacements.stream()
                    .filter(r -> r.cost != null)
                    .count();
                double avgCost = replacementsWithCost > 0 ? totalCost / replacementsWithCost : 0;

                // Calculate incident-to-replacement ratio
                long incidentsWithReplacements = allIncidents.stream()
                    .filter(inc -> filteredReplacements.stream()
                        .anyMatch(repl -> inc.incidentId.equals(repl.incidentId)))
                    .count();
                double resolutionRate = total > 0 ? (incidentsWithReplacements * 100.0 / total) : 0;

                // Update summary labels
                totalIncidentsLabel.setText("Total Incidents: " + total);
                highSeverityLabel.setText("HIGH Severity: " + high);
                mediumSeverityLabel.setText("MEDIUM Severity: " + medium);
                lowSeverityLabel.setText("LOW Severity: " + low);
                totalReplacementsLabel.setText("Total Replacements: " + totalReplacements);
                totalReplacementCostLabel.setText(String.format("Total Cost: $%.2f", totalCost));
                avgReplacementCostLabel.setText(String.format("Average Cost: $%.2f", avgCost));
                resolutionRateLabel.setText(String.format("Incident Resolution Rate: %.1f%%", resolutionRate));

                // Count component failures across all incidents
                Map<String, Integer> componentFailures = new HashMap<>();
                Map<String, String> componentIdToName = new HashMap<>();

                for (Incident incident : allIncidents) {
                    List<String> componentIds = IncidentComponentRepository
                        .getComponentIdsForIncident(incident.incidentId);
                    
                    // Increment failure count for each component in this incident
                    for (String componentId : componentIds) {
                        String componentName = ComponentRepository.getNameById(componentId);
                        if (componentName != null) {
                            componentIdToName.put(componentId, componentName);
                            componentFailures.put(componentName, 
                                componentFailures.getOrDefault(componentName, 0) + 1);
                        }
                    }
                }

                // Build component failure ranking table data
                List<ComponentFailureData> componentData = new ArrayList<>();
                int rank = 1;
                
                // Calculate total component failures for percentage calculation
                int totalComponentFailures = componentFailures.values().stream()
                    .mapToInt(Integer::intValue).sum();
                
                for (Map.Entry<String, Integer> entry : componentFailures.entrySet()
                        .stream()
                        .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                        .collect(Collectors.toList())) {
                    
                    ComponentFailureData data = new ComponentFailureData();
                    data.rank = rank++;
                    data.componentName = entry.getKey();
                    data.failureCount = entry.getValue();
                    // Calculate percentage of total component failures
                    data.percentOfTotal = totalComponentFailures > 0 
                        ? (entry.getValue() * 100.0 / totalComponentFailures) : 0;
                    componentData.add(data);
                }

                componentTable.getItems().setAll(componentData);

                // Count replacement frequency per component
                Map<String, Integer> replacementCounts = new HashMap<>();
                Map<String, List<Double>> replacementCosts = new HashMap<>();

                for (ComponentReplacementRepository.ComponentReplacement repl : filteredReplacements) {
                    String componentName = ComponentRepository.getNameById(repl.componentId);
                    if (componentName != null) {
                        replacementCounts.put(componentName, 
                            replacementCounts.getOrDefault(componentName, 0) + 1);
                        
                        if (repl.cost != null) {
                            replacementCosts.putIfAbsent(componentName, new ArrayList<>());
                            replacementCosts.get(componentName).add(repl.cost);
                        }
                    }
                }

                // Build replacement ranking table
                List<ComponentReplacementData> replacementData = new ArrayList<>();
                for (Map.Entry<String, Integer> entry : replacementCounts.entrySet()
                        .stream()
                        .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                        .collect(Collectors.toList())) {
                    
                    ComponentReplacementData data = new ComponentReplacementData();
                    data.componentName = entry.getKey();
                    data.replacementCount = entry.getValue();
                    data.percentOfTotal = totalReplacements > 0 
                        ? (entry.getValue() * 100.0 / totalReplacements) : 0;
                    
                    // Calculate average cost for this component
                    if (replacementCosts.containsKey(entry.getKey())) {
                        List<Double> costs = replacementCosts.get(entry.getKey());
                        data.averageCost = costs.stream().mapToDouble(Double::doubleValue).average().orElse(0);
                    }
                    
                    replacementData.add(data);
                }

                replacementTable.getItems().setAll(replacementData);

                // Count incidents per unit
                Map<String, Integer> unitIncidentCount = new HashMap<>();
                Map<String, Integer> unitHighCount = new HashMap<>();

                for (Incident incident : allIncidents) {
                    unitIncidentCount.put(incident.unitId, 
                        unitIncidentCount.getOrDefault(incident.unitId, 0) + 1);
                    
                    // Track HIGH severity incidents separately
                    if ("HIGH".equals(incident.severity)) {
                        unitHighCount.put(incident.unitId,
                            unitHighCount.getOrDefault(incident.unitId, 0) + 1);
                    }
                }

                // Build units ranking table data (top 10 only)
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

                // Generate text representation of top 10 units
                StringBuilder unitsText = new StringBuilder();
                for (UnitIncidentData data : unitData) {
                    unitsText.append(String.format("#%d - %s\n   %d incidents (%d HIGH)\n\n",
                        data.rank, data.unitName, data.incidentCount, data.highCount));
                }
                if (unitsText.length() == 0) {
                    unitsText.append("No units found in the selected date range.");
                }
                unitsTextArea.setText(unitsText.toString());

                // Analyze which components fail together (co-occurrence)
                Map<String, Integer> pairCounts = new HashMap<>();
                for (Incident incident : allIncidents) {
                    List<String> componentIds = IncidentComponentRepository
                        .getComponentIdsForIncident(incident.incidentId);
                    
                    // Count all pairwise combinations of components in this incident
                    for (int i = 0; i < componentIds.size(); i++) {
                        for (int j = i + 1; j < componentIds.size(); j++) {
                            String comp1 = ComponentRepository.getNameById(componentIds.get(i));
                            String comp2 = ComponentRepository.getNameById(componentIds.get(j));
                            if (comp1 != null && comp2 != null) {
                                // Sort names to ensure "A + B" and "B + A" are treated as same pair
                                String pair = comp1.compareTo(comp2) < 0 
                                    ? comp1 + " + " + comp2 
                                    : comp2 + " + " + comp1;
                                pairCounts.put(pair, pairCounts.getOrDefault(pair, 0) + 1);
                            }
                        }
                    }
                }

                // Build co-occurrence text (top 10 pairs)
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

                // Generate key insights automatically
                StringBuilder insights = new StringBuilder();
                
                // Insight about top failing component
                if (!componentData.isEmpty()) {
                    ComponentFailureData topComponent = componentData.get(0);
                    insights.append(String.format("• %s is the most problematic component with %d failures (%.1f%% of all component failures)\n\n",
                        topComponent.componentName, topComponent.failureCount, topComponent.percentOfTotal));
                }

                // Insight about top 3 components concentration
                if (componentData.size() >= 3) {
                    int top3Total = componentData.stream().limit(3)
                        .mapToInt(d -> d.failureCount).sum();
                    double top3Percent = totalComponentFailures > 0 
                        ? (top3Total * 100.0 / totalComponentFailures) : 0;
                    insights.append(String.format("• Top 3 components account for %.1f%% of all component failures\n\n", top3Percent));
                }

                // Insight about HIGH severity concentration
                if (high > 0) {
                    double highPercent = (high * 100.0 / total);
                    insights.append(String.format("• %.1f%% of incidents are HIGH severity - consider preventive maintenance\n\n", highPercent));
                }

                // Insight about most problematic unit
                if (!unitData.isEmpty()) {
                    UnitIncidentData topUnit = unitData.get(0);
                    insights.append(String.format("• %s has the most incidents (%d total, %d HIGH severity)\n\n",
                        topUnit.unitName, topUnit.incidentCount, topUnit.highCount));
                }

                // Insight about replacement rate
                if (totalReplacements > 0) {
                    insights.append(String.format("• %d component replacements recorded (%.1f%% of incidents resulted in replacements)\n\n",
                        totalReplacements, resolutionRate));
                }

                // Insight about most replaced component
                if (!replacementData.isEmpty()) {
                    ComponentReplacementData topReplacement = replacementData.get(0);
                    insights.append(String.format("• %s is replaced most often (%d times, avg cost: $%.2f)\n\n",
                        topReplacement.componentName, topReplacement.replacementCount, topReplacement.averageCost));
                }

                // Insight about total replacement costs
                if (totalCost > 0) {
                    insights.append(String.format("• Total replacement costs: $%.2f (average $%.2f per replacement)\n\n",
                        totalCost, avgCost));
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

        // Refresh button triggers analytics reload
        refresh.setOnAction(e -> loadAnalytics.run());
        
        // Load analytics on page open
        loadAnalytics.run();

        // PDF export button with file chooser dialog
        Button exportPdf = new Button("Export PDF Report");
        exportPdf.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20;");
        exportPdf.setOnAction(e -> {
            try {
                LocalDate start = startDate.getValue();
                LocalDate end = endDate.getValue();
                
                // Create file chooser dialog
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Save Analytics Report");
                fileChooser.setInitialFileName("Analytics_Report_" + start + "_to_" + end + ".pdf");
                fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("PDF Files", "*.pdf")
                );
                fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
                
                // Show save dialog and get selected file
                File file = fileChooser.showSaveDialog(getScene().getWindow());
                if (file == null) return;  // User cancelled
                
                String filepath = file.getAbsolutePath();
                
                // Generate PDF report at selected location
                generatePdfReport(filepath, start, end, 
                    componentTable.getItems(), 
                    unitsTable.getItems(),
                    replacementTable.getItems(),
                    coOccurrenceText.getText(),
                    insightsText.getText(),
                    totalIncidentsLabel.getText(),
                    highSeverityLabel.getText(),
                    mediumSeverityLabel.getText(),
                    lowSeverityLabel.getText(),
                    totalReplacementsLabel.getText(),
                    totalReplacementCostLabel.getText(),
                    avgReplacementCostLabel.getText(),
                    resolutionRateLabel.getText());
                
                // Show success message
                Alert success = new Alert(Alert.AlertType.INFORMATION);
                success.setTitle("Export Complete");
                success.setHeaderText("PDF report generated successfully");
                success.setContentText("Saved to: " + filepath);
                success.showAndWait();
                
            } catch (Exception ex) {
                ex.printStackTrace();
                Alert error = new Alert(Alert.AlertType.ERROR);
                error.setTitle("Export Failed");
                error.setHeaderText("Failed to generate PDF");
                error.setContentText(ex.getMessage());
                error.showAndWait();
            }
        });

        // Back button returns to main menu
        Button back = new Button("Back");
        back.setOnAction(e -> Navigator.show(new LandingPage()));
        back.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20;");
        HBox buttonRow = new HBox(10, exportPdf, back);

        // Add all components to content container
        content.getChildren().addAll(
                title,
                dateRangeBox,
                new Separator(),
                statsGrid,
                new Separator(),
                tablesRow,
                new Separator(),
                textSectionsRow,
                new Separator(),
                buttonRow
        );

        // Wrap content in scrollpane for overflow handling
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);

        getChildren().add(scrollPane);
    }

    private void generatePdfReport(String filepath, LocalDate startDate, LocalDate endDate,
                                   ObservableList<ComponentFailureData> componentData,
                                   ObservableList<UnitIncidentData> unitData,
                                   ObservableList<ComponentReplacementData> replacementData,
                                   String coOccurrenceText, String insightsText,
                                   String totalLabel, String highLabel, 
                                   String mediumLabel, String lowLabel,
                                   String totalReplLabel, String totalCostLabel,
                                   String avgCostLabel, String resRateLabel) throws Exception {
        
        PdfWriter writer = new PdfWriter(new FileOutputStream(filepath));
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);
        
        PdfFont boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        PdfFont normalFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);
        
        Paragraph title = new Paragraph("SDS Weather Analytics Report")
            .setFont(boldFont)
            .setFontSize(20);
        document.add(title);
        
        Paragraph dateRange = new Paragraph("Report Period: " + startDate + " to " + endDate)
            .setFont(normalFont)
            .setFontSize(12);
        document.add(dateRange);
        
        document.add(new Paragraph("\n"));
        
        Paragraph summaryTitle = new Paragraph("Incident Summary Statistics")
            .setFont(boldFont)
            .setFontSize(16);
        document.add(summaryTitle);
        
        document.add(new Paragraph(totalLabel).setFont(normalFont).setFontSize(12));
        document.add(new Paragraph(highLabel).setFont(normalFont).setFontSize(12));
        document.add(new Paragraph(mediumLabel).setFont(normalFont).setFontSize(12));
        document.add(new Paragraph(lowLabel).setFont(normalFont).setFontSize(12));
        
        document.add(new Paragraph("\n"));
        
        Paragraph replacementSummaryTitle = new Paragraph("Replacement Summary Statistics")
            .setFont(boldFont)
            .setFontSize(16);
        document.add(replacementSummaryTitle);
        
        document.add(new Paragraph(totalReplLabel).setFont(normalFont).setFontSize(12));
        document.add(new Paragraph(totalCostLabel).setFont(normalFont).setFontSize(12));
        document.add(new Paragraph(avgCostLabel).setFont(normalFont).setFontSize(12));
        document.add(new Paragraph(resRateLabel).setFont(normalFont).setFontSize(12));
        
        document.add(new Paragraph("\n"));
        
        Paragraph componentTitle = new Paragraph("Component Failure Frequency")
            .setFont(boldFont)
            .setFontSize(16);
        document.add(componentTitle);
        
        Table componentTable = new Table(new float[]{1, 5, 2, 2});
        componentTable.setWidth(500);
        
        componentTable.addCell(new Cell().add(new Paragraph("#").setFont(boldFont)));
        componentTable.addCell(new Cell().add(new Paragraph("Component").setFont(boldFont)));
        componentTable.addCell(new Cell().add(new Paragraph("Failures").setFont(boldFont)));
        componentTable.addCell(new Cell().add(new Paragraph("% of Total").setFont(boldFont)));
        
        for (ComponentFailureData data : componentData) {
            componentTable.addCell(new Cell().add(new Paragraph(String.valueOf(data.rank)).setFont(normalFont)));
            componentTable.addCell(new Cell().add(new Paragraph(data.componentName).setFont(normalFont)));
            componentTable.addCell(new Cell().add(new Paragraph(String.valueOf(data.failureCount)).setFont(normalFont)));
            componentTable.addCell(new Cell().add(new Paragraph(String.format("%.1f%%", data.percentOfTotal)).setFont(normalFont)));
        }
        
        document.add(componentTable);
        document.add(new Paragraph("\n"));
        
        Paragraph replacementTitle = new Paragraph("Component Replacement History")
            .setFont(boldFont)
            .setFontSize(16);
        document.add(replacementTitle);
        
        Table replacementTable = new Table(new float[]{5, 2, 2, 2});
        replacementTable.setWidth(500);
        
        replacementTable.addCell(new Cell().add(new Paragraph("Component").setFont(boldFont)));
        replacementTable.addCell(new Cell().add(new Paragraph("Replacements").setFont(boldFont)));
        replacementTable.addCell(new Cell().add(new Paragraph("% of Total").setFont(boldFont)));
        replacementTable.addCell(new Cell().add(new Paragraph("Avg Cost").setFont(boldFont)));
        
        for (ComponentReplacementData data : replacementData) {
            replacementTable.addCell(new Cell().add(new Paragraph(data.componentName).setFont(normalFont)));
            replacementTable.addCell(new Cell().add(new Paragraph(String.valueOf(data.replacementCount)).setFont(normalFont)));
            replacementTable.addCell(new Cell().add(new Paragraph(String.format("%.1f%%", data.percentOfTotal)).setFont(normalFont)));
            replacementTable.addCell(new Cell().add(new Paragraph(
                data.averageCost > 0 ? String.format("$%.2f", data.averageCost) : "-").setFont(normalFont)));
        }
        
        document.add(replacementTable);
        document.add(new Paragraph("\n"));
        
        Paragraph unitsTitle = new Paragraph("Units with Most Incidents")
            .setFont(boldFont)
            .setFontSize(16);
        document.add(unitsTitle);
        
        Table unitsTable = new Table(new float[]{1, 5, 2, 2});
        unitsTable.setWidth(500);
        
        unitsTable.addCell(new Cell().add(new Paragraph("#").setFont(boldFont)));
        unitsTable.addCell(new Cell().add(new Paragraph("Unit").setFont(boldFont)));
        unitsTable.addCell(new Cell().add(new Paragraph("Incidents").setFont(boldFont)));
        unitsTable.addCell(new Cell().add(new Paragraph("HIGH").setFont(boldFont)));
        
        for (UnitIncidentData data : unitData) {
            unitsTable.addCell(new Cell().add(new Paragraph(String.valueOf(data.rank)).setFont(normalFont)));
            unitsTable.addCell(new Cell().add(new Paragraph(data.unitName).setFont(normalFont)));
            unitsTable.addCell(new Cell().add(new Paragraph(String.valueOf(data.incidentCount)).setFont(normalFont)));
            unitsTable.addCell(new Cell().add(new Paragraph(String.valueOf(data.highCount)).setFont(normalFont)));
        }
        
        document.add(unitsTable);
        document.add(new Paragraph("\n"));
        
        Paragraph coOccurrenceTitle = new Paragraph("Components That Fail Together")
            .setFont(boldFont)
            .setFontSize(16);
        document.add(coOccurrenceTitle);
        
        Paragraph coOccurrence = new Paragraph(coOccurrenceText)
            .setFont(normalFont)
            .setFontSize(10);
        document.add(coOccurrence);
        
        document.add(new Paragraph("\n"));
        
        Paragraph insightsTitle = new Paragraph("Key Insights")
            .setFont(boldFont)
            .setFontSize(16);
        document.add(insightsTitle);
        
        Paragraph insights = new Paragraph(insightsText)
            .setFont(normalFont)
            .setFontSize(10);
        document.add(insights);
        
        document.close();
    }

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

    public static class ComponentReplacementData {
        String componentName;
        int replacementCount;
        double percentOfTotal;
        double averageCost;
    }
}