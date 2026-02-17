package com.sdsweather.ui;

import com.sdsweather.navigation.Navigator;
import com.sdsweather.repository.UnitRepository;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

/**
 * AddUnitPage - Form for registering new units in the system.
 *
 * Supports both STOCK and DEPLOYED unit types. The form dynamically
 * shows the stock number field for STOCK units or the title field for
 * DEPLOYED units. After saving, offers options to add another or return
 * to the main menu.
 *
 * @author Zachary Sneed
 * @version 1.0
 * @since 2026-02-16
 */
public class AddUnitPage extends VBox {

    public AddUnitPage() {

        setPadding(new Insets(20));
        setSpacing(10);

        ComboBox<String> type = new ComboBox<>();
        type.getItems().addAll("STOCK","DEPLOYED");
        type.setValue("STOCK");

        TextField uuid = new TextField();
        uuid.setPromptText("UUID");

        TextField stock = new TextField();
        stock.setPromptText("Stock Number");

        TextField title = new TextField();
        title.setPromptText("Title");

        title.setVisible(false);
        title.setManaged(false);

        type.setOnAction(e -> {
            boolean isStock = type.getValue().equals("STOCK");
            stock.setVisible(isStock);
            stock.setManaged(isStock);
            title.setVisible(!isStock);
            title.setManaged(!isStock);
        });

        Label result = new Label();

        Button save = new Button("Save");
        Button back = new Button("Back");

        // ===== SAVE WITH CHOICE DIALOG =====
        save.setOnAction(e -> {
            try {

                if (type.getValue().equals("STOCK")) {
                    UnitRepository.createStock(uuid.getText(), stock.getText());
                } else {
                    UnitRepository.createDeployed(uuid.getText(), title.getText());
                }

                // ===== POST SAVE CHOICE =====
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Unit Saved");
                alert.setHeaderText("Unit saved successfully");

                ButtonType addAnother = new ButtonType("Add Another Unit");
                ButtonType mainMenu = new ButtonType("Return To Main Menu");

                alert.getButtonTypes().setAll(addAnother, mainMenu);

                ButtonType choice = alert.showAndWait().orElse(mainMenu);

                if (choice == addAnother) {
                    uuid.clear();
                    stock.clear();
                    title.clear();
                } else {
                    Navigator.show(new LandingPage());
                }

            } catch (Exception ex) {
                result.setText(ex.getMessage());
                ex.printStackTrace();
            }
        });

        back.setOnAction(e -> Navigator.show(new LandingPage()));

        getChildren().addAll(
                type,
                uuid,
                stock,
                title,
                save,
                result,
                back
        );
    }
}
