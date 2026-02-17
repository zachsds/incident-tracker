package com.sdsweather.ui;

import com.sdsweather.navigation.Navigator;
import com.sdsweather.repository.AuthRepository;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * LoginPage - Authentication entry point for the application.
 *
 * Displays the company logo, a styled login form with username and password
 * fields, show/hide password toggle, and a forgot password link. On successful
 * authentication navigates to the LandingPage.
 *
 * @author Zachary Sneed
 * @version 1.0
 * @since 2026-02-16
 */
public class LoginPage extends VBox {

    public LoginPage() {

        setPadding(new Insets(40));
        setSpacing(20);
        setMaxWidth(450);
        setAlignment(Pos.CENTER);
        setStyle("-fx-background-color: #f5f5f5;");

        // Logo
        try {
            Image logo = new Image(getClass().getResourceAsStream("/logo.png"));
            ImageView logoView = new ImageView(logo);
            logoView.setFitWidth(200);
            logoView.setPreserveRatio(true);
            getChildren().add(logoView);
        } catch (Exception e) {
            // If logo not found, show text title instead
            Label title = new Label("SDS Weather");
            title.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
            getChildren().add(title);
        }

        Label subtitle = new Label("Incident Tracker");
        subtitle.setStyle("-fx-font-size: 16px; -fx-text-fill: #7f8c8d;");

        // Login form container
        VBox formBox = new VBox(15);
        formBox.setMaxWidth(350);
        formBox.setAlignment(Pos.CENTER);
        formBox.setPadding(new Insets(30));
        formBox.setStyle(
            "-fx-background-color: white; " +
            "-fx-background-radius: 10; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);"
        );

        TextField username = new TextField();
        username.setPromptText("Username");
        username.setStyle(
            "-fx-padding: 12; " +
            "-fx-font-size: 14px; " +
            "-fx-background-radius: 5; " +
            "-fx-border-color: #ddd; " +
            "-fx-border-radius: 5;"
        );

        // Password fields
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        passwordField.setStyle(
            "-fx-padding: 12; " +
            "-fx-font-size: 14px; " +
            "-fx-background-radius: 5; " +
            "-fx-border-color: #ddd; " +
            "-fx-border-radius: 5;"
        );
        
        TextField passwordVisible = new TextField();
        passwordVisible.setPromptText("Password");
        passwordVisible.setStyle(
            "-fx-padding: 12; " +
            "-fx-font-size: 14px; " +
            "-fx-background-radius: 5; " +
            "-fx-border-color: #ddd; " +
            "-fx-border-radius: 5;"
        );
        
        passwordVisible.textProperty().bindBidirectional(passwordField.textProperty());

        CheckBox showPasswordCheckbox = new CheckBox("Show password");
        showPasswordCheckbox.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d;");
        
        passwordVisible.setManaged(false);
        passwordVisible.setVisible(false);
        
        passwordVisible.managedProperty().bind(showPasswordCheckbox.selectedProperty());
        passwordVisible.visibleProperty().bind(showPasswordCheckbox.selectedProperty());
        passwordField.managedProperty().bind(showPasswordCheckbox.selectedProperty().not());
        passwordField.visibleProperty().bind(showPasswordCheckbox.selectedProperty().not());

        StackPane passwordStack = new StackPane();
        passwordStack.getChildren().addAll(passwordField, passwordVisible);

        Label result = new Label();
        result.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 13px;");

        Button login = new Button("Login");
        login.setMaxWidth(Double.MAX_VALUE);
        login.setStyle(
            "-fx-background-color: #3498db; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 16px; " +
            "-fx-font-weight: bold; " +
            "-fx-padding: 12; " +
            "-fx-background-radius: 5; " +
            "-fx-cursor: hand;"
        );
        login.setOnMouseEntered(e -> login.setStyle(
            "-fx-background-color: #2980b9; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 16px; " +
            "-fx-font-weight: bold; " +
            "-fx-padding: 12; " +
            "-fx-background-radius: 5; " +
            "-fx-cursor: hand;"
        ));
        login.setOnMouseExited(e -> login.setStyle(
            "-fx-background-color: #3498db; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 16px; " +
            "-fx-font-weight: bold; " +
            "-fx-padding: 12; " +
            "-fx-background-radius: 5; " +
            "-fx-cursor: hand;"
        ));

        Button forgot = new Button("Forgot Password?");
        forgot.setStyle(
            "-fx-background-color: transparent; " +
            "-fx-text-fill: #3498db; " +
            "-fx-font-size: 13px; " +
            "-fx-underline: true; " +
            "-fx-cursor: hand;"
        );

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
                    result.setText("Invalid username or password");
                }

            } catch (Exception ex) {
                ex.printStackTrace();
                result.setText("Connection error - please try again");
            }
        };

        login.setOnAction(e -> performLogin.run());

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

        formBox.getChildren().addAll(
                username,
                passwordStack,
                showPasswordCheckbox,
                result,
                login,
                forgot
        );

        getChildren().addAll(
                subtitle,
                formBox
        );
    }
}