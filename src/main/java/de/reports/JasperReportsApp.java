package de.reports;

import de.reports.gui.MainWindow;
import de.reports.i18n.MessageBundle;
import de.reports.utils.ConfigManager;
import de.reports.utils.FileUtils;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

public class JasperReportsApp extends Application {
    private static final Logger logger = LoggerFactory.getLogger(JasperReportsApp.class);

    public static void main(String[] args) {
        logger.info("Starting JasperReports Desktop Application...");

        try {
            // Initialize application data directory&&
            String appDataDir = FileUtils.ensureAppDataDirectory();
            logger.info("Application data directory: {}", appDataDir);

            // Clean temp directory
            FileUtils.cleanTempDirectory();

            // Set default locale
            ConfigManager config = ConfigManager.getInstance();
            String defaultLanguage = config.getDefaultLanguage();
            MessageBundle.setLanguage(defaultLanguage);
            Locale.setDefault(new Locale(defaultLanguage));

            logger.info("Application initialized successfully");

            // Launch JavaFX application
            launch(args);

        } catch (Exception e) {
            logger.error("Failed to start application", e);
            showErrorDialog("Anwendungsfehler", "Die Anwendung konnte nicht gestartet werden: " + e.getMessage());
            System.exit(1);
        }
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            logger.info("Initializing JavaFX application...");

            // Prevent automatic exit when all windows are closed
            Platform.setImplicitExit(false);

            // Set up global exception handler
            Thread.setDefaultUncaughtExceptionHandler((thread, exception) -> {
                logger.error("Uncaught exception in thread {}", thread.getName(), exception);
                Platform.runLater(() -> {
                    showErrorDialog(
                        MessageBundle.getMessage("error.unexpected"),
                        MessageBundle.getMessage("error.unexpected.message") + ": " + exception.getMessage()
                    );
                });
            });

            // Create and show main window
            MainWindow mainWindow = new MainWindow();
            mainWindow.show(primaryStage);

            logger.info("JavaFX application started successfully");

        } catch (Exception e) {
            logger.error("Failed to start JavaFX application", e);
            showErrorDialog(
                MessageBundle.getMessage("error.startup"),
                MessageBundle.getMessage("error.startup.message") + ": " + e.getMessage()
            );
            Platform.exit();
        }
    }

    @Override
    public void stop() throws Exception {
        logger.info("Shutting down application...");

        try {
            // Cleanup tasks
            // Any cleanup code would go here

            logger.info("Application shutdown complete");
        } catch (Exception e) {
            logger.error("Error during application shutdown", e);
        }

        super.stop();
    }

    private static void showErrorDialog(String title, String message) {
        try {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        } catch (Exception e) {
            logger.error("Failed to show error dialog", e);
            System.err.println("ERROR: " + title + " - " + message);
        }
    }
}