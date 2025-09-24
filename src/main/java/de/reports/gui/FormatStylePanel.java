package de.reports.gui;

import de.reports.i18n.MessageBundle;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

public class FormatStylePanel {
    private VBox container;
    private Consumer<Boolean> onFormatConfigured;

    public FormatStylePanel() {
        initializeComponents();
        setupLayout();
    }

    private void initializeComponents() {
        container = new VBox();
        container.setPadding(new Insets(20));
        container.setSpacing(15);

        Label titleLabel = new Label(MessageBundle.getMessage("format.style"));
        titleLabel.getStyleClass().add("section-title");

        Label placeholderLabel = new Label("Format & Style Panel - To be implemented");
        placeholderLabel.getStyleClass().add("placeholder-text");

        container.getChildren().addAll(titleLabel, placeholderLabel);
    }

    private void setupLayout() {
        container.getStyleClass().add("content-panel");
    }

    public void setReportConfiguration(Object reportConfiguration) {
        // Implementation placeholder
        if (onFormatConfigured != null) {
            onFormatConfigured.accept(true);
        }
    }

    public Object getFormatConfiguration() {
        return new Object(); // Placeholder
    }

    public void setOnFormatConfigured(Consumer<Boolean> onFormatConfigured) {
        this.onFormatConfigured = onFormatConfigured;
    }

    public Node getNode() {
        return container;
    }
}