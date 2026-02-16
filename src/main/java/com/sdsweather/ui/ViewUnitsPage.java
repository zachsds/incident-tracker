package com.sdsweather.ui;

import com.sdsweather.model.Unit;
import com.sdsweather.navigation.Navigator;
import com.sdsweather.repository.UnitRepository;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;

import java.util.List;

public class ViewUnitsPage extends VBox {

    public ViewUnitsPage() {

        setPadding(new Insets(20));
        setSpacing(10);

        TextField search = new TextField();
        search.setPromptText("Search UUID / Title / Stock...");

        ListView<Unit> list = new ListView<>();

        
        list.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Unit unit, boolean empty) {
                super.updateItem(unit, empty);

                if (empty || unit == null) {
                    setText(null);
                    return;
                }

                if ("STOCK".equals(unit.unitType)) {
                    setText("STOCK - " + unit.stockNumber);
                } else {
                    setText("DEPLOYED - " + unit.title);
                }
            }
        });

        list.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                Unit selected = list.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    Navigator.show(new UnitDetailPage(selected));
                }
            }
        });

        try {
            List<Unit> allUnits = UnitRepository.getAll();
            list.getItems().addAll(allUnits);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        Button viewDetails = new Button("View Unit Details");
        Button addIncident = new Button("Add Incident");
        Button deleteUnit = new Button("Delete Unit");

        viewDetails.setDisable(true);
        addIncident.setDisable(true);
        deleteUnit.setDisable(true);

        list.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean hasSelection = newVal != null;
            viewDetails.setDisable(!hasSelection);
            addIncident.setDisable(!hasSelection);
            deleteUnit.setDisable(!hasSelection);
        });

        viewDetails.setOnAction(e -> {
            Unit selected = list.getSelectionModel().getSelectedItem();
            if (selected != null) {
                Navigator.show(new UnitDetailPage(selected));
            }
        });

        addIncident.setOnAction(e -> {
            Unit selected = list.getSelectionModel().getSelectedItem();
            if (selected != null) {
                new AddIncidentDialog(selected.unitId).showAndWait();
            }
        });

        deleteUnit.setOnAction(e -> {
            Unit selected = list.getSelectionModel().getSelectedItem();
            if (selected == null) return;

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Delete Unit");
            confirm.setHeaderText("Delete selected unit?");
            confirm.setContentText(selected.unitId);

            if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                try {
                    UnitRepository.delete(selected.unitId);
                    list.getItems().remove(selected);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        HBox actions = new HBox(10, viewDetails, addIncident, deleteUnit);

        Button back = new Button("Back");
        back.setOnAction(e -> Navigator.show(new LandingPage()));

        getChildren().addAll(
                search,
                list,
                actions,
                back
        );
    }
}
