package com.example.demo.admin;

import com.example.demo.Launcher;
import com.example.demo.common.*;
import com.example.demo.model.Category;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Admin screen for viewing, searching, creating, updating, and deleting course
 * categories. Manages a filtered/sorted table, a simple form, and integrates
 * with {@link CategoryApi} for CRUD operations.
 */
public class CategoryController {

    // Table
    @FXML private TableView<Category> categoryTable;
    @FXML private TableColumn<Category, Long>   categoryIdColumn;
    @FXML private TableColumn<Category, String> categoryNameColumn;

    // Form
    @FXML private TextField categoryNameField;

    // Dynamic (from FXML toolbars)
    @FXML private TextField categorySearchField;
    @FXML private Label categoryCountLabel;

    private final CategoryApi categoryApi = new CategoryApi();

    // Data pipeline
    private final ObservableList<Category> master = FXCollections.observableArrayList();
    private final FilteredList<Category> filtered = new FilteredList<>(master, c -> true);
    private final SortedList<Category>   sorted   = new SortedList<>(filtered);
    private long totalCategories;

    /**
     * Initializes the category table, search, and loads the initial data set.
     */
    @FXML
    public void initialize() {
        // Columns
        categoryIdColumn.setCellValueFactory(c ->
                new ReadOnlyObjectWrapper<>(c.getValue().getId()));
        categoryNameColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("name"));

        // Sorting + show all rows
        sorted.comparatorProperty().bind(categoryTable.comparatorProperty());
        categoryTable.setItems(sorted);
        TableUtils.style(categoryTable, categoryIdColumn, categoryNameColumn);

        // Selection -> form
        categoryTable.getSelectionModel().selectedItemProperty().addListener((o, oldSel, c) -> {
            if (c != null) categoryNameField.setText(c.getName());
        });

        // Live search
        categorySearchField.textProperty().addListener((obs, o, n) -> {
            final String q = n == null ? "" : n.toLowerCase().trim();
            filtered.setPredicate(c -> {
                if (c == null) return false;
                boolean byName = c.getName() != null && c.getName().toLowerCase().contains(q);
                boolean byId   = c.getId() != null && String.valueOf(c.getId()).contains(q);
                return q.isEmpty() || byName || byId;
            });
            updateCount();
        });

        // Not ambiguous: explicitly ListChangeListener
        filtered.addListener((ListChangeListener<Category>) change -> updateCount());

        // Initial data
        loadCategories();
    }

    /**
     * Fetches categories asynchronously and refreshes the table.
     */
    private void loadCategories() {
        CompletableFuture.runAsync(() -> {
            try {
                PageResponse<Category> response = categoryApi.list(0, 50);
                List<Category> list = response != null && response.getContent() != null
                        ? response.getContent()
                        : Collections.emptyList();
                totalCategories = response != null ? response.getTotalElements() : list.size();
                Platform.runLater(() -> {
                    master.setAll(list);
                    updateCount();
                });
            } catch (ApiException ex) {
                Platform.runLater(() -> ErrorDialog.showError(ex.getMessage(), ex.getErrorCode()));
            } catch (Exception ex) {
                Platform.runLater(() -> ErrorDialog.showError("Failed to load categories: " + ex.getMessage()));
            }
        });
    }

    /**
     * Updates the count label based on current filters and totals.
     */
    private void updateCount() {
        long displayed = filtered.size();
        if (totalCategories > 0 && totalCategories >= displayed) {
            categoryCountLabel.setText(displayed + " items (total " + totalCategories + ")");
        } else {
            categoryCountLabel.setText(displayed + " items");
        }
    }

    /**
     * Handles pressing Enter in the category form by routing to add or update
     * based on whether a row is selected.
     */
    @FXML
    private void onSubmitCategory() {
        if (categoryTable.getSelectionModel().getSelectedItem() != null) {
            onUpdateCategory();
        } else {
            onAddCategory();
        }
    }

    /**
     * Handles adding a new category from the form input.
     */
    @FXML
    private void onAddCategory() {
        String name = categoryNameField.getText().trim();
        if (name.isEmpty()) { AlertUtils.warn("Please enter a category name."); return; }
        CompletableFuture.runAsync(() -> {
            try {
                Category created = categoryApi.create(name);
                Platform.runLater(() -> {
                    master.add(created);
                    clearForm();
                    updateCount();
                    AlertUtils.info("Category added successfully!");
                });
            } catch (ApiException ex) {
                Platform.runLater(() -> ErrorDialog.showError(ex.getMessage(), ex.getErrorCode()));
            } catch (Exception ex) {
                Platform.runLater(() -> ErrorDialog.showError("Failed to add category: " + ex.getMessage()));
            }
        });
    }

    /**
     * Handles updating the selected category with the current form value.
     */
    @FXML
    private void onUpdateCategory() {
        Category selected = categoryTable.getSelectionModel().getSelectedItem();
        if (selected == null) { AlertUtils.warn("Please select a category to update."); return; }
        String name = categoryNameField.getText().trim();
        if (name.isEmpty()) { AlertUtils.warn("Category name cannot be empty."); return; }

        CompletableFuture.runAsync(() -> {
            try {
                Category updated = categoryApi.update(selected.getId(), name);
                Platform.runLater(() -> {
                    for (int i = 0; i < master.size(); i++) {
                        if (master.get(i).getId().equals(selected.getId())) {
                            master.set(i, updated);
                            break;
                        }
                    }
                    clearForm();
                    updateCount();
                    AlertUtils.info("Category updated successfully!");
                });
            } catch (ApiException ex) {
                Platform.runLater(() -> ErrorDialog.showError(ex.getMessage(), ex.getErrorCode()));
            } catch (Exception ex) {
                Platform.runLater(() -> ErrorDialog.showError("Failed to update category: " + ex.getMessage()));
            }
        });
    }

    /**
     * Deletes the selected category.
     */
    @FXML
    private void onDeleteCategory() {
        Category selected = categoryTable.getSelectionModel().getSelectedItem();
        if (selected == null) { AlertUtils.warn("Please select a category to delete."); return; }
        CompletableFuture.runAsync(() -> {
            try {
                categoryApi.delete(selected.getId());
                Platform.runLater(() -> {
                    master.removeIf(c -> c.getId().equals(selected.getId()));
                    clearForm();
                    updateCount();
                    AlertUtils.info("Category deleted successfully!");
                });
            } catch (ApiException ex) {
                Platform.runLater(() -> ErrorDialog.showError(ex.getMessage(), ex.getErrorCode()));
            } catch (Exception ex) {
                Platform.runLater(() -> ErrorDialog.showError("Failed to delete category: " + ex.getMessage()));
            }
        });
    }

    /**
     * Clears the form and table selection.
     */
    private void clearForm() {
        categoryNameField.clear();
        categoryTable.getSelectionModel().clearSelection();
    }

    /**
     * Navigates back to the admin dashboard.
     */
    @FXML private void backToMain() { Launcher.go("dashboard.fxml", "Dashboard"); }
    /**
     * Logs out the current user and returns to the login screen.
     */
    @FXML public void onLogout() {
        TokenStore.clear();
        SessionStore.clearAll();
        Launcher.go("login.fxml", "Login");
    }
}
