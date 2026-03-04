package com.sdsweather.ui;

import com.sdsweather.navigation.Navigator;
import com.sdsweather.repository.ComponentCategoryRepository;
import com.sdsweather.repository.ComponentRepository;
import com.sdsweather.repository.UserRepository;
import com.sdsweather.security.SessionManager;
import com.sdsweather.update.UpdateChecker;
import com.sdsweather.update.UpdateDialog;
import com.sdsweather.repository.IncidentTemplateRepository;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * SettingsPage - System configuration and administration interface.
 *
 * Provides component category and component management for all users.
 * Admin users also get access to user management (add, reset password,
 * delete), incident template management, and the audit log viewer. 
 * Includes application update checking with manual trigger and version display.
 *
 * Template Management Features:
 *   - Select components from multiple categories (cumulative selection)
 *   - View all selected components in a dedicated list
 *   - Remove individual components from selection
 *   - Create templates with pre-configured severity and component sets
 *
 * Admin-only sections are conditionally rendered based on SessionManager.isAdmin().
 *
 * @author Zachary Sneed
 * @version 1.0
 * @since 2026-02-16
 */
public class SettingsPage extends VBox {

    public SettingsPage() {

        // Create ScrollPane for entire page to handle overflow
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        
        // Main content container that will go inside the ScrollPane
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        // DEBUG - verify admin status
        System.out.println("SettingsPage - SessionManager.isAdmin(): " + SessionManager.isAdmin());
        System.out.println("SettingsPage - SessionManager.getRole(): " + SessionManager.getRole());

        // ===== USER MANAGEMENT SECTION (ADMIN ONLY) =====
        if (SessionManager.isAdmin()) {
            
            Label userTitle = new Label("User Management");
            userTitle.setFont(Font.font(null, FontWeight.BOLD, 14));

            // Dropdown to select existing users for password reset or deletion
            ComboBox<String> userSelect = new ComboBox<>();
            userSelect.setPromptText("Select a user");

            // Refresh user list from database
            Runnable refreshUsers = () -> {
                try {
                    userSelect.getItems().clear();
                    List<UserRepository.User> users = UserRepository.getAll();
                    for (UserRepository.User user : users) {
                        String display = user.username + " (" + user.role + ")";
                        if (user.isDisabled) display += " [DISABLED]";
                        userSelect.getItems().add(display);
                        // Store user object in dropdown properties for later retrieval
                        userSelect.getProperties().put(display, user);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            };

            // Initial load of users
            refreshUsers.run();

            // Fields for creating new user
            TextField newUsername = new TextField();
            newUsername.setPromptText("Username");

            PasswordField newPassword = new PasswordField();
            newPassword.setPromptText("Password");

            ComboBox<String> newUserRole = new ComboBox<>();
            newUserRole.getItems().addAll("USER", "ADMIN");
            newUserRole.setValue("USER");

            Button addUser = new Button("Add User");

            // Handle new user creation
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

                    // Clear form fields after successful creation
                    newUsername.clear();
                    newPassword.clear();
                    newUserRole.setValue("USER");

                    // Reload user list to show new user
                    refreshUsers.run();

                } catch (Exception ex) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText("Failed to create user");
                    alert.setContentText(ex.getMessage());
                    alert.showAndWait();
                }
            });

            // Buttons for managing existing users
            Button resetPassword = new Button("Reset Password");
            Button deleteUser = new Button("Delete User");

            // Disable buttons until a user is selected
            resetPassword.setDisable(true);
            deleteUser.setDisable(true);

            // Enable/disable buttons based on selection
            userSelect.valueProperty().addListener((obs, o, n) -> {
                boolean hasSelection = n != null;
                resetPassword.setDisable(!hasSelection);
                deleteUser.setDisable(!hasSelection);
            });

            // Handle password reset
            resetPassword.setOnAction(e -> {
                String selected = userSelect.getValue();
                if (selected == null) return;

                // Retrieve user object from dropdown properties
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

            // Handle user deletion
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

            // Add user management section to content
            content.getChildren().addAll(
                    userTitle,
                    userSelect,
                    addUserRow,
                    userButtonRow,
                    new Separator()
            );
        }

        // ===== COMPONENT SETTINGS SECTION (ALL USERS) =====
        Label title = new Label("Component Settings");
        title.setFont(Font.font(null, FontWeight.BOLD, 14));

        // Field for creating new component category
        TextField newCategoryName = new TextField();
        newCategoryName.setPromptText("New Category Name");

        Button addCategory = new Button("Add Category");

        // Dropdown for selecting category to view its components
        ComboBox<String> categorySelect = new ComboBox<>();
        categorySelect.setPromptText("Select Category");

        // Load existing categories
        try {
            categorySelect.getItems().addAll(
                    ComponentCategoryRepository.getAllActiveNames()
            );
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        // Field for creating new component within selected category
        TextField newComponentName = new TextField();
        newComponentName.setPromptText("New Component Name");

        Button addComponent = new Button("Add Component");

        // List of components in the selected category
        ListView<String> componentList = new ListView<>();
        componentList.setPrefHeight(150);

        // Load components when category is selected
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

        // Handle creating new category
        addCategory.setOnAction(e -> {
            try {
                if (newCategoryName.getText().isBlank()) return;

                ComponentCategoryRepository.create(newCategoryName.getText());

                // Refresh category dropdown to show new category
                categorySelect.getItems().setAll(
                        ComponentCategoryRepository.getAllActiveNames()
                );

                newCategoryName.clear();

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        // Handle creating new component in selected category
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

                // Refresh component list to show new component
                componentList.getItems().setAll(
                        ComponentRepository.getActiveNamesByCategory(categoryId)
                );

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        // Button to delete selected component
        Button deleteComponent = new Button("Delete Component");
        deleteComponent.setDisable(true);

        // Enable delete button only when a component is selected
        componentList.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            deleteComponent.setDisable(n == null);
        });

        // Handle component deletion
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

        // Button to delete selected category
        Button deleteCategory = new Button("Delete Category");
        deleteCategory.setDisable(true);

        // Enable delete button only when a category is selected
        categorySelect.valueProperty().addListener((obs, o, n) -> {
            deleteCategory.setDisable(n == null);
        });

        // Handle category deletion (only if empty)
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

                // Don't allow deletion if category contains components
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

        HBox categoryRow = new HBox(10, newCategoryName, addCategory);
        HBox componentRow = new HBox(10, categorySelect, newComponentName, addComponent);
        HBox deleteRow = new HBox(10, deleteComponent, deleteCategory);

        // ===== INCIDENT TEMPLATES SECTION (ADMIN ONLY) =====
        if (SessionManager.isAdmin()) {
            Label templateTitle = new Label("Incident Templates");
            templateTitle.setFont(Font.font(null, FontWeight.BOLD, 14));

            // List to display existing templates
            ListView<String> templateList = new ListView<>();
            templateList.setPrefHeight(150);

            // Refresh template list from database
            Runnable refreshTemplates = () -> {
                try {
                    templateList.getItems().clear();
                    List<IncidentTemplateRepository.Template> templates = IncidentTemplateRepository.getAll();
                    for (IncidentTemplateRepository.Template t : templates) {
                        String display = t.name + " (" + t.severity + ")";
                        templateList.getItems().add(display);
                        // Store template object in list properties for later retrieval
                        templateList.getProperties().put(display, t);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            };

            // Initial load of templates
            refreshTemplates.run();

            // Fields for creating new template
            TextField templateName = new TextField();
            templateName.setPromptText("Template Name");

            ComboBox<String> templateSeverity = new ComboBox<>();
            templateSeverity.getItems().addAll("LOW", "MEDIUM", "HIGH");
            templateSeverity.setValue("MEDIUM");

            TextField templateDesc = new TextField();
            templateDesc.setPromptText("Description (optional)");

            // Category selector for choosing which components to add
            ComboBox<String> templateCategory = new ComboBox<>();
            templateCategory.setPromptText("Select Category");
            try {
                templateCategory.getItems().addAll(ComponentCategoryRepository.getAllActiveNames());
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            // List showing available components from selected category
            ListView<String> availableComponents = new ListView<>();
            availableComponents.setPrefHeight(100);

            // Map to store selected components (componentName -> componentId)
            Map<String, String> selectedComponentMap = new HashMap<>();
            
            // Observable list for selected components display
            ObservableList<String> selectedComponentsList = FXCollections.observableArrayList();
            
            // ListView showing currently selected components with remove capability
            ListView<String> selectedComponentsView = new ListView<>(selectedComponentsList);
            selectedComponentsView.setPrefHeight(100);

            // Load available components when category is selected
            templateCategory.setOnAction(e -> {
                availableComponents.getItems().clear();
                try {
                    String catId = ComponentCategoryRepository.getIdByName(templateCategory.getValue());
                    availableComponents.getItems().addAll(ComponentRepository.getActiveNamesByCategory(catId));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });

            // Button to add selected component from available list to selected list
            Button addComponentToTemplate = new Button("Add Selected →");
            addComponentToTemplate.setOnAction(e -> {
                String selected = availableComponents.getSelectionModel().getSelectedItem();
                if (selected == null) return;
                
                // Don't add duplicates
                if (selectedComponentMap.containsKey(selected)) return;
                
                try {
                    // Get component ID and store in map
                    String componentId = ComponentRepository.getIdByName(selected);
                    if (componentId != null) {
                        selectedComponentMap.put(selected, componentId);
                        selectedComponentsList.add(selected);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });

            // Button to remove selected component from selected list
            Button removeComponentFromTemplate = new Button("← Remove Selected");
            removeComponentFromTemplate.setOnAction(e -> {
                String selected = selectedComponentsView.getSelectionModel().getSelectedItem();
                if (selected == null) return;
                
                // Remove from both map and display list
                selectedComponentMap.remove(selected);
                selectedComponentsList.remove(selected);
            });

            // Handle template creation
            Button addTemplate = new Button("Create Template");
            addTemplate.setOnAction(e -> {
                try {
                    if (templateName.getText().isBlank()) return;
                    if (selectedComponentMap.isEmpty()) {
                        Alert alert = new Alert(Alert.AlertType.WARNING);
                        alert.setTitle("No Components");
                        alert.setHeaderText("Please select at least one component");
                        alert.showAndWait();
                        return;
                    }

                    // Collect component IDs from the selected components map
                    List<String> componentIds = new ArrayList<>(selectedComponentMap.values());

                    // Create template in database
                    IncidentTemplateRepository.create(
                            templateName.getText(),
                            templateSeverity.getValue(),
                            templateDesc.getText().isBlank() ? null : templateDesc.getText(),
                            componentIds
                    );

                    // Clear form fields
                    templateName.clear();
                    templateDesc.clear();
                    templateCategory.setValue(null);
                    availableComponents.getItems().clear();
                    selectedComponentMap.clear();
                    selectedComponentsList.clear();
                    
                    // Refresh template list
                    refreshTemplates.run();

                } catch (Exception ex) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText("Failed to create template");
                    alert.setContentText(ex.getMessage());
                    alert.showAndWait();
                }
            });

            // Handle template deletion
            Button deleteTemplate = new Button("Delete Template");
            deleteTemplate.setDisable(true);

            // Enable delete button only when a template is selected
            templateList.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
                deleteTemplate.setDisable(n == null);
            });

            deleteTemplate.setOnAction(e -> {
                String selected = templateList.getSelectionModel().getSelectedItem();
                if (selected == null) return;

                // Retrieve template object from list properties
                IncidentTemplateRepository.Template template = 
                    (IncidentTemplateRepository.Template) templateList.getProperties().get(selected);

                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Confirm Delete");
                confirm.setHeaderText("Delete template?");
                confirm.setContentText(template.name);

                if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

                try {
                    IncidentTemplateRepository.delete(template.id);
                    refreshTemplates.run();
                } catch (Exception ex) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setContentText(ex.getMessage());
                    alert.showAndWait();
                }
            });

            // Layout for template creation form
            HBox templateRow1 = new HBox(10, templateName, templateSeverity);
            HBox templateRow2 = new HBox(10, templateDesc);
            HBox templateRow3 = new HBox(10, templateCategory);
            
            // Two-column layout for component selection
            VBox availableColumn = new VBox(5);
            availableColumn.getChildren().addAll(
                new Label("Available Components:"),
                availableComponents,
                addComponentToTemplate
            );
            
            VBox selectedColumn = new VBox(5);
            selectedColumn.getChildren().addAll(
                new Label("Selected Components:"),
                selectedComponentsView,
                removeComponentFromTemplate
            );
            
            HBox componentSelectionRow = new HBox(10, availableColumn, selectedColumn);

            // Add template management section to content
            content.getChildren().addAll(
                    new Separator(),
                    templateTitle,
                    new Label("Template Details:"),
                    templateRow1,
                    templateRow2,
                    templateRow3,
                    new Label("Component Selection:"),
                    componentSelectionRow,
                    addTemplate,
                    new Label("Existing Templates:"),
                    templateList,
                    deleteTemplate
            );
        }
        
        // ===== APPLICATION UPDATE SECTION (ALL USERS) =====
        Label updateTitle = new Label("Application Updates");
        updateTitle.setFont(Font.font(null, FontWeight.BOLD, 14));

        // Get and display current version from version.properties
        String currentVersion = getCurrentVersion();
        Label versionLabel = new Label("Current Version: " + currentVersion);
        versionLabel.setTextFill(Color.web("#e74c3c"));
        versionLabel.setFont(Font.font(null, FontWeight.BOLD, 14));

        // Button to manually check for updates
        Button checkUpdateBtn = new Button("Check for Updates");
        checkUpdateBtn.setOnAction(e -> {
            // Disable button during check to prevent multiple simultaneous requests
            checkUpdateBtn.setDisable(true);
            checkUpdateBtn.setText("Checking...");

            // Run update check on background thread to avoid blocking UI
            new Thread(() -> {
                try {
                    UpdateChecker.UpdateInfo updateInfo = UpdateChecker.checkForUpdates();

                    Platform.runLater(() -> {
                        // Re-enable button after check completes
                        checkUpdateBtn.setDisable(false);
                        checkUpdateBtn.setText("Check for Updates");

                        if (updateInfo == null) {
                            // Update check failed (network issue, GitHub API down, etc.)
                            Alert error = new Alert(Alert.AlertType.ERROR);
                            error.setTitle("Update Check Failed");
                            error.setHeaderText("Could not check for updates");
                            error.setContentText("Please check your internet connection and try again.");
                            error.showAndWait();
                        } else if (updateInfo.updateAvailable) {
                            // New version available - show update dialog
                            UpdateDialog dialog = new UpdateDialog(updateInfo);
                            dialog.showAndWait();
                        } else {
                            // Already on latest version
                            Alert upToDate = new Alert(Alert.AlertType.INFORMATION);
                            upToDate.setTitle("No Updates Available");
                            upToDate.setHeaderText("You're up to date!");
                            upToDate.setContentText("You are running the latest version (" + currentVersion + ").");
                            upToDate.showAndWait();
                        }
                    });

                } catch (Exception ex) {
                    ex.printStackTrace();
                    Platform.runLater(() -> {
                        checkUpdateBtn.setDisable(false);
                        checkUpdateBtn.setText("Check for Updates");

                        Alert error = new Alert(Alert.AlertType.ERROR);
                        error.setTitle("Error");
                        error.setHeaderText("Update check failed");
                        error.setContentText(ex.getMessage());
                        error.showAndWait();
                    });
                }
            }).start();
        });

        HBox updateRow = new HBox(15, checkUpdateBtn, versionLabel);

        // ===== BOTTOM SECTION (NAVIGATION) =====
        Button back = new Button("Back");
        back.setOnAction(e -> Navigator.show(new LandingPage()));

        VBox bottomSection = new VBox(10);
        
        // Admin-only audit log button
        if (SessionManager.isAdmin()) {
            Button viewAuditLog = new Button("View Audit Log");
            viewAuditLog.setOnAction(e -> Navigator.show(new AuditLogPage()));
            bottomSection.getChildren().add(viewAuditLog);
        }
        
        bottomSection.getChildren().add(back);

        // Add all sections to content container
        content.getChildren().addAll(
                title,
                new Label("Categories"),
                categoryRow,
                new Label("Components"),
                componentRow,
                componentList,
                deleteRow,
                new Separator(),
                updateTitle,
                updateRow,
                new Separator(),
                bottomSection
        );

        // Place content inside scrollpane and add to page
        scrollPane.setContent(content);
        getChildren().add(scrollPane);
    }

    /**
     * Reads the current version from version.properties resource file.
     * 
     * @return Current version string or "Unknown" if not found
     */
    private String getCurrentVersion() {
        try (InputStream input = getClass().getResourceAsStream("/version.properties")) {
            if (input == null) {
                return "Unknown";
            }
            Properties props = new Properties();
            props.load(input);
            return props.getProperty("version", "Unknown");
        } catch (Exception e) {
            e.printStackTrace();
            return "Unknown";
        }
    }
}