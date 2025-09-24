package de.reports;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class SimpleTest extends Application {

    @Override
    public void start(Stage primaryStage) {
        VBox root = new VBox();
        Label label = new Label("JasperReports Desktop - Test");
        root.getChildren().add(label);

        Scene scene = new Scene(root, 300, 200);
        primaryStage.setTitle("Test");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}