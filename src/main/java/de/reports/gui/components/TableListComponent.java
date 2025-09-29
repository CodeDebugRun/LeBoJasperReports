package de.reports.gui.components;

import de.reports.database.DatabaseManager;
import de.reports.i18n.MessageBundle;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * TableListComponent - Modular component for table selection and search
 * Handles table list display, search functionality, and table selection
 *
 * Features:
 * - Table list with search filtering
 * - Table selection callbacks
 * - Loading states and refresh functionality
 * - System table filtering (shows only user tables)
 */
public class TableListComponent {
    private static final Logger logger = LoggerFactory.getLogger(TableListComponent.class);

    // UI Components
    private VBox container;
    private Button refreshButton;
    private ListView<String> tableListView;
    private ProgressIndicator progressIndicator;

    // State
    private DatabaseManager databaseManager;
    private ObservableList<String> allTables;
    private Consumer<String> onTableSelected;
    private Consumer<String> statusUpdateCallback;

    // Constructor
    public TableListComponent() {
        initializeComponents();
        setupLayout();
        setupEventHandlers();
    }

    private void initializeComponents() {
        container = new VBox();
        container.setSpacing(10);
        container.setPadding(new Insets(10));

        // Refresh button
        refreshButton = new Button(MessageBundle.getMessage("table.refresh"));

        progressIndicator = new ProgressIndicator();
        progressIndicator.setVisible(false);
        progressIndicator.setPrefSize(20, 20);

        // Table list
        tableListView = new ListView<>();
        tableListView.setPrefHeight(80); // About 3 rows
        tableListView.setMinHeight(40); // Minimum height to ensure at least one row is visible
        tableListView.setFixedCellSize(32); // Fixed row height for better visibility
        tableListView.getStyleClass().add("table-list");

        allTables = FXCollections.observableArrayList();
        tableListView.setItems(allTables);
    }

    private void setupLayout() {
        // Table selection box - label and table side by side with refresh button
        HBox tableSelectionBox = new HBox(10);
        Label tableLabel = new Label(MessageBundle.getMessage("table.selection") + ":");
        tableLabel.setPrefWidth(120); // Fixed width for alignment

        tableSelectionBox.getChildren().addAll(
            tableLabel,
            tableListView,
            refreshButton,
            progressIndicator
        );

        // Add to container
        container.getChildren().add(tableSelectionBox);
    }

    private void setupEventHandlers() {
        // Table selection handler
        tableListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && onTableSelected != null) {
                onTableSelected.accept(newVal);
            }
        });

        // Refresh button handler
        refreshButton.setOnAction(e -> refreshTables());
    }

    /**
     * Refresh tables from database
     */
    public void refreshTables() {
        if (databaseManager == null || !databaseManager.isConnected()) {
            updateStatus("Keine Datenbankverbindung");
            return;
        }

        setLoading(true);
        updateStatus("Lade Tabellen...");

        Task<List<String>> loadTablesTask = new Task<List<String>>() {
            @Override
            protected List<String> call() throws Exception {
                logger.info("Loading tables from database");
                return databaseManager.getTableNames();
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    List<String> tables = getValue();

                    // Filter out system tables
                    List<String> userTables = tables.stream()
                        .filter(this::isUserTable)
                        .collect(Collectors.toList());

                    allTables.clear();
                    allTables.addAll(userTables);

                    updateStatus(userTables.size() + " Tabellen geladen");
                    setLoading(false);

                    logger.info("Loaded {} user tables (filtered from {} total tables)",
                              userTables.size(), tables.size());
                });
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    updateStatus("Fehler beim Laden der Tabellen: " + getException().getMessage());
                    setLoading(false);
                    logger.error("Failed to load tables", getException());
                });
            }

            private boolean isUserTable(String tableName) {
                String lowerName = tableName.toLowerCase();

                // Filter out common system tables
                return !lowerName.startsWith("sys") &&
                       !lowerName.startsWith("information_schema") &&
                       !lowerName.startsWith("msreplication") &&
                       !lowerName.startsWith("dbo.sys") &&
                       !lowerName.startsWith("__") &&
                       !lowerName.contains("$") &&
                       !lowerName.equals("dtproperties") &&
                       !lowerName.equals("sysdiagrams");
            }
        };

        Thread loadThread = new Thread(loadTablesTask);
        loadThread.setDaemon(true);
        loadThread.start();
    }


    /**
     * Get currently selected table
     */
    public String getSelectedTable() {
        return tableListView.getSelectionModel().getSelectedItem();
    }

    /**
     * Select a specific table programmatically
     */
    public void selectTable(String tableName) {
        tableListView.getSelectionModel().select(tableName);
    }

    /**
     * Clear table selection
     */
    public void clearSelection() {
        tableListView.getSelectionModel().clearSelection();
    }

    // Helper methods
    private void setLoading(boolean loading) {
        progressIndicator.setVisible(loading);
        refreshButton.setDisable(loading);
    }

    private void updateStatus(String message) {
        if (statusUpdateCallback != null) {
            statusUpdateCallback.accept(message);
        }
    }

    // Public API
    public VBox getContainer() {
        return container;
    }

    public void setDatabaseManager(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
        refreshTables(); // Auto-refresh when database manager is set
    }

    public void setOnTableSelected(Consumer<String> callback) {
        this.onTableSelected = callback;
    }

    public void setStatusUpdateCallback(Consumer<String> callback) {
        this.statusUpdateCallback = callback;
    }

    public int getTableCount() {
        return allTables.size();
    }

    public List<String> getAllTables() {
        return FXCollections.unmodifiableObservableList(allTables);
    }
}