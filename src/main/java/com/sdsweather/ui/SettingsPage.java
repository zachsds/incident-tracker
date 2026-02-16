package com.sdsweather.ui;

import com.sdsweather.navigation.Navigator;
import com.sdsweather.repository.ComponentCategoryRepository;
import com.sdsweather.repository.ComponentRepository;
import com.sdsweather.repository.UserRepository;
import com.sdsweather.security.SessionManager;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;

public class SettingsPage extends VBox {

    public SettingsPage() {

        setPadding(new Insets(20));
        setSpacing(15);

        // DEBUG
        System.out.println("SettingsPage - SessionManager.isAdmin(): " + SessionManager.isAdmin());
        System.out.println("SettingsPage - SessionManager.getRole(): " + SessionManager.getRole());

        // USER MANAGEMENT (ADMIN ONLY)
        if (SessionManager.isAdmin()) {
            
            Label userTitle = new Label("User Management");
            userTitle.setFont(Font.font(null, FontWeight.BOLD, 14));

            ComboBox<String> userSelect = new ComboBox<>();
            userSelect.setPromptText("Select a user");

            Runnable refreshUsers = () -> {
                try {
                    userSelect.getItems().clear();
                    List<UserRepository.User> users = UserRepository.getAll();
                    for (UserRepository.User user : users) {
                        String display = user.username + " (" + user.role + ")";
                        if (user.isDisabled) display += " [DISABLED]";
                        userSelect.getItems().add(display);
                        userSelect.getProperties().put(display, user);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            };

            refreshUsers.run();

            TextField newUsername = new TextField();
            newUsername.setPromptText("Username");

            PasswordField newPassword = new PasswordField();
            newPassword.setPromptText("Password");

            ComboBox<String> newUserRole = new ComboBox<>();
            newUserRole.getItems().addAll("USER", "ADMIN");
            newUserRole.setValue("USER");

            Button addUser = new Button("Add User");

            addUser.setOnAction(e -> {
                try {
                    if (newUsername.getText().isBlank() || newPassword.getText().isBlank()) {
                        return;
                    }

                    UserRepository.create(
                            newUsername.getText(),
                            newPassword.getText(),
                            newUserRole.getValue()
                    );

                    newUsername.clear();
                    newPassword.clear();
                    newUserRole.setValue("USER");

                    refreshUsers.run();

                } catch (Exception ex) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText("Failed to create user");
                    alert.setContentText(ex.getMessage());
                    alert.showAndWait();
                }
            });

            Button resetPassword = new Button("Reset Password");
            Button deleteUser = new Button("Delete User");

            resetPassword.setDisable(true);
            deleteUser.setDisable(true);

            userSelect.valueProperty().addListener((obs, o, n) -> {
                boolean hasSelection = n != null;
                resetPassword.setDisable(!hasSelection);
                deleteUser.setDisable(!hasSelection);
            });

            resetPassword.setOnAction(e -> {
                String selected = userSelect.getValue();
                if (selected == null) return;

                UserRepository.User user = (UserRepository.User) userSelect.getProperties().get(selected);

                TextInputDialog dialog = new TextInputDialog();
                dialog.setTitle("Reset Password");
                dialog.setHeaderText("Reset password for " + user.username);
                dialog.setContentText("New password:");

                dialog.showAndWait().ifPresent(newPass -> {
                    try {
                        UserRepository.updatePassword(user.id, newPass);
                        Alert success = new Alert(Alert.AlertType.INFORMATION);
                        success.setTitle("Success");
                        success.setHeaderText("Password reset");
                        success.showAndWait();
                    } catch (Exception ex) {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Error");
                        alert.setContentText(ex.getMessage());
                        alert.showAndWait();
                    }
                });
            });

            deleteUser.setOnAction(e -> {
                String selected = userSelect.getValue();
                if (selected == null) return;

                UserRepository.User user = (UserRepository.User) userSelect.getProperties().get(selected);

                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Confirm Delete");
                confirm.setHeaderText("Delete user?");
                confirm.setContentText(user.username);

                if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

                try {
                    UserRepository.delete(user.id);
                    refreshUsers.run();
                } catch (Exception ex) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setContentText(ex.getMessage());
                    alert.showAndWait();
                }
            });

            HBox addUserRow = new HBox(10, newUsername, newPassword, newUserRole, addUser);
            HBox userButtonRow = new HBox(10, resetPassword, deleteUser);

            getChildren().addAll(
                    userTitle,
                    userSelect,
                    addUserRow,
                    userButtonRow,
                    new Separator()
            );
        }

        Label title = new Label("Component Settings");
        title.setFont(Font.font(null, FontWeight.BOLD, 14));

        TextField newCategoryName = new TextField();
        newCategoryName.setPromptText("New Category Name");

        Button addCategory = new Button("Add Category");

        ComboBox<String> categorySelect = new ComboBox<>();
        categorySelect.setPromptText("Select Category");

        try {
            categorySelect.getItems().addAll(
                    ComponentCategoryRepository.getAllActiveNames()
            );
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        TextField newComponentName = new TextField();
        newComponentName.setPromptText("New Component Name");

        Button addComponent = new Button("Add Component");

        ListView<String> componentList = new ListView<>();
        componentList.setPrefHeight(150);

        categorySelect.setOnAction(e -> {

            componentList.getItems().clear();

            try {
                String categoryId = ComponentCategoryRepository.getIdByName(
                        categorySelect.getValue()
                );

                componentList.getItems().addAll(
                        ComponentRepository.getActiveNamesByCategory(categoryId)
                );

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        addCategory.setOnAction(e -> {
            try {
                if (newCategoryName.getText().isBlank()) return;

                ComponentCategoryRepository.create(newCategoryName.getText());

                categorySelect.getItems().setAll(
                        ComponentCategoryRepository.getAllActiveNames()
                );

                newCategoryName.clear();

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        addComponent.setOnAction(e -> {
            try {

                if (categorySelect.getValue() == null) return;
                if (newComponentName.getText().isBlank()) return;

                String categoryId = ComponentCategoryRepository.getIdByName(
                        categorySelect.getValue()
                );

                ComponentRepository.create(
                        newComponentName.getText(),
                        categoryId
                );

                newComponentName.clear();

                componentList.getItems().setAll(
                        ComponentRepository.getActiveNamesByCategory(categoryId)
                );

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        Button deleteComponent = new Button("Delete Component");
        deleteComponent.setDisable(true);

        componentList.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            deleteComponent.setDisable(n == null);
        });

        deleteComponent.setOnAction(e -> {

            String selected = componentList.getSelectionModel().getSelectedItem();
            if (selected == null) return;

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirm Delete");
            confirm.setHeaderText("Delete Component?");
            confirm.setContentText(selected);

            if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

            try {

                String componentId = ComponentRepository.getIdByName(selected);
                ComponentRepository.delete(componentId);

                componentList.getItems().remove(selected);

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        Button deleteCategory = new Button("Delete Category");
        deleteCategory.setDisable(true);

        categorySelect.valueProperty().addListener((obs, o, n) -> {
            deleteCategory.setDisable(n == null);
        });

        deleteCategory.setOnAction(e -> {

            String selected = categorySelect.getValue();
            if (selected == null) return;

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirm Delete");
            confirm.setHeaderText("Delete Category?");
            confirm.setContentText(selected);

            if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

            try {

                String categoryId = ComponentCategoryRepository.getIdByName(selected);

                List<String> components =
                        ComponentRepository.getActiveNamesByCategory(categoryId);

                if (!components.isEmpty()) {

                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Cannot Delete Category");
                    alert.setHeaderText("Category contains components");
                    alert.setContentText("Delete components first.");
                    alert.showAndWait();
                    return;
                }

                ComponentCategoryRepository.delete(categoryId);

                categorySelect.getItems().remove(selected);
                componentList.getItems().clear();

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        Button back = new Button("Back");
        back.setOnAction(e -> Navigator.show(new LandingPage()));

        HBox categoryRow = new HBox(10, newCategoryName, addCategory);
        HBox componentRow = new HBox(10, categorySelect, newComponentName, addComponent);
        HBox deleteRow = new HBox(10, deleteComponent, deleteCategory);

        // Admin-only audit log button
        VBox bottomSection = new VBox(10);
        if (SessionManager.isAdmin()) {
            Button viewAuditLog = new Button("View Audit Log");
            viewAuditLog.setOnAction(e -> Navigator.show(new AuditLogPage()));
            bottomSection.getChildren().add(viewAuditLog);
        }
        bottomSection.getChildren().add(back);

        getChildren().addAll(
                title,
                new Label("Categories"),
                categoryRow,
                new Label("Components"),
                componentRow,
                componentList,
                deleteRow,
                bottomSection
        );
    }
}