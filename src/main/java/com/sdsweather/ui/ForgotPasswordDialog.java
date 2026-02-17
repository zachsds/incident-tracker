package com.sdsweather.ui;

import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * ForgotPasswordDialog - Dialog for submitting a password reset request.
 *
 * Sends the user's username and details to the server which emails the
 * administrator. An admin then manually resets the password via Settings.
 *
 * API Endpoint: POST https://192.168.0.237:3000/auth/forgot-password
 *
 * @author Zachary Sneed
 * @version 1.0
 * @since 2026-02-16
 */
public class ForgotPasswordDialog extends Dialog<Void> {

    public ForgotPasswordDialog(String maybeUsername) {

        setTitle("Forgot Password");

        TextField username = new TextField();
        username.setPromptText("Username");
        if (maybeUsername != null && !maybeUsername.isBlank()) username.setText(maybeUsername);

        TextArea details = new TextArea();
        details.setPromptText("Details (site, unit, reason, etc)");
        details.setPrefRowCount(6);

        VBox root = new VBox(10, username, details);
        getDialogPane().setContent(root);

        ButtonType send = new ButtonType("Send", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(send, ButtonType.CANCEL);

        setResultConverter(btn -> {
            if (btn == send) {
                try {
                    
                    String json = """
                            {
                              "username": "%s",
                              "details": "%s"
                            }
                            """.formatted(
                                    username.getText().replace("\"", "\\\""),
                                    details.getText().replace("\"", "\\\"").replace("\n", "\\n")
                            );

                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create("https://192.168.0.237:3000/auth/forgot-password"))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(json))
                            .build();

                    HttpResponse<String> response =
                            HttpClient.newHttpClient()
                                    .send(request, HttpResponse.BodyHandlers.ofString());

                    if (response.statusCode() == 200) {
                        Alert success = new Alert(Alert.AlertType.INFORMATION);
                        success.setTitle("Request Sent");
                        success.setHeaderText("Password reset request sent");
                        success.setContentText("An administrator will contact you shortly.");
                        success.showAndWait();
                    } else {
                        Alert error = new Alert(Alert.AlertType.ERROR);
                        error.setTitle("Error");
                        error.setHeaderText("Failed to send request");
                        error.setContentText("Please contact your administrator directly.");
                        error.showAndWait();
                    }

                } catch (Exception ex) {
                    ex.printStackTrace();
                    Alert error = new Alert(Alert.AlertType.ERROR);
                    error.setTitle("Error");
                    error.setHeaderText("Failed to send request");
                    error.setContentText("Please contact your administrator directly.");
                    error.showAndWait();
                }
            }
            return null;
        });
    }
}