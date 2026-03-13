package com.sdsweather.ui;

import com.sdsweather.model.Incident;
import com.sdsweather.model.Unit;
import com.sdsweather.navigation.Navigator;
import com.sdsweather.repository.IncidentRepository;
import com.sdsweather.repository.IncidentComponentRepository;
import com.sdsweather.repository.ComponentRepository;
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
 * AnalyticsPage - Comprehensive analytics dashboard for incident trends and patterns.
 *
 * Provides visual analysis of component failures, unit performance, and incident
 * patterns over a configurable date range. Helps identify problematic components
 * and units requiring preventive maintenance.
 *
 * Features:
 *   - Date range filtering (default: last 3 months)
 *   - Summary statistics (total, HIGH, MEDIUM, LOW counts)
 *   - Component failure frequency ranking table with corrected percentages
 *   - Most problematic units table (top 10)
 *   - Component co-occurrence analysis (which parts fail together)
 *   - Auto-generated key insights
 *   - PDF report export with file chooser dialog
 *
 * Percentage Calculation:
 *   Component failure percentages are calculated as:
 *   (individual component failures / total component failures across all incidents) * 100
 *   This ensures percentages sum to 100% across all components.
 *
 * @author Zachary Sneed
 * @version 1.0
 * @since 2026-02-16
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

        // Summary statistics labels
        Label totalIncidentsLabel = new Label("Total Incidents: -");
        Label highSeverityLabel = new Label("HIGH Severity: -");
        highSeverityLabel.setTextFill(Color.RED);
        Label mediumSeverityLabel = new Label("MEDIUM Severity: -");
        mediumSeverityLabel.setTextFill(Color.ORANGE);
        Label lowSeverityLabel = new Label("LOW Severity: -");

        // Grid layout for summary stats
        GridPane statsGrid = new GridPane();
        statsGrid.setHgap(20);
        statsGrid.setVgap(10);
        statsGrid.add(totalIncidentsLabel, 0, 0);
        statsGrid.add(highSeverityLabel, 1, 0);
        statsGrid.add(mediumSeverityLabel, 2, 0);
        statsGrid.add(lowSeverityLabel, 3, 0);

        // Component ranking table section title
        Label componentRankingTitle = new Label("Component Failure Frequency");
        componentRankingTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        // Component ranking table setup
        TableView<ComponentFailureData> componentTable = new TableView<>();
        componentTable.setPrefHeight(300);
        componentTable.setMaxWidth(Double.MAX_VALUE);

        // Rank column
        TableColumn<ComponentFailureData, String> rankCol = new TableColumn<>("#");
        rankCol.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty(String.valueOf(data.getValue().rank)));
        rankCol.setPrefWidth(60);

        // Component name column
        TableColumn<ComponentFailureData, String> componentCol = new TableColumn<>("Component");
        componentCol.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty(data.getValue().componentName));
        componentCol.setPrefWidth(400);

        // Failure count column
        TableColumn<ComponentFailureData, String> failureCountCol = new TableColumn<>("Failures");
        failureCountCol.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty(String.valueOf(data.getValue().failureCount)));
        failureCountCol.setPrefWidth(100);

        // Percentage column - now correctly calculated
        TableColumn<ComponentFailureData, String> percentCol = new TableColumn<>("% of Total");
        percentCol.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty(
                String.format("%.1f%%", data.getValue().percentOfTotal)));
        percentCol.setPrefWidth(120);

        componentTable.getColumns().addAll(rankCol, componentCol, failureCountCol, percentCol);

        // Units ranking table section title
        Label unitsTitle = new Label("Units with Most Incidents");
        unitsTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        // Units ranking table setup
        TableView<UnitIncidentData> unitsTable = new TableView<>();
        unitsTable.setPrefHeight(250);
        unitsTable.setMaxWidth(Double.MAX_VALUE);

        // Unit rank column
        TableColumn<UnitIncidentData, String> unitRankCol = new TableColumn<>("#");
        unitRankCol.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty(String.valueOf(data.getValue().rank)));
        unitRankCol.setPrefWidth(60);

        // Unit name column
        TableColumn<UnitIncidentData, String> unitNameCol = new TableColumn<>("Unit");
        unitNameCol.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty(data.getValue().unitName));
        unitNameCol.setPrefWidth(300);

        // Incident count column
        TableColumn<UnitIncidentData, String> incidentCountCol = new TableColumn<>("Incidents");
        incidentCountCol.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty(String.valueOf(data.getValue().incidentCount)));
        incidentCountCol.setPrefWidth(100);

        // HIGH severity count column
        TableColumn<UnitIncidentData, String> highCountCol = new TableColumn<>("HIGH");
        highCountCol.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty(String.valueOf(data.getValue().highCount)));
        highCountCol.setPrefWidth(80);

        unitsTable.getColumns().addAll(unitRankCol, unitNameCol, incidentCountCol, highCountCol);

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

                // Calculate summary statistics
                int total = allIncidents.size();
                long high = allIncidents.stream().filter(i -> "HIGH".equals(i.severity)).count();
                long medium = allIncidents.stream().filter(i -> "MEDIUM".equals(i.severity)).count();
                long low = allIncidents.stream().filter(i -> "LOW".equals(i.severity)).count();

                // Update summary labels
                totalIncidentsLabel.setText("Total Incidents: " + total);
                highSeverityLabel.setText("HIGH Severity: " + high);
                mediumSeverityLabel.setText("MEDIUM Severity: " + medium);
                lowSeverityLabel.setText("LOW Severity: " + low);

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

                // Build component ranking table data
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
                    // Calculate percentage of total component failures (not total incidents)
                    data.percentOfTotal = totalComponentFailures > 0 
                        ? (entry.getValue() * 100.0 / totalComponentFailures) : 0;
                    componentData.add(data);
                }

                componentTable.getItems().setAll(componentData);

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
                    coOccurrenceText.getText(),
                    insightsText.getText(),
                    totalIncidentsLabel.getText(),
                    highSeverityLabel.getText(),
                    mediumSeverityLabel.getText(),
                    lowSeverityLabel.getText());
                
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
                componentRankingTitle,
                componentTable,
                new Separator(),
                unitsTitle,
                unitsTable,
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

    /**
     * Generates a PDF report containing all analytics data.
     * Creates a formatted PDF with tables, statistics, and insights.
     * 
     * @param filepath Path where PDF will be saved
     * @param startDate Report start date
     * @param endDate Report end date
     * @param componentData Component failure ranking data
     * @param unitData Unit incident ranking data
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
        
        PdfWriter writer = new PdfWriter(new FileOutputStream(filepath));
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);
        
        PdfFont boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        PdfFont normalFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);
        
        // Report title
        Paragraph title = new Paragraph("SDS Weather Analytics Report")
            .setFont(boldFont)
            .setFontSize(20);
        document.add(title);
        
        // Date range
        Paragraph dateRange = new Paragraph("Report Period: " + startDate + " to " + endDate)
            .setFont(normalFont)
            .setFontSize(12);
        document.add(dateRange);
        
        document.add(new Paragraph("\n"));
        
        // Summary statistics section
        Paragraph summaryTitle = new Paragraph("Summary Statistics")
            .setFont(boldFont)
            .setFontSize(16);
        document.add(summaryTitle);
        
        document.add(new Paragraph(totalLabel).setFont(normalFont).setFontSize(12));
        document.add(new Paragraph(highLabel).setFont(normalFont).setFontSize(12));
        document.add(new Paragraph(mediumLabel).setFont(normalFont).setFontSize(12));
        document.add(new Paragraph(lowLabel).setFont(normalFont).setFontSize(12));
        
        document.add(new Paragraph("\n"));
        
        // Component failure frequency table
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
        
        // Units with most incidents table
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
        
        // Component co-occurrence section
        Paragraph coOccurrenceTitle = new Paragraph("Components That Fail Together")
            .setFont(boldFont)
            .setFontSize(16);
        document.add(coOccurrenceTitle);
        
        Paragraph coOccurrence = new Paragraph(coOccurrenceText)
            .setFont(normalFont)
            .setFontSize(10);
        document.add(coOccurrence);
        
        document.add(new Paragraph("\n"));
        
        // Key insights section
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

    /**
     * Data class for component failure statistics.
     * Used in the component ranking table.
     */
    public static class ComponentFailureData {
        int rank;
        String componentName;
        int failureCount;
        double percentOfTotal;
    }

    /**
     * Data class for unit incident statistics.
     * Used in the units ranking table.
     */
    public static class UnitIncidentData {
        int rank;
        String unitName;
        int incidentCount;
        int highCount;
    }
}