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
}