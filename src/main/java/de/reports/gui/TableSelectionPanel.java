package de.reports.gui;

import de.reports.database.ColumnInfo;
import de.reports.database.DatabaseManager;
import de.reports.database.QueryResult;
import de.reports.i18n.MessageBundle;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class TableSelectionPanel {
    private static final Logger logger = LoggerFactory.getLogger(TableSelectionPanel.class);

    private VBox container;
    private ListView<String> tableListView;
    private TableView<ColumnInfo> columnTableView;
    private TableView<Map<String, Object>> previewTableView;
    private TextField searchField;
    private Button refreshButton;
    private Label statusLabel;
    private ProgressIndicator progressIndicator;

    private DatabaseManager databaseManager;
    private Consumer<String> onTableSelected;
    private List<ColumnInfo> currentTableColumns;
    private String selectedTableName;

    public TableSelectionPanel() {
        initializeComponents();
        setupLayout();
        setupEventHandlers();
    }

    private void initializeComponents() {
        container = new VBox();
        container.setPadding(new Insets(20));
        container.setSpacing(15);

        // Title
        Label titleLabel = new Label(MessageBundle.getMessage("table.selection.title"));
        titleLabel.getStyleClass().add("section-title");

        // Search and refresh controls
        HBox controlsBox = new HBox(10);
        searchField = new TextField();
        searchField.setPromptText(MessageBundle.getMessage("table.search.prompt"));
        searchField.setPrefWidth(200);

        refreshButton = new Button(MessageBundle.getMessage("table.refresh"));
        progressIndicator = new ProgressIndicator();
        progressIndicator.setVisible(false);
        progressIndicator.setPrefSize(20, 20);

        statusLabel = new Label(MessageBundle.getMessage("status.ready"));
        statusLabel.getStyleClass().add("status-label");

        controlsBox.getChildren().addAll(new Label("Suche:"), searchField, refreshButton, progressIndicator);

        // Table list
        Label tableLabel = new Label(MessageBundle.getMessage("table.selection") + ":");
        tableListView = new ListView<>();
        tableListView.setPrefHeight(180);
        tableListView.getStyleClass().add("table-list");

        // Column information table
        Label columnLabel = new Label(MessageBundle.getMessage("table.columns") + ":");
        setupColumnTableView();

        // Data preview table
        Label previewLabel = new Label(MessageBundle.getMessage("table.data.preview") + " (erste 100 Zeilen):");
        setupPreviewTableView();

        container.getChildren().addAll(
            titleLabel,
            controlsBox,
            statusLabel,
            tableLabel,
            tableListView,
            columnLabel,
            columnTableView,
            previewLabel,
            previewTableView
        );
    }

    private void setupColumnTableView() {
        columnTableView = new TableView<>();
        columnTableView.setPrefHeight(150);

        // Column name
        TableColumn<ColumnInfo, String> nameColumn = new TableColumn<>("Spaltenname");
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("columnName"));
        nameColumn.setPrefWidth(150);

        // Data type
        TableColumn<ColumnInfo, String> typeColumn = new TableColumn<>("Typ");
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("dataType"));
        typeColumn.setPrefWidth(100);

        // Nullable
        TableColumn<ColumnInfo, Boolean> nullableColumn = new TableColumn<>("Nullable");
        nullableColumn.setCellValueFactory(new PropertyValueFactory<>("nullable"));
        nullableColumn.setPrefWidth(80);

        // Size
        TableColumn<ColumnInfo, Integer> sizeColumn = new TableColumn<>("Größe");
        sizeColumn.setCellValueFactory(new PropertyValueFactory<>("columnSize"));
        sizeColumn.setPrefWidth(80);

        columnTableView.getColumns().addAll(nameColumn, typeColumn, nullableColumn, sizeColumn);
    }

    private void setupPreviewTableView() {
        previewTableView = new TableView<>();
        previewTableView.setPrefHeight(200);
        previewTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void setupLayout() {
        container.getStyleClass().add("content-panel");
    }

    private void setupEventHandlers() {
        // Table selection handler
        tableListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectTable(newVal);
            }
        });

        // Search filter handler
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filterTables(newVal);
        });

        // Refresh button handler
        refreshButton.setOnAction(e -> refreshTables());
    }

    public void setDatabaseManager(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
        refreshTables();
    }

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
                logger.info("Loading table names from database");
                return databaseManager.getTableNames();
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    List<String> tables = getValue();
                    tableListView.getItems().clear();
                    tableListView.getItems().addAll(tables);
                    updateStatus(tables.size() + " Tabellen gefunden");
                    setLoading(false);
                    logger.info("Loaded {} tables successfully", tables.size());
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
        };

        Thread loadThread = new Thread(loadTablesTask);
        loadThread.setDaemon(true);
        loadThread.start();
    }

    private void selectTable(String tableName) {
        if (databaseManager == null || !databaseManager.isConnected()) {
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

                // Load preview data (first 100 rows)
                QueryResult previewData = databaseManager.executeQuery(
                    "SELECT TOP 100 * FROM " + tableName
                );

                Platform.runLater(() -> {
                    // Update column table
                    ObservableList<ColumnInfo> columnData = FXCollections.observableArrayList(currentTableColumns);
                    columnTableView.setItems(columnData);

                    // Update preview table
                    setupPreviewTableColumns(previewData);
                    ObservableList<Map<String, Object>> previewRows = FXCollections.observableArrayList(previewData.getData());
                    previewTableView.setItems(previewRows);

                    updateStatus("Tabelle '" + tableName + "' geladen (" + previewData.getData().size() + " Zeilen Vorschau)");
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

    private void setupPreviewTableColumns(QueryResult queryResult) {
        previewTableView.getColumns().clear();

        for (String columnName : queryResult.getColumnNames()) {
            TableColumn<Map<String, Object>, Object> column = new TableColumn<>(columnName);
            column.setCellValueFactory(cellData -> {
                Object value = cellData.getValue().get(columnName);
                return new javafx.beans.property.SimpleObjectProperty<>(value);
            });

            // Format cell display
            column.setCellFactory(col -> new TableCell<Map<String, Object>, Object>() {
                @Override
                protected void updateItem(Object item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item.toString());
                    }
                }
            });

            column.setPrefWidth(120);
            previewTableView.getColumns().add(column);
        }
    }

    private void filterTables(String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) {
            // Show all tables
            refreshTables();
            return;
        }

        String lowerSearchText = searchText.toLowerCase();
        ObservableList<String> filteredTables = FXCollections.observableArrayList();

        for (String table : tableListView.getItems()) {
            if (table.toLowerCase().contains(lowerSearchText)) {
                filteredTables.add(table);
            }
        }

        tableListView.setItems(filteredTables);
        updateStatus(filteredTables.size() + " Tabellen gefunden (gefiltert)");
    }

    private void updateStatus(String message) {
        if (statusLabel != null) {
            Platform.runLater(() -> statusLabel.setText(message));
        }
        logger.debug("Status updated: {}", message);
    }

    private void setLoading(boolean loading) {
        Platform.runLater(() -> {
            progressIndicator.setVisible(loading);
            refreshButton.setDisable(loading);
        });
    }

    public List<ColumnInfo> getTableColumns() {
        return currentTableColumns;
    }

    public void setOnTableSelected(Consumer<String> onTableSelected) {
        this.onTableSelected = onTableSelected;
    }

    public Node getNode() {
        return container;
    }

    /**
     * Show filtered data in preview table with smart loading
     */
    public void showFilteredData(String tableName, String whereClause) {
        if (databaseManager == null || !databaseManager.isConnected()) {
            updateStatus("Nicht mit Datenbank verbunden");
            return;
        }

        this.selectedTableName = tableName;
        setLoading(true);

        Task<DatabaseManager.FilteredDataResult> task = new Task<>() {
            @Override
            protected DatabaseManager.FilteredDataResult call() {
                return databaseManager.getFilteredData(tableName, whereClause, 0, 2000);
            }
        };

        task.setOnSucceeded(event -> {
            Platform.runLater(() -> {
                DatabaseManager.FilteredDataResult result = task.getValue();
                handleFilteredDataResult(result, whereClause);
                setLoading(false);
            });
        });

        task.setOnFailed(event -> {
            Platform.runLater(() -> {
                Throwable exception = task.getException();
                logger.error("Failed to load filtered data", exception);
                updateStatus("Fehler beim Laden der gefilterten Daten: " + exception.getMessage());
                setLoading(false);
            });
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void handleFilteredDataResult(DatabaseManager.FilteredDataResult result, String whereClause) {
        if (!result.isSuccess()) {
            updateStatus("Fehler: " + result.getMessage());
            return;
        }

        long totalCount = result.getTotalCount();
        int currentCount = result.getCurrentCount();

        // Smart Loading Logic
        if (totalCount == 0) {
            updateStatus("Keine Datensätze gefunden für die angegebenen Filter");
            clearPreviewTable();
            return;
        }

        if (totalCount > 10000) {
            updateStatus(String.format("WARNUNG: %,d Datensätze gefunden! Bitte Filter eingrenzen (zu viele Ergebnisse)", totalCount));
            showFilterWarningDialog(totalCount);
            return;
        }

        if (totalCount <= 2000) {
            // Load all data
            updateStatus(String.format("Filter aktiv: %,d Datensätze geladen (alle angezeigt)", totalCount));
            updateFilteredPreviewTable(result.getData());
        } else {
            // Partial load with "Load More" option
            updateStatus(String.format("Filter aktiv: %,d von %,d Datensätzen geladen", currentCount, totalCount));
            updateFilteredPreviewTable(result.getData());
            showLoadMoreButton(whereClause, currentCount, totalCount);
        }
    }

    private void updateFilteredPreviewTable(List<Map<String, Object>> data) {
        if (data == null || data.isEmpty()) {
            clearPreviewTable();
            return;
        }

        Platform.runLater(() -> {
            // Clear existing columns
            previewTableView.getColumns().clear();

            // Get column names from first row
            Map<String, Object> firstRow = data.get(0);
            for (String columnName : firstRow.keySet()) {
                TableColumn<Map<String, Object>, Object> column = new TableColumn<>(columnName);
                column.setCellValueFactory(cellData -> {
                    Object value = cellData.getValue().get(columnName);
                    return new javafx.beans.property.SimpleObjectProperty<>(value);
                });
                column.setPrefWidth(120);
                previewTableView.getColumns().add(column);
            }

            // Convert to observable list
            ObservableList<Map<String, Object>> observableData = FXCollections.observableArrayList(data);
            previewTableView.setItems(observableData);
        });
    }

    private void clearPreviewTable() {
        Platform.runLater(() -> {
            previewTableView.getColumns().clear();
            previewTableView.setItems(FXCollections.observableArrayList());
        });
    }

    private void showFilterWarningDialog(long totalCount) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Zu viele Ergebnisse");
            alert.setHeaderText(String.format("%,d Datensätze gefunden", totalCount));
            alert.setContentText("Bitte schränken Sie Ihre Filter ein, um die Leistung zu verbessern.\n\n" +
                               "Empfohlen: Weniger als 10.000 Datensätze");

            ButtonType restrictFilters = new ButtonType("Filter einschränken");
            ButtonType forceLoad = new ButtonType("Trotzdem laden (kann langsam sein)");
            ButtonType cancel = new ButtonType("Abbrechen", ButtonBar.ButtonData.CANCEL_CLOSE);

            alert.getButtonTypes().setAll(restrictFilters, forceLoad, cancel);
            alert.showAndWait();
        });
    }

    private void showLoadMoreButton(String whereClause, int currentCount, long totalCount) {
        // This would be implemented to show a "Load More" button
        // For now, just log the need for it
        logger.info("Need to implement Load More button: {}/{} loaded", currentCount, totalCount);
        // TODO: Add "Load More" button to UI
    }

    /**
     * Load original table preview data without filters
     */
    private void loadTablePreview(String tableName) {
        if (databaseManager == null || !databaseManager.isConnected()) {
            updateStatus("Nicht mit Datenbank verbunden");
            return;
        }

        updateStatus("Lade ursprüngliche Daten für: " + tableName);
        setLoading(true);

        Task<Void> loadPreviewTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                logger.info("Loading original preview data for table: {}", tableName);

                // Load original preview data (first 100 rows, no filters)
                QueryResult previewData = databaseManager.executeQuery(
                    "SELECT TOP 100 * FROM " + tableName
                );

                Platform.runLater(() -> {
                    // Update preview table with original data
                    setupPreviewTableColumns(previewData);
                    ObservableList<Map<String, Object>> previewRows = FXCollections.observableArrayList(previewData.getData());
                    previewTableView.setItems(previewRows);

                    updateStatus("Filter entfernt - " + previewData.getData().size() + " ursprüngliche Datensätze angezeigt");
                    setLoading(false);
                });

                return null;
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    updateStatus("Fehler beim Laden der ursprünglichen Daten: " + getException().getMessage());
                    setLoading(false);
                    logger.error("Failed to load original preview data for table: " + tableName, getException());
                });
            }
        };

        Thread loadThread = new Thread(loadPreviewTask);
        loadThread.setDaemon(true);
        loadThread.start();
    }

    /**
     * Clear any active filters and show original table data
     */
    public void clearFilters() {
        if (selectedTableName != null) {
            loadTablePreview(selectedTableName);
        } else {
            updateStatus("Filter entfernt");
        }
    }
}