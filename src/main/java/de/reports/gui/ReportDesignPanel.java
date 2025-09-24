package de.reports.gui;

import de.reports.database.ColumnInfo;
import de.reports.i18n.MessageBundle;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Consumer;

public class ReportDesignPanel {
    private VBox container;
    private Consumer<Boolean> onReportDesigned;

    public ReportDesignPanel() {
        initializeComponents();
        setupLayout();
    }

    private void initializeComponents() {
        container = new VBox();
        container.setPadding(new Insets(20));
        container.setSpacing(15);

        Label titleLabel = new Label(MessageBundle.getMessage("report.design.title"));
        titleLabel.getStyleClass().add("section-title");

        Label placeholderLabel = new Label("Report Design Panel - To be implemented");
        placeholderLabel.getStyleClass().add("placeholder-text");

        container.getChildren().addAll(titleLabel, placeholderLabel);
    }

    private void setupLayout() {
        container.getStyleClass().add("content-panel");
    }

    public void setSelectedTable(String tableName, List<ColumnInfo> columns) {
        // Implementation placeholder
        if (onReportDesigned != null) {
            onReportDesigned.accept(true);
        }
    }

    public Object getReportConfiguration() {
        return new Object(); // Placeholder
    }

    public void setOnReportDesigned(Consumer<Boolean> onReportDesigned) {
        this.onReportDesigned = onReportDesigned;
    }

    public Node getNode() {
        return container;
    }
}