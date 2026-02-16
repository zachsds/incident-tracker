package com.sdsweather.ui;

import com.sdsweather.navigation.Navigator;
import com.sdsweather.repository.AuthRepository;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class LoginPage extends VBox {

    public LoginPage() {

        setPadding(new Insets(20));
        setSpacing(10);
        setMaxWidth(400);
        setAlignment(Pos.TOP_CENTER);

        Label title = new Label("SDS Weather Login");

        TextField username = new TextField();
        username.setPromptText("Username");
        username.setMaxWidth(300);

        // Actual password field (masked)
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        passwordField.setMaxWidth(300);
        
        // Text field to show password as unmasked
        TextField passwordVisible = new TextField();
        passwordVisible.setPromptText("Password");
        passwordVisible.setMaxWidth(300);
        
        // Bind the textField and passwordField text values bidirectionally
        passwordVisible.textProperty().bindBidirectional(passwordField.textProperty());

        // Create checkbox for show/hide password
        CheckBox showPasswordCheckbox = new CheckBox("Show password?");
        showPasswordCheckbox.setStyle("-fx-font-style: italic; -fx-font-size: 11px;");
        
        // Set initial state
        passwordVisible.setManaged(false);
        passwordVisible.setVisible(false);
        
        // Bind visibility properties to checkbox state
        passwordVisible.managedProperty().bind(showPasswordCheckbox.selectedProperty());
        passwordVisible.visibleProperty().bind(showPasswordCheckbox.selectedProperty());
        passwordField.managedProperty().bind(showPasswordCheckbox.selectedProperty().not());
        passwordField.visibleProperty().bind(showPasswordCheckbox.selectedProperty().not());

        // Stack the password fields so they occupy the same space
        StackPane passwordStack = new StackPane();
        passwordStack.getChildren().addAll(passwordField, passwordVisible);
        passwordStack.setAlignment(Pos.CENTER_LEFT);
        passwordStack.setMaxWidth(300);

        Label result = new Label();

        Button login = new Button("Login");
        Button forgot = new Button("Forgot Password");

        // Login action
        Runnable performLogin = () -> {
            try {

                boolean ok = AuthRepository.login(
                        username.getText(),
                        passwordField.getText()
                );

                if (ok) {
                    Navigator.show(new LandingPage());
                } else {
                    result.setText("Invalid login");
                }

            } catch (Exception ex) {
                ex.printStackTrace();
                result.setText("Login error");
            }
        };

        login.setOnAction(e -> performLogin.run());

        // Add Enter key handler to both username and password fields
        username.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                performLogin.run();
            }
        });

        passwordField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                performLogin.run();
            }
        });

        passwordVisible.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                performLogin.run();
            }
        });

        forgot.setOnAction(e ->
                new ForgotPasswordDialog(username.getText()).showAndWait()
        );

        getChildren().addAll(
                title,
                username,
                passwordStack,
                showPasswordCheckbox,
                login,
                forgot,
                result
        );
    }
}