package de.reports.gui;

import de.reports.database.ColumnInfo;
import de.reports.database.DatabaseManager;
import de.reports.database.QueryResult;
import de.reports.gui.components.PaginationComponent;
import de.reports.gui.components.TableListComponent;
import de.reports.gui.components.TablePreviewComponent;
import de.reports.i18n.MessageBundle;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Consumer;

/**
 * TableSelectionPanel - Main coordinator for table selection functionality
 *
 * This is the refactored main panel that coordinates between:
 * - TableListComponent (table list and search)
 * - TablePreviewComponent (data preview with pagination)
 * - Component communication and state management
 *
 * Responsibilities:
 * - Component coordination and communication
 * - Database manager distribution
 * - Public API for external components
 * - Status management
 */
public class TableSelectionPanel {
    private static final Logger logger = LoggerFactory.getLogger(TableSelectionPanel.class);

    // UI Components
    private VBox container;
    private Label titleLabel;
    private Label statusLabel;
    private ProgressIndicator progressIndicator;

    // Modular Components
    private TableListComponent tableListComponent;
    private TablePreviewComponent tablePreviewComponent;

    // State
    private DatabaseManager databaseManager;
    private Consumer<String> onTableSelected;
    private List<ColumnInfo> currentTableColumns;
    private String selectedTableName;

    public TableSelectionPanel() {
        initializeComponents();
        setupLayout();
        setupComponentCommunication();
    }

    private void initializeComponents() {
        container = new VBox();
        container.setPadding(new Insets(20));
        container.setSpacing(15);

        // Title
        titleLabel = new Label(MessageBundle.getMessage("table.selection.title"));
        titleLabel.getStyleClass().add("section-title");

        // Status
        statusLabel = new Label(MessageBundle.getMessage("status.ready"));
        statusLabel.getStyleClass().add("status-label");

        progressIndicator = new ProgressIndicator();
        progressIndicator.setVisible(false);
        progressIndicator.setPrefSize(20, 20);

        // Initialize modular components
        tableListComponent = new TableListComponent();
        tablePreviewComponent = new TablePreviewComponent();
    }

    private void setupLayout() {
        container.getChildren().addAll(
            titleLabel,
            statusLabel,
            tableListComponent.getContainer(),
            tablePreviewComponent.getContainer()
        );

        container.getStyleClass().add("content-panel");
    }

    private void setupComponentCommunication() {
        // Table selection communication
        tableListComponent.setOnTableSelected(this::selectTable);

        // Status update callbacks
        tableListComponent.setStatusUpdateCallback(this::updateStatus);
        tablePreviewComponent.setStatusUpdateCallback(this::updateStatus);
    }

    /**
     * Handle table selection from TableListComponent
     */
    private void selectTable(String tableName) {
        if (databaseManager == null || !databaseManager.isConnected()) {
            updateStatus("Nicht mit Datenbank verbunden");
            return;
        }

        selectedTableName = tableName;
        updateStatus("Lade Tabelle: " + tableName);
        setLoading(true);

        Task<Void> loadTableTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                logger.info("Loading table information for: {}", tableName);

                // Load column information
                currentTableColumns = databaseManager.getTableColumns(tableName);
                logger.info("Loaded {} columns for table {}", currentTableColumns.size(), tableName);

                // Get total record count for pagination
                QueryResult countResult = databaseManager.executeQuery(
                    "SELECT COUNT(*) as total_count FROM " + tableName
                );

                long totalRecords = 0;
                if (!countResult.getData().isEmpty()) {
                    Object countValue = countResult.getData().get(0).get("total_count");
                    totalRecords = countValue instanceof Number ? ((Number) countValue).longValue() : 0;
                }
                final long finalTotalRecords = totalRecords;

                Platform.runLater(() -> {
                    // Initialize table preview with total record count
                    tablePreviewComponent.loadTablePreview(tableName, finalTotalRecords);

                    updateStatus("Tabelle '" + tableName + "' geladen (" + finalTotalRecords + " Datensätze total)");
                    setLoading(false);

                    // Notify parent that table was selected
                    if (onTableSelected != null) {
                        onTableSelected.accept(tableName);
                    }
                });

                return null;
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    updateStatus("Fehler beim Laden der Tabelle: " + getException().getMessage());
                    setLoading(false);
                    logger.error("Failed to load table: " + tableName, getException());
                });
            }
        };

        Thread loadThread = new Thread(loadTableTask);
        loadThread.setDaemon(true);
        loadThread.start();
    }

    /**
     * Show filtered data in preview component
     */
    public void showFilteredData(String tableName, String whereClause) {
        if (tablePreviewComponent != null) {
            tablePreviewComponent.showFilteredData(tableName, whereClause);
        }
    }

    /**
     * Clear any active filters and return to normal table view
     */
    public void clearFilters() {
        if (selectedTableName != null && tablePreviewComponent != null) {
            // Get total record count again
            Task<Void> clearFilterTask = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    QueryResult countResult = databaseManager.executeQuery(
                        "SELECT COUNT(*) as total_count FROM " + selectedTableName
                    );

                    long totalRecords = 0;
                    if (!countResult.getData().isEmpty()) {
                        Object countValue = countResult.getData().get(0).get("total_count");
                        totalRecords = countValue instanceof Number ? ((Number) countValue).longValue() : 0;
                    }
                    final long finalTotalRecords = totalRecords;

                    Platform.runLater(() -> {
                        tablePreviewComponent.loadTablePreview(selectedTableName, finalTotalRecords);
                        updateStatus("Filter entfernt - zurück zur Originalansicht");
                    });

                    return null;
                }

                @Override
                protected void failed() {
                    Platform.runLater(() -> {
                        updateStatus("Fehler beim Entfernen der Filter: " + getException().getMessage());
                        logger.error("Failed to clear filters", getException());
                    });
                }
            };

            Thread clearThread = new Thread(clearFilterTask);
            clearThread.setDaemon(true);
            clearThread.start();
        } else {
            updateStatus("Filter entfernt");
        }
    }

    // Helper methods
    private void updateStatus(String message) {
        statusLabel.setText(message);
    }

    private void setLoading(boolean loading) {
        progressIndicator.setVisible(loading);
    }

    // Public API
    public Node getNode() {
        return container;
    }

    public void setDatabaseManager(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;

        // Distribute database manager to components
        tableListComponent.setDatabaseManager(databaseManager);
        tablePreviewComponent.setDatabaseManager(databaseManager);
    }

    public void refreshTables() {
        if (tableListComponent != null) {
            tableListComponent.refreshTables();
        }
    }

    public void setOnTableSelected(Consumer<String> onTableSelected) {
        this.onTableSelected = onTableSelected;
    }

    public String getSelectedTableName() {
        return selectedTableName;
    }

    public List<ColumnInfo> getCurrentTableColumns() {
        return currentTableColumns;
    }

    public TablePreviewComponent getTablePreviewComponent() {
        return tablePreviewComponent;
    }

    public TableListComponent getTableListComponent() {
        return tableListComponent;
    }

    /**
     * Get currently selected table from table list
     */
    public String getSelectedTable() {
        return tableListComponent != null ? tableListComponent.getSelectedTable() : null;
    }

    /**
     * Programmatically select a table
     */
    public void selectTableByName(String tableName) {
        if (tableListComponent != null) {
            tableListComponent.selectTable(tableName);
        }
    }

    /**
     * Clear all selections and reset components
     */
    public void clearAll() {
        if (tableListComponent != null) {
            tableListComponent.clearSelection();
        }
        if (tablePreviewComponent != null) {
            tablePreviewComponent.clearPreview();
        }
        selectedTableName = null;
        currentTableColumns = null;
        updateStatus("Bereit");
    }
}