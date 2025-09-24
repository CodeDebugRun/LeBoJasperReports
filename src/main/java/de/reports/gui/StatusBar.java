package de.reports.gui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

public class StatusBar {
    private HBox container;
    private Label statusLabel;
    private ProgressBar progressBar;
    private Label progressLabel;

    public StatusBar() {
        initializeComponents();
        setupLayout();
    }

    private void initializeComponents() {
        container = new HBox();
        container.setPadding(new Insets(5, 10, 5, 10));
        container.setAlignment(Pos.CENTER_LEFT);
        container.setSpacing(10);

        statusLabel = new Label("Bereit");
        statusLabel.getStyleClass().add("status-label");

        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        progressBar = new ProgressBar();
        progressBar.setPrefWidth(150);
        progressBar.setVisible(false);

        progressLabel = new Label();
        progressLabel.getStyleClass().add("progress-label");
        progressLabel.setVisible(false);

        container.getChildren().addAll(statusLabel, spacer, progressLabel, progressBar);
    }

    private void setupLayout() {
        container.getStyleClass().add("status-bar");
    }

    public void setText(String text) {
        Platform.runLater(() -> statusLabel.setText(text));
    }

    public void showProgress(boolean show) {
        Platform.runLater(() -> {
            progressBar.setVisible(show);
            progressLabel.setVisible(show);
        });
    }

    public void setProgress(double progress) {
        Platform.runLater(() -> progressBar.setProgress(progress));
    }

    public void setProgressText(String text) {
        Platform.runLater(() -> progressLabel.setText(text));
    }

    public void setProgressIndeterminate(boolean indeterminate) {
        Platform.runLater(() -> {
            if (indeterminate) {
                progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
            } else {
                progressBar.setProgress(0);
            }
        });
    }

    public void startProgress(String text) {
        Platform.runLater(() -> {
            progressLabel.setText(text);
            progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
            showProgress(true);
        });
    }

    public void stopProgress() {
        Platform.runLater(() -> {
            showProgress(false);
            progressBar.setProgress(0);
            progressLabel.setText("");
        });
    }

    public Node getNode() {
        return container;
    }
}