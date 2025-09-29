package de.reports.gui.components;

import de.reports.database.ColumnInfo;
import de.reports.i18n.MessageBundle;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static javafx.scene.layout.Priority.*;

/**
 * Column Selection Component for Report Design
 * Allows users to select which columns to include in the report
 * Optimized for LEBO merkmalstexte table structure
 * Max size: ~200 lines as requested
 */
public class ColumnSelectionComponent {

    // UI Components
    private VBox container;
    private TableView<ColumnSelectionItem> columnTable;
    private ObservableList<ColumnSelectionItem> columnItems;
    private Label statusLabel;

    // Callbacks
    private Consumer<List<String>> onSelectionChanged;

    // Selected table info
    private String selectedTableName;

    public ColumnSelectionComponent() {
        initializeComponents();
        setupLayout();
        setupEventHandlers();
    }

    private void initializeComponents() {
        container = new VBox(10);
        container.setPadding(new Insets(15));

        // Title section
        Label titleLabel = new Label(MessageBundle.getMessage("report.column.selection.title"));
        titleLabel.getStyleClass().addAll("section-title", "h3");

        // Instructions
        Label instructionsLabel = new Label(MessageBundle.getMessage("report.column.selection.instructions"));
        instructionsLabel.getStyleClass().add("instructions-text");
        instructionsLabel.setWrapText(true);

        // Quick selection buttons
        HBox quickSelectBox = createQuickSelectionButtons();

        // Column table
        createColumnTable();

        // Status
        statusLabel = new Label();
        statusLabel.getStyleClass().add("status-text");

        container.getChildren().addAll(
            titleLabel,
            instructionsLabel,
            quickSelectBox,
            columnTable,
            statusLabel
        );
    }

    private void setupLayout() {
        container.getStyleClass().add("column-selection-component");

        // Make table expandable (alternative method)
        columnTable.setMaxHeight(Double.MAX_VALUE);
        columnTable.setMinHeight(200);

        // Set preferred size
        columnTable.setPrefHeight(300);
        container.setMaxHeight(450); // Keep component compact
    }

    private HBox createQuickSelectionButtons() {
        HBox buttonBox = new HBox(10);
        buttonBox.setPadding(new Insets(5, 0, 10, 0));

        Button selectAllBtn = new Button(MessageBundle.getMessage("report.column.select.all"));
        selectAllBtn.getStyleClass().add("secondary-button");
        selectAllBtn.setOnAction(e -> selectAllColumns(true));

        Button selectNoneBtn = new Button(MessageBundle.getMessage("report.column.select.none"));
        selectNoneBtn.getStyleClass().add("secondary-button");
        selectNoneBtn.setOnAction(e -> selectAllColumns(false));

        // LEBO-specific quick selections
        Button selectLeboEssential = new Button("Merkmal + Ausprägung");
        selectLeboEssential.getStyleClass().add("accent-button");
        selectLeboEssential.setOnAction(e -> selectLeboEssentialColumns());

        buttonBox.getChildren().addAll(selectAllBtn, selectNoneBtn, selectLeboEssential);
        return buttonBox;
    }

    private void createColumnTable() {
        columnItems = FXCollections.observableArrayList();
        columnTable = new TableView<>(columnItems);

        // Selection column
        TableColumn<ColumnSelectionItem, Boolean> selectCol = new TableColumn<>(
            MessageBundle.getMessage("report.column.select"));
        selectCol.setCellValueFactory(param -> param.getValue().selectedProperty);
        selectCol.setCellFactory(CheckBoxTableCell.forTableColumn(selectCol));
        selectCol.setPrefWidth(80);
        selectCol.setEditable(true);

        // Column name
        TableColumn<ColumnSelectionItem, String> nameCol = new TableColumn<>(
            MessageBundle.getMessage("report.column.name"));
        nameCol.setCellValueFactory(param -> param.getValue().columnNameProperty);
        nameCol.setPrefWidth(150);

        // Data type
        TableColumn<ColumnSelectionItem, String> typeCol = new TableColumn<>(
            MessageBundle.getMessage("report.column.type"));
        typeCol.setCellValueFactory(param -> param.getValue().dataTypeProperty);
        typeCol.setPrefWidth(120);

        // Description (LEBO-specific)
        TableColumn<ColumnSelectionItem, String> descCol = new TableColumn<>(
            MessageBundle.getMessage("report.column.description"));
        descCol.setCellValueFactory(param -> param.getValue().descriptionProperty);
        descCol.setPrefWidth(200);

        columnTable.getColumns().addAll(selectCol, nameCol, typeCol, descCol);
        columnTable.setEditable(true);
        columnTable.getStyleClass().add("data-table");

        // Row styling for better UX
        columnTable.setRowFactory(tv -> {
            TableRow<ColumnSelectionItem> row = new TableRow<>();
            row.itemProperty().addListener((obs, oldItem, newItem) -> {
                if (newItem != null && newItem.selectedProperty.get()) {
                    row.getStyleClass().add("selected-row");
                } else {
                    row.getStyleClass().remove("selected-row");
                }
            });
            return row;
        });
    }

    private void setupEventHandlers() {
        // Listen for selection changes
        columnItems.addListener((javafx.collections.ListChangeListener<ColumnSelectionItem>) change -> {
            updateStatus();
            notifySelectionChanged();
        });
    }

    /**
     * Set the available columns from the selected table
     */
    public void setAvailableColumns(String tableName, List<ColumnInfo> columns) {
        this.selectedTableName = tableName;

        columnItems.clear();

        for (ColumnInfo column : columns) {
            ColumnSelectionItem item = new ColumnSelectionItem(
                column.getName(),
                column.getDataType(),
                getLeboColumnDescription(column.getName())
            );

            // Pre-select essential LEBO columns
            if (isLeboEssentialColumn(column.getName())) {
                item.setSelected(true);
            }

            columnItems.add(item);
        }

        updateStatus();
        notifySelectionChanged();
    }

    private void selectAllColumns(boolean selected) {
        for (ColumnSelectionItem item : columnItems) {
            item.setSelected(selected);
        }
        columnTable.refresh();
    }

    private void selectLeboEssentialColumns() {
        selectAllColumns(false);
        for (ColumnSelectionItem item : columnItems) {
            if (isLeboEssentialColumn(item.getColumnName())) {
                item.setSelected(true);
            }
        }
        columnTable.refresh();
    }

    private boolean isLeboEssentialColumn(String columnName) {
        String lower = columnName.toLowerCase();
        return lower.equals("merkmal") ||
               lower.equals("auspraegung") ||
               lower.equals("identnr");
    }

    private String getLeboColumnDescription(String columnName) {
        switch (columnName.toLowerCase()) {
            case "identnr": return "Eindeutige Identifikationsnummer";
            case "merkmal": return "Merkmalbezeichnung";
            case "auspraegung": return "Merkmalausprägung/Wert";
            default: return "Spalte: " + columnName;
        }
    }

    private void updateStatus() {
        long selectedCount = columnItems.stream().mapToLong(item -> item.isSelected() ? 1 : 0).sum();
        statusLabel.setText(String.format("%d von %d Spalten ausgewählt", selectedCount, columnItems.size()));
    }

    private void notifySelectionChanged() {
        if (onSelectionChanged != null) {
            List<String> selectedColumns = getSelectedColumns();
            onSelectionChanged.accept(selectedColumns);
        }
    }

    /**
     * Get currently selected column names
     */
    public List<String> getSelectedColumns() {
        return columnItems.stream()
            .filter(ColumnSelectionItem::isSelected)
            .map(ColumnSelectionItem::getColumnName)
            .collect(Collectors.toList());
    }

    /**
     * Set callback for selection changes
     */
    public void setOnSelectionChanged(Consumer<List<String>> callback) {
        this.onSelectionChanged = callback;
    }

    public Node getNode() {
        return container;
    }

    /**
     * Data model for column selection table
     */
    public static class ColumnSelectionItem {
        private final SimpleBooleanProperty selectedProperty;
        private final SimpleStringProperty columnNameProperty;
        private final SimpleStringProperty dataTypeProperty;
        private final SimpleStringProperty descriptionProperty;

        public ColumnSelectionItem(String columnName, String dataType, String description) {
            this.selectedProperty = new SimpleBooleanProperty(false);
            this.columnNameProperty = new SimpleStringProperty(columnName);
            this.dataTypeProperty = new SimpleStringProperty(dataType);
            this.descriptionProperty = new SimpleStringProperty(description);

            // Listen for selection changes to update UI
            this.selectedProperty.addListener((obs, oldVal, newVal) -> {
                // This will trigger table refresh through the list listener
            });
        }

        // Getters and setters
        public boolean isSelected() { return selectedProperty.get(); }
        public void setSelected(boolean selected) { selectedProperty.set(selected); }
        public SimpleBooleanProperty selectedProperty() { return selectedProperty; }

        public String getColumnName() { return columnNameProperty.get(); }
        public SimpleStringProperty columnNameProperty() { return columnNameProperty; }

        public String getDataType() { return dataTypeProperty.get(); }
        public SimpleStringProperty dataTypeProperty() { return dataTypeProperty; }

        public String getDescription() { return descriptionProperty.get(); }
        public SimpleStringProperty descriptionProperty() { return descriptionProperty; }
    }
}