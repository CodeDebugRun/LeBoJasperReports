package de.reports.gui.components;

import de.reports.database.ColumnInfo;
import de.reports.i18n.MessageBundle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Filter Builder Component for Report Design
 * Creates WHERE clause conditions for SQL queries
 * Optimized for LEBO merkmalstexte table structure
 * Target: ~280 lines (under 300 as requested)
 */
public class FilterBuilderComponent {

    // UI Components
    private VBox container;
    private VBox filtersContainer;
    private TextArea sqlPreview;
    private Label statusLabel;
    private Button addFilterBtn;

    // Data
    private ObservableList<FilterCondition> filterConditions;
    private List<ColumnInfo> availableColumns;

    // Callbacks
    private Consumer<String> onSqlChanged;

    public FilterBuilderComponent() {
        filterConditions = FXCollections.observableArrayList();
        initializeComponents();
        setupLayout();
        setupEventHandlers();
    }

    private void initializeComponents() {
        container = new VBox(10);
        container.setPadding(new Insets(15));

        // Title
        Label titleLabel = new Label(MessageBundle.getMessage("report.filter.title"));
        titleLabel.getStyleClass().addAll("section-title", "h3");

        // Instructions
        Label instructionsLabel = new Label(MessageBundle.getMessage("report.filter.instructions"));
        instructionsLabel.getStyleClass().add("instructions-text");
        instructionsLabel.setWrapText(true);

        // Quick filters for LEBO
        HBox quickFiltersBox = createQuickFiltersBox();

        // Filters container (dynamic)
        filtersContainer = new VBox(8);
        filtersContainer.getStyleClass().add("filters-container");

        // Add filter button
        addFilterBtn = new Button(MessageBundle.getMessage("report.filter.add"));
        addFilterBtn.getStyleClass().add("primary-button");
        addFilterBtn.setOnAction(e -> addNewFilter());

        // SQL Preview (read-only)
        Label previewLabel = new Label("SQL WHERE Vorschau:");
        previewLabel.getStyleClass().add("section-label");

        sqlPreview = new TextArea();
        sqlPreview.setPrefRowCount(3);
        sqlPreview.setEditable(false);
        sqlPreview.getStyleClass().add("sql-preview");
        sqlPreview.setWrapText(true);

        // Status
        statusLabel = new Label();
        statusLabel.getStyleClass().add("status-text");

        container.getChildren().addAll(
            titleLabel,
            instructionsLabel,
            quickFiltersBox,
            addFilterBtn,
            filtersContainer,
            previewLabel,
            sqlPreview,
            statusLabel
        );
    }

    private void setupLayout() {
        container.getStyleClass().add("filter-builder-component");
        container.setMaxHeight(400); // Keep compact
        sqlPreview.setPrefHeight(80);
    }

    private void setupEventHandlers() {
        // Listen for filter changes
        filterConditions.addListener((javafx.collections.ListChangeListener<FilterCondition>) change -> {
            updateSqlPreview();
            updateStatus();
        });
    }

    private HBox createQuickFiltersBox() {
        HBox quickBox = new HBox(10);
        quickBox.setPadding(new Insets(5, 0, 10, 0));

        Label quickLabel = new Label("Schnellfilter:");
        quickLabel.getStyleClass().add("section-label");

        // LEBO-specific quick filters
        Button merkmalFilterBtn = new Button("Merkmal Filter");
        merkmalFilterBtn.getStyleClass().add("secondary-button");
        merkmalFilterBtn.setOnAction(e -> addLeboMerkmalFilter());

        Button identnrFilterBtn = new Button("ID Filter");
        identnrFilterBtn.getStyleClass().add("secondary-button");
        identnrFilterBtn.setOnAction(e -> addLeboIdentnrFilter());

        Button clearAllBtn = new Button("Alle löschen");
        clearAllBtn.getStyleClass().add("danger-button");
        clearAllBtn.setOnAction(e -> clearAllFilters());

        quickBox.getChildren().addAll(quickLabel, merkmalFilterBtn, identnrFilterBtn, clearAllBtn);
        return quickBox;
    }

    private void addNewFilter() {
        if (availableColumns == null || availableColumns.isEmpty()) {
            showAlert("Keine Spalten verfügbar. Wählen Sie zuerst eine Tabelle aus.");
            return;
        }

        FilterCondition condition = new FilterCondition();
        condition.setColumn(availableColumns.get(0).getName()); // Default to first column
        addFilterToUI(condition);
        filterConditions.add(condition);
    }

    private void addLeboMerkmalFilter() {
        if (!hasColumn("merkmal")) {
            showAlert("Spalte 'merkmal' nicht gefunden.");
            return;
        }

        FilterCondition condition = new FilterCondition();
        condition.setColumn("merkmal");
        condition.setOperator("=");
        condition.setValue("");
        addFilterToUI(condition);
        filterConditions.add(condition);
    }

    private void addLeboIdentnrFilter() {
        if (!hasColumn("identnr")) {
            showAlert("Spalte 'identnr' nicht gefunden.");
            return;
        }

        FilterCondition condition = new FilterCondition();
        condition.setColumn("identnr");
        condition.setOperator("=");
        condition.setValue("");
        addFilterToUI(condition);
        filterConditions.add(condition);
    }

    private void addFilterToUI(FilterCondition condition) {
        HBox filterBox = new HBox(8);
        filterBox.getStyleClass().add("filter-row");

        // Column selection
        ComboBox<String> columnCombo = new ComboBox<>();
        if (availableColumns != null) {
            columnCombo.setItems(FXCollections.observableArrayList(
                availableColumns.stream().map(ColumnInfo::getName).collect(Collectors.toList())
            ));
        }
        columnCombo.setValue(condition.getColumn());
        columnCombo.setPrefWidth(120);

        // Operator selection
        ComboBox<String> operatorCombo = new ComboBox<>();
        operatorCombo.setItems(FXCollections.observableArrayList("=", "!=", "LIKE", "NOT LIKE", ">", "<", ">=", "<="));
        operatorCombo.setValue(condition.getOperator());
        operatorCombo.setPrefWidth(80);

        // Value input
        TextField valueField = new TextField(condition.getValue());
        valueField.setPrefWidth(150);

        // Logic connector (AND/OR)
        ComboBox<String> logicCombo = new ComboBox<>();
        logicCombo.setItems(FXCollections.observableArrayList("AND", "OR"));
        logicCombo.setValue(condition.getLogic());
        logicCombo.setPrefWidth(60);
        logicCombo.setVisible(filterConditions.size() > 0); // Hide for first condition

        // Remove button
        Button removeBtn = new Button("✖");
        removeBtn.getStyleClass().add("danger-button");
        removeBtn.setPrefWidth(30);

        // Event handlers
        columnCombo.setOnAction(e -> {
            condition.setColumn(columnCombo.getValue());
            filterConditions.set(filterConditions.indexOf(condition), condition);
        });

        operatorCombo.setOnAction(e -> {
            condition.setOperator(operatorCombo.getValue());
            filterConditions.set(filterConditions.indexOf(condition), condition);
        });

        valueField.textProperty().addListener((obs, oldVal, newVal) -> {
            condition.setValue(newVal);
            filterConditions.set(filterConditions.indexOf(condition), condition);
        });

        logicCombo.setOnAction(e -> {
            condition.setLogic(logicCombo.getValue());
            filterConditions.set(filterConditions.indexOf(condition), condition);
        });

        removeBtn.setOnAction(e -> {
            filterConditions.remove(condition);
            filtersContainer.getChildren().remove(filterBox);
            updateLogicVisibility();
        });

        filterBox.getChildren().addAll(columnCombo, operatorCombo, valueField, logicCombo, removeBtn);
        filtersContainer.getChildren().add(filterBox);
        updateLogicVisibility();
    }

    private void updateLogicVisibility() {
        for (int i = 0; i < filtersContainer.getChildren().size(); i++) {
            HBox filterBox = (HBox) filtersContainer.getChildren().get(i);
            ComboBox<String> logicCombo = (ComboBox<String>) filterBox.getChildren().get(3);
            logicCombo.setVisible(i > 0); // Hide logic for first filter
        }
    }

    private void clearAllFilters() {
        filterConditions.clear();
        filtersContainer.getChildren().clear();
    }

    private void updateSqlPreview() {
        if (filterConditions.isEmpty()) {
            sqlPreview.setText("-- Keine Filter aktiv");
            return;
        }

        StringBuilder sql = new StringBuilder("WHERE ");
        for (int i = 0; i < filterConditions.size(); i++) {
            FilterCondition condition = filterConditions.get(i);

            if (i > 0) {
                sql.append(" ").append(condition.getLogic()).append(" ");
            }

            sql.append(condition.getColumn())
               .append(" ")
               .append(condition.getOperator())
               .append(" ");

            // Add quotes for string values in LIKE operations
            String value = condition.getValue();
            if (condition.getOperator().contains("LIKE") && !value.startsWith("'")) {
                sql.append("'%").append(value).append("%'");
            } else if (!value.matches("\\d+") && !value.startsWith("'")) {
                sql.append("'").append(value).append("'");
            } else {
                sql.append(value);
            }
        }

        sqlPreview.setText(sql.toString());

        if (onSqlChanged != null) {
            onSqlChanged.accept(sql.toString());
        }
    }

    private void updateStatus() {
        if (filterConditions.isEmpty()) {
            statusLabel.setText("Keine Filter aktiv");
        } else {
            statusLabel.setText(filterConditions.size() + " Filter aktiv");
        }
    }

    private boolean hasColumn(String columnName) {
        return availableColumns != null &&
               availableColumns.stream().anyMatch(col -> col.getName().equalsIgnoreCase(columnName));
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Filter Builder");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Public API
    public void setAvailableColumns(List<ColumnInfo> columns) {
        this.availableColumns = columns;
        updateStatus();
    }

    public String getWhereClause() {
        if (filterConditions.isEmpty()) {
            return "";
        }
        return sqlPreview.getText();
    }

    public void setOnSqlChanged(Consumer<String> callback) {
        this.onSqlChanged = callback;
    }

    public Node getNode() {
        return container;
    }

    // Data model for filter conditions
    public static class FilterCondition {
        private String column = "";
        private String operator = "=";
        private String value = "";
        private String logic = "AND";

        // Getters and setters
        public String getColumn() { return column; }
        public void setColumn(String column) { this.column = column; }

        public String getOperator() { return operator; }
        public void setOperator(String operator) { this.operator = operator; }

        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }

        public String getLogic() { return logic; }
        public void setLogic(String logic) { this.logic = logic; }
    }
}