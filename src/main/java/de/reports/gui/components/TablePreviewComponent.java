package de.reports.gui.components;

import de.reports.database.DatabaseManager;
import de.reports.database.QueryResult;
import de.reports.i18n.MessageBundle;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * TablePreviewComponent - Modular component for table data preview
 * Handles data loading, pagination, and table display
 *
 * Features:
 * - Table data preview with pagination
 * - Dynamic column setup
 * - Filtered data display
 * - Loading states and error handling
 */
public class TablePreviewComponent {
    private static final Logger logger = LoggerFactory.getLogger(TablePreviewComponent.class);

    // UI Components
    private VBox container;
    private Label titleLabel;
    private TableView<Map<String, Object>> previewTableView;
    private PaginationComponent paginationComponent;
    private ProgressIndicator progressIndicator;

    // State
    private DatabaseManager databaseManager;
    private String currentTableName;
    private Consumer<String> statusUpdateCallback;

    // Constructor
    public TablePreviewComponent() {
        initializeComponents();
        setupLayout();
        setupPaginationCallbacks();
    }

    private void initializeComponents() {
        container = new VBox();
        container.setSpacing(10);
        container.setPadding(new Insets(10));

        // Title
        titleLabel = new Label(MessageBundle.getMessage("table.data.preview") + ":");
        titleLabel.getStyleClass().add("section-label");

        // Preview table
        setupPreviewTableView();

        // Pagination component
        paginationComponent = new PaginationComponent();

        // Progress indicator
        progressIndicator = new ProgressIndicator();
        progressIndicator.setVisible(false);
        progressIndicator.setPrefSize(20, 20);
    }

    private void setupPreviewTableView() {
        previewTableView = new TableView<>();
        // Remove fixed height to make it responsive
        previewTableView.setMaxHeight(Double.MAX_VALUE); // Allow unlimited vertical growth
        previewTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        previewTableView.setPlaceholder(new Label(MessageBundle.getMessage("table.no.data")));
    }

    private void setupLayout() {
        container.getChildren().addAll(
            titleLabel,
            previewTableView,
            paginationComponent.getContainer()
        );

        // Make TableView grow to fill available vertical space
        VBox.setVgrow(previewTableView, Priority.ALWAYS);
    }

    private void setupPaginationCallbacks() {
        paginationComponent.setCallback(new PaginationComponent.PaginationCallback() {
            @Override
            public void onPageChanged(int page, int recordsPerPage) {
                if (currentTableName != null) {
                    loadTableDataPaginated(currentTableName, page, recordsPerPage);
                }
            }

            @Override
            public void onRecordsPerPageChanged(int recordsPerPage) {
                if (currentTableName != null) {
                    // Reset to page 1 when changing records per page
                    loadTableDataPaginated(currentTableName, 1, recordsPerPage);
                }
            }
        });
    }

    /**
     * Load initial table preview data
     */
    public void loadTablePreview(String tableName, long totalRecords) {
        this.currentTableName = tableName;

        // Initialize pagination
        paginationComponent.setTotalRecords(totalRecords);
        paginationComponent.setCurrentPage(1);
        paginationComponent.setRecordsPerPage(100);

        // Load first page
        loadTableDataPaginated(tableName, 1, 100);
    }

    /**
     * Load table data with pagination
     */
    private void loadTableDataPaginated(String tableName, int page, int recordsPerPage) {
        setLoading(true);
        updateStatus("Lade Seite " + page + " von " + tableName + "...");

        Task<QueryResult> loadDataTask = new Task<QueryResult>() {
            @Override
            protected QueryResult call() throws Exception {
                // Use SQL Server pagination syntax
                int offset = (page - 1) * recordsPerPage;
                String sql = "SELECT * FROM " + tableName +
                           " ORDER BY (SELECT NULL) OFFSET " + offset +
                           " ROWS FETCH NEXT " + recordsPerPage + " ROWS ONLY";
                return databaseManager.executeQuery(sql);
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    QueryResult result = getValue();
                    setupPreviewTableColumns(result);

                    ObservableList<Map<String, Object>> previewRows =
                        FXCollections.observableArrayList(result.getData());
                    previewTableView.setItems(previewRows);

                    // Update status
                    int startRecord = (page - 1) * recordsPerPage + 1;
                    int endRecord = Math.min(page * recordsPerPage, (int)paginationComponent.getTotalRecords());
                    updateStatus("Seite " + page + " geladen: Datensätze " + startRecord + "-" + endRecord);

                    setLoading(false);
                });
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    updateStatus("Fehler beim Laden der Daten: " + getException().getMessage());
                    setLoading(false);
                    logger.error("Failed to load paginated data for table: " + tableName, getException());
                });
            }
        };

        Thread loadThread = new Thread(loadDataTask);
        loadThread.setDaemon(true);
        loadThread.start();
    }

    /**
     * Setup table columns dynamically based on query result
     */
    private void setupPreviewTableColumns(QueryResult queryResult) {
        previewTableView.getColumns().clear();

        if (queryResult.getData().isEmpty()) {
            return;
        }

        // Get column names from first row
        Map<String, Object> firstRow = queryResult.getData().get(0);

        for (String columnName : firstRow.keySet()) {
            // Skip ID column - not needed for preview
            if ("id".equalsIgnoreCase(columnName)) {
                continue;
            }

            // Get German display name for column
            String displayName = getGermanColumnName(columnName);

            TableColumn<Map<String, Object>, Object> column = new TableColumn<>(displayName);
            column.setCellValueFactory(cellData -> {
                Object value = cellData.getValue().get(columnName);
                return new javafx.beans.property.SimpleObjectProperty<>(value);
            });

            // Set cell factory for better display with special formatting
            column.setCellFactory(col -> new TableCell<Map<String, Object>, Object>() {
                @Override
                protected void updateItem(Object item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setAlignment(Pos.CENTER_LEFT); // Default alignment
                    } else {
                        // Special formatting for maka (Sonder Abt.) column
                        if ("maka".equalsIgnoreCase(columnName)) {
                            setText(getMakaDisplayText(item));
                            setAlignment(Pos.CENTER); // Center alignment for Sonder Abt.
                        } else if ("merkmalsposition".equalsIgnoreCase(columnName)) {
                            setText(item.toString());
                            setAlignment(Pos.CENTER); // Center alignment for Position
                        } else if ("fertigungsliste".equalsIgnoreCase(columnName)) {
                            setText(item.toString());
                            setAlignment(Pos.CENTER); // Center alignment for Fertigungsliste
                        } else {
                            setText(item.toString());
                            setAlignment(Pos.CENTER_LEFT); // Default left alignment
                        }
                    }
                }
            });

            previewTableView.getColumns().add(column);
        }
    }

    /**
     * Get German display name for database column
     */
    private String getGermanColumnName(String columnName) {
        switch (columnName.toLowerCase()) {
            case "identnr": return "Identnr";
            case "merkmal": return "Merkmal";
            case "auspraegung": return "Ausprägung";
            case "drucktext": return "Drucktext";
            case "sondermerkmal": return "Sondermerkmal";
            case "merkmalsposition": return "Position";
            case "maka": return "Sonder Abt.";
            case "fertigungsliste": return "Fertigungsliste";
            default: return columnName; // Fallback to original name
        }
    }

    /**
     * Convert maka numeric value to German color text
     */
    private String getMakaDisplayText(Object makaValue) {
        if (makaValue == null) {
            return "Keine";
        }

        try {
            int value = Integer.parseInt(makaValue.toString());
            switch (value) {
                case 0: return "Keine";
                case 1: return "schwarz";
                case 2: return "blau";
                case 3: return "rot";
                case 4: return "orange";
                case 5: return "grün";
                case 6: return "weiss";
                case 7: return "gelb";
                default: return "Unbekannt (" + value + ")";
            }
        } catch (NumberFormatException e) {
            return makaValue.toString();
        }
    }

    /**
     * Show filtered data (called from filter components)
     */
    public void showFilteredData(String tableName, String whereClause) {
        this.currentTableName = tableName;
        setLoading(true);
        updateStatus("Lade gefilterte Daten...");

        Task<QueryResult> filterTask = new Task<QueryResult>() {
            @Override
            protected QueryResult call() throws Exception {
                String sql = "SELECT * FROM " + tableName;
                if (whereClause != null && !whereClause.trim().isEmpty()) {
                    sql += " WHERE " + whereClause;
                }
                sql += " ORDER BY (SELECT NULL) OFFSET 0 ROWS FETCH NEXT 1000 ROWS ONLY";
                return databaseManager.executeQuery(sql);
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    QueryResult result = getValue();
                    setupPreviewTableColumns(result);

                    ObservableList<Map<String, Object>> filteredRows =
                        FXCollections.observableArrayList(result.getData());
                    previewTableView.setItems(filteredRows);

                    updateStatus("Filter angewendet: " + result.getData().size() + " Datensätze gefunden");
                    setLoading(false);

                    // Update pagination for filtered data
                    paginationComponent.setTotalRecords(result.getData().size());
                    paginationComponent.setCurrentPage(1);
                });
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    updateStatus("Fehler beim Filtern: " + getException().getMessage());
                    setLoading(false);
                    logger.error("Failed to load filtered data", getException());
                });
            }
        };

        Thread filterThread = new Thread(filterTask);
        filterThread.setDaemon(true);
        filterThread.start();
    }

    /**
     * Clear all data and reset view
     */
    public void clearPreview() {
        previewTableView.getColumns().clear();
        previewTableView.setItems(FXCollections.observableArrayList());
        paginationComponent.reset();
        currentTableName = null;
        updateStatus("Bereit");
    }

    // Helper methods
    private void setLoading(boolean loading) {
        progressIndicator.setVisible(loading);
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
    }

    public void setStatusUpdateCallback(Consumer<String> callback) {
        this.statusUpdateCallback = callback;
    }

    public String getCurrentTableName() {
        return currentTableName;
    }

    public PaginationComponent getPaginationComponent() {
        return paginationComponent;
    }
}