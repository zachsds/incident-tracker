package com.sdsweather.ui;

import com.sdsweather.model.Incident;
import com.sdsweather.model.Unit;
import com.sdsweather.navigation.Navigator;
import com.sdsweather.repository.IncidentRepository;
import com.sdsweather.repository.IncidentComponentRepository;
import com.sdsweather.repository.ComponentRepository;
import com.sdsweather.repository.UnitRepository;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;

import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.io.font.constants.StandardFonts;

import java.io.FileOutputStream;
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
 *   - PDF report export functionality
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

        // Export PDF button
        Button exportPdf = new Button("Export PDF Report");
        exportPdf.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20;");
        exportPdf.setOnAction(e -> {
            try {
                // Get current analytics data
                LocalDate start = startDate.getValue();
                LocalDate end = endDate.getValue();
                
                // Generate PDF
                String filename = "Analytics_Report_" + start + "_to_" + end + ".pdf";
                String filepath = System.getProperty("user.home") + "/Desktop/" + filename;
                
                generatePdfReport(filepath, start, end, 
                    componentTable.getItems(), 
                    unitsTable.getItems(),
                    coOccurrenceText.getText(),
                    insightsText.getText(),
                    totalIncidentsLabel.getText(),
                    highSeverityLabel.getText(),
                    mediumSeverityLabel.getText(),
                    lowSeverityLabel.getText());
                
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

        Button back = new Button("Back");
        back.setOnAction(e -> Navigator.show(new LandingPage()));
        back.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20;");
        HBox buttonRow = new HBox(10, exportPdf, back);

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
                buttonRow
        );

        // Wrap content in ScrollPane
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);

        getChildren().add(scrollPane);
    }

    /**
     * Generates a professional PDF report of the analytics data.
     *
     * @param filepath Path where the PDF will be saved
     * @param startDate Start date of the report period
     * @param endDate End date of the report period
     * @param componentData Component failure data table
     * @param unitData Unit incident data table
     * @param coOccurrenceText Component co-occurrence text
     * @param insightsText Key insights text
     * @param totalLabel Total incidents label text
     * @param highLabel HIGH severity label text
     * @param mediumLabel MEDIUM severity label text
     * @param lowLabel LOW severity label text
     * @throws Exception If PDF generation fails
     */
    private void generatePdfReport(String filepath, LocalDate startDate, LocalDate endDate,
                                   ObservableList<ComponentFailureData> componentData,
                                   ObservableList<UnitIncidentData> unitData,
                                   String coOccurrenceText, String insightsText,
                                   String totalLabel, String highLabel, 
                                   String mediumLabel, String lowLabel) throws Exception {
        
        // Initialize PDF writer and document
        PdfWriter writer = new PdfWriter(new FileOutputStream(filepath));
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);
        
        // Set up fonts for headings and body text
        PdfFont boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        PdfFont normalFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);
        
        // Add report title
        Paragraph title = new Paragraph("SDS Weather Analytics Report")
            .setFont(boldFont)
            .setFontSize(20);
        document.add(title);
        
        // Add date range subtitle
        Paragraph dateRange = new Paragraph("Report Period: " + startDate + " to " + endDate)
            .setFont(normalFont)
            .setFontSize(12);
        document.add(dateRange);
        
        document.add(new Paragraph("\n"));
        
        // Summary Statistics section
        Paragraph summaryTitle = new Paragraph("Summary Statistics")
            .setFont(boldFont)
            .setFontSize(16);
        document.add(summaryTitle);
        
        document.add(new Paragraph(totalLabel).setFont(normalFont).setFontSize(12));
        document.add(new Paragraph(highLabel).setFont(normalFont).setFontSize(12));
        document.add(new Paragraph(mediumLabel).setFont(normalFont).setFontSize(12));
        document.add(new Paragraph(lowLabel).setFont(normalFont).setFontSize(12));
        
        document.add(new Paragraph("\n"));
        
        // Component Failure Frequency section
        Paragraph componentTitle = new Paragraph("Component Failure Frequency")
            .setFont(boldFont)
            .setFontSize(16);
        document.add(componentTitle);
        
        // Create table with 4 columns: rank, component name, failures, percentage
        Table componentTable = new Table(new float[]{1, 5, 2, 2});
        componentTable.setWidth(500);
        
        // Add table header row
        componentTable.addCell(new Cell().add(new Paragraph("#").setFont(boldFont)));
        componentTable.addCell(new Cell().add(new Paragraph("Component").setFont(boldFont)));
        componentTable.addCell(new Cell().add(new Paragraph("Failures").setFont(boldFont)));
        componentTable.addCell(new Cell().add(new Paragraph("% of Total").setFont(boldFont)));
        
        // Add data rows from component failure table
        for (ComponentFailureData data : componentData) {
            componentTable.addCell(new Cell().add(new Paragraph(String.valueOf(data.rank)).setFont(normalFont)));
            componentTable.addCell(new Cell().add(new Paragraph(data.componentName).setFont(normalFont)));
            componentTable.addCell(new Cell().add(new Paragraph(String.valueOf(data.failureCount)).setFont(normalFont)));
            componentTable.addCell(new Cell().add(new Paragraph(String.format("%.1f%%", data.percentOfTotal)).setFont(normalFont)));
        }
        
        document.add(componentTable);
        document.add(new Paragraph("\n"));
        
        // Most Problematic Units section
        Paragraph unitsTitle = new Paragraph("Units with Most Incidents")
            .setFont(boldFont)
            .setFontSize(16);
        document.add(unitsTitle);
        
        // Create table with 4 columns: rank, unit name, incidents, high severity count
        Table unitsTable = new Table(new float[]{1, 5, 2, 2});
        unitsTable.setWidth(500);
        
        // Add table header row
        unitsTable.addCell(new Cell().add(new Paragraph("#").setFont(boldFont)));
        unitsTable.addCell(new Cell().add(new Paragraph("Unit").setFont(boldFont)));
        unitsTable.addCell(new Cell().add(new Paragraph("Incidents").setFont(boldFont)));
        unitsTable.addCell(new Cell().add(new Paragraph("HIGH").setFont(boldFont)));
        
        // Add data rows from units table
        for (UnitIncidentData data : unitData) {
            unitsTable.addCell(new Cell().add(new Paragraph(String.valueOf(data.rank)).setFont(normalFont)));
            unitsTable.addCell(new Cell().add(new Paragraph(data.unitName).setFont(normalFont)));
            unitsTable.addCell(new Cell().add(new Paragraph(String.valueOf(data.incidentCount)).setFont(normalFont)));
            unitsTable.addCell(new Cell().add(new Paragraph(String.valueOf(data.highCount)).setFont(normalFont)));
        }
        
        document.add(unitsTable);
        document.add(new Paragraph("\n"));
        
        // Component Co-occurrence section
        Paragraph coOccurrenceTitle = new Paragraph("Components That Fail Together")
            .setFont(boldFont)
            .setFontSize(16);
        document.add(coOccurrenceTitle);
        
        Paragraph coOccurrence = new Paragraph(coOccurrenceText)
            .setFont(normalFont)
            .setFontSize(10);
        document.add(coOccurrence);
        
        document.add(new Paragraph("\n"));
        
        // Key Insights section
        Paragraph insightsTitle = new Paragraph("Key Insights")
            .setFont(boldFont)
            .setFontSize(16);
        document.add(insightsTitle);
        
        Paragraph insights = new Paragraph(insightsText)
            .setFont(normalFont)
            .setFontSize(10);
        document.add(insights);
        
        // Close and save the PDF document
        document.close();
    }

    // Helper classes for table data
    /** Data transfer object for component failure frequency table rows. */
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