package de.reports.gui;

import de.reports.database.ColumnInfo;
import de.reports.gui.components.ColumnSelectionComponent;
import de.reports.gui.components.FilterBuilderComponent;
import de.reports.i18n.MessageBundle;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Consumer;

/**
 * Report Design Panel - Main coordinator for report configuration
 * Integrates column selection, filters, sorting, and grouping components
 * Keeps code under 1000 lines by delegating to specialized components
 */
public class ReportDesignPanel {

    private VBox container;
    private ScrollPane scrollPane;
    private Consumer<Boolean> onReportDesigned;
    private Consumer<String> onFilterChanged;

    // Component sections
    private ColumnSelectionComponent columnSelection;
    private FilterBuilderComponent filterBuilder;
    private Label statusLabel;

    // Current state
    private String currentTableName;
    private List<ColumnInfo> currentColumns;

    public ReportDesignPanel() {
        initializeComponents();
        setupLayout();
        setupEventHandlers();
    }

    private void initializeComponents() {
        container = new VBox(15);
        container.setPadding(new Insets(20));

        // Title
        Label titleLabel = new Label(MessageBundle.getMessage("report.design.title"));
        titleLabel.getStyleClass().addAll("section-title", "h2");

        // Instructions
        Label instructionsLabel = new Label("Konfigurieren Sie Ihren Bericht durch Auswahl der gewünschten Spalten.");
        instructionsLabel.getStyleClass().add("instructions-text");
        instructionsLabel.setWrapText(true);

        // Column Selection Component
        columnSelection = new ColumnSelectionComponent();

        // Separator
        Separator separator1 = new Separator();

        // Filter Builder Component
        filterBuilder = new FilterBuilderComponent();

        // Separator for future components
        Separator separator2 = new Separator();

        // Placeholder for future components (Sort, Group)
        Label futureLabel = new Label("Sortierung und Gruppierung folgen...");
        futureLabel.getStyleClass().add("placeholder-text");

        // Status
        statusLabel = new Label();
        statusLabel.getStyleClass().add("status-text");

        container.getChildren().addAll(
            titleLabel,
            instructionsLabel,
            columnSelection.getNode(),
            separator1,
            filterBuilder.getNode(),
            separator2,
            futureLabel,
            statusLabel
        );

        // Wrap in scroll pane for long content
        scrollPane = new ScrollPane(container);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("scroll-pane");
    }

    private void setupLayout() {
        container.getStyleClass().add("content-panel");
        scrollPane.getStyleClass().add("report-design-panel");
    }

    private void setupEventHandlers() {
        // Listen to column selection changes
        columnSelection.setOnSelectionChanged(selectedColumns -> {
            updateStatus();
            checkReportReadiness();
        });

        // Listen to filter changes
        filterBuilder.setOnSqlChanged(sqlClause -> {
            updateStatus();
            checkReportReadiness();

            // Notify MainWindow about filter changes for TableSelectionPanel
            if (onFilterChanged != null && currentTableName != null) {
                onFilterChanged.accept(sqlClause);
            }
        });
    }

    /**
     * Set the selected table and its columns for report design
     */
    public void setSelectedTable(String tableName, List<ColumnInfo> columns) {
        this.currentTableName = tableName;
        this.currentColumns = columns;

        // Pass to column selection component
        columnSelection.setAvailableColumns(tableName, columns);

        // Pass to filter builder component
        filterBuilder.setAvailableColumns(columns);

        updateStatus();
        checkReportReadiness();
    }

    private void updateStatus() {
        if (currentTableName != null) {
            List<String> selectedColumns = columnSelection.getSelectedColumns();
            statusLabel.setText(String.format(
                "Tabelle: %s | %d Spalten ausgewählt",
                currentTableName,
                selectedColumns.size()
            ));
        } else {
            statusLabel.setText("Keine Tabelle ausgewählt");
        }
    }

    private void checkReportReadiness() {
        boolean isReady = currentTableName != null &&
                         !columnSelection.getSelectedColumns().isEmpty();

        if (onReportDesigned != null) {
            onReportDesigned.accept(isReady);
        }
    }

    /**
     * Get the current report configuration
     */
    public ReportConfiguration getReportConfiguration() {
        if (currentTableName == null) {
            return null;
        }

        return new ReportConfiguration(
            currentTableName,
            columnSelection.getSelectedColumns(),
            filterBuilder.getWhereClause()
        );
    }

    public void setOnReportDesigned(Consumer<Boolean> onReportDesigned) {
        this.onReportDesigned = onReportDesigned;
    }

    public void setOnFilterChanged(Consumer<String> onFilterChanged) {
        this.onFilterChanged = onFilterChanged;
    }

    public Node getNode() {
        return scrollPane;
    }

    /**
     * Simple data class for report configuration
     * Will be expanded as we add more components
     */
    public static class ReportConfiguration {
        private final String tableName;
        private final List<String> selectedColumns;
        private final String whereClause;

        public ReportConfiguration(String tableName, List<String> selectedColumns, String whereClause) {
            this.tableName = tableName;
            this.selectedColumns = selectedColumns;
            this.whereClause = whereClause;
        }

        public String getTableName() { return tableName; }
        public List<String> getSelectedColumns() { return selectedColumns; }
        public String getWhereClause() { return whereClause; }
    }
}