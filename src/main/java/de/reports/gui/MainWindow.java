package de.reports.gui;

import de.reports.i18n.MessageBundle;
import de.reports.utils.ConfigManager;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainWindow {
    private static final Logger logger = LoggerFactory.getLogger(MainWindow.class);

    private Stage primaryStage;
    private BorderPane rootPane;
    private TabPane tabPane;
    private MenuBar menuBar;
    private ToolBar toolBar;
    private StatusBar statusBar;

    // Panels
    private DatabaseConnectionPanel databasePanel;
    private TableSelectionPanel tablePanel;
    private ReportDesignPanel reportPanel;
    private FormatStylePanel formatPanel;
    private ExportPreviewPanel exportPanel;

    public MainWindow() {
        initializeComponents();
        setupLayout();
        setupEventHandlers();
    }

    private void initializeComponents() {
        logger.info("Initializing main window components...");

        // Create main layout
        rootPane = new BorderPane();

        // Create menu bar
        createMenuBar();

        // Create toolbar
        createToolBar();

        // Create tab pane for workflow steps
        createTabPane();

        // Create status bar
        createStatusBar();

        // Initialize panels
        databasePanel = new DatabaseConnectionPanel();
        tablePanel = new TableSelectionPanel();
        reportPanel = new ReportDesignPanel();
        formatPanel = new FormatStylePanel();
        exportPanel = new ExportPreviewPanel();

        logger.info("Main window components initialized");
    }

    private void createMenuBar() {
        menuBar = new MenuBar();

        // File Menu
        Menu fileMenu = new Menu(MessageBundle.getMessage("menu.file"));
        MenuItem newItem = new MenuItem(MessageBundle.getMessage("menu.file.new"));
        MenuItem openItem = new MenuItem(MessageBundle.getMessage("menu.file.open"));
        MenuItem saveItem = new MenuItem(MessageBundle.getMessage("menu.file.save"));
        MenuItem saveAsItem = new MenuItem(MessageBundle.getMessage("menu.file.saveAs"));
        MenuItem exitItem = new MenuItem(MessageBundle.getMessage("menu.file.exit"));

        fileMenu.getItems().addAll(newItem, openItem, new SeparatorMenuItem(),
                                  saveItem, saveAsItem, new SeparatorMenuItem(), exitItem);

        // Edit Menu
        Menu editMenu = new Menu(MessageBundle.getMessage("menu.edit"));

        // View Menu
        Menu viewMenu = new Menu(MessageBundle.getMessage("menu.view"));
        Menu languageMenu = new Menu(MessageBundle.getMessage("menu.view.language"));
        MenuItem germanItem = new MenuItem("Deutsch");
        MenuItem englishItem = new MenuItem("English");
        languageMenu.getItems().addAll(germanItem, englishItem);

        Menu themeMenu = new Menu(MessageBundle.getMessage("menu.view.theme"));
        MenuItem lightThemeItem = new MenuItem("Light");
        MenuItem darkThemeItem = new MenuItem("Dark");
        themeMenu.getItems().addAll(lightThemeItem, darkThemeItem);

        viewMenu.getItems().addAll(languageMenu, themeMenu);

        // Reports Menu
        Menu reportsMenu = new Menu(MessageBundle.getMessage("menu.reports"));

        // Help Menu
        Menu helpMenu = new Menu(MessageBundle.getMessage("menu.help"));
        MenuItem aboutItem = new MenuItem(MessageBundle.getMessage("menu.help.about"));
        helpMenu.getItems().add(aboutItem);

        menuBar.getMenus().addAll(fileMenu, editMenu, viewMenu, reportsMenu, helpMenu);

        // Event handlers
        exitItem.setOnAction(e -> handleExit());
        aboutItem.setOnAction(e -> showAboutDialog());
        germanItem.setOnAction(e -> changeLanguage("de"));
        englishItem.setOnAction(e -> changeLanguage("en"));
    }

    private void createToolBar() {
        toolBar = new ToolBar();

        Button connectBtn = new Button(MessageBundle.getMessage("db.test.connection"));
        Button refreshBtn = new Button(MessageBundle.getMessage("table.refresh"));
        Button previewBtn = new Button(MessageBundle.getMessage("button.preview"));
        Button exportBtn = new Button(MessageBundle.getMessage("button.export"));

        toolBar.getItems().addAll(connectBtn, new Separator(), refreshBtn, new Separator(),
                                 previewBtn, exportBtn);

        // Event handlers
        connectBtn.setOnAction(e -> databasePanel.testConnection());
        refreshBtn.setOnAction(e -> tablePanel.refreshTables());
        previewBtn.setOnAction(e -> exportPanel.previewReport());
        exportBtn.setOnAction(e -> exportPanel.exportReport());
    }

    private void createTabPane() {
        tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // Create tabs for workflow steps
        Tab dbTab = new Tab(MessageBundle.getMessage("db.connection"));
        Tab tableTab = new Tab(MessageBundle.getMessage("table.selection"));
        Tab designTab = new Tab(MessageBundle.getMessage("report.design"));
        Tab formatTab = new Tab(MessageBundle.getMessage("format.style"));
        Tab exportTab = new Tab(MessageBundle.getMessage("export.preview"));

        // Initially disable all tabs except database connection
        tableTab.setDisable(true);
        designTab.setDisable(true);
        formatTab.setDisable(true);
        exportTab.setDisable(true);

        tabPane.getTabs().addAll(dbTab, tableTab, designTab, formatTab, exportTab);
    }

    private void createStatusBar() {
        statusBar = new StatusBar();
        statusBar.setText(MessageBundle.getMessage("status.ready"));
    }

    private void setupLayout() {
        // Set content for tabs
        Tab dbTab = tabPane.getTabs().get(0);
        Tab tableTab = tabPane.getTabs().get(1);
        Tab designTab = tabPane.getTabs().get(2);
        Tab formatTab = tabPane.getTabs().get(3);
        Tab exportTab = tabPane.getTabs().get(4);

        dbTab.setContent(databasePanel.getNode());
        tableTab.setContent(tablePanel.getNode());
        designTab.setContent(reportPanel.getNode());
        formatTab.setContent(formatPanel.getNode());
        exportTab.setContent(exportPanel.getNode());

        // Layout main window
        VBox topBox = new VBox(menuBar, toolBar);
        rootPane.setTop(topBox);
        rootPane.setCenter(tabPane);
        rootPane.setBottom(statusBar.getNode());
    }

    private void setupEventHandlers() {
        // Set up communication between panels
        databasePanel.setOnConnectionChanged(connected -> {
            Tab tableTab = tabPane.getTabs().get(1);
            tableTab.setDisable(!connected); // Table selection tab
            if (connected) {
                tablePanel.setDatabaseManager(databasePanel.getDatabaseManager());
                updateStatus(MessageBundle.getMessage("success.connection.established"));
            } else {
                updateStatus(MessageBundle.getMessage("status.ready"));
            }
        });

        tablePanel.setOnTableSelected(tableName -> {
            if (tableName != null && !tableName.isEmpty()) {
                Tab designTab = tabPane.getTabs().get(2);
                designTab.setDisable(false); // Report design tab
                reportPanel.setSelectedTable(tableName, tablePanel.getTableColumns());
                updateStatus(MessageBundle.getMessage("status.table.selected", tableName));
            }
        });

        reportPanel.setOnReportDesigned(designed -> {
            Tab formatTab = tabPane.getTabs().get(3);
            formatTab.setDisable(!designed); // Format style tab
            if (designed) {
                formatPanel.setReportConfiguration(reportPanel.getReportConfiguration());
                updateStatus(MessageBundle.getMessage("status.report.designed"));
            }
        });

        // Listen for filter changes from ReportDesignPanel
        reportPanel.setOnFilterChanged(whereClause -> {
            String currentTable = reportPanel.getReportConfiguration() != null ?
                reportPanel.getReportConfiguration().getTableName() : null;

            if (currentTable != null && whereClause != null && !whereClause.trim().isEmpty()) {
                // Show filtered data in TableSelectionPanel
                tablePanel.showFilteredData(currentTable, whereClause);
                updateStatus("Filter angewendet - gefilterte Daten werden angezeigt");
            } else if (currentTable != null) {
                // Clear filters and show original data
                tablePanel.clearFilters();
                updateStatus("Filter entfernt - ursprüngliche Daten angezeigt");
            }
        });

        formatPanel.setOnFormatConfigured(configured -> {
            Tab exportTab = tabPane.getTabs().get(4);
            exportTab.setDisable(!configured); // Export preview tab
            if (configured) {
                exportPanel.setReportConfiguration(reportPanel.getReportConfiguration());
                exportPanel.setFormatConfiguration(formatPanel.getFormatConfiguration());
                exportPanel.setDatabaseManager(databasePanel.getDatabaseManager());
                updateStatus(MessageBundle.getMessage("status.format.configured"));
            }
        });
    }

    public void show(Stage stage) {
        this.primaryStage = stage;

        ConfigManager config = ConfigManager.getInstance();

        // Set window properties
        stage.setTitle(MessageBundle.getMessage("app.title"));
        stage.setWidth(config.getWindowWidth());
        stage.setHeight(config.getWindowHeight());
        stage.setMinWidth(config.getMinWindowWidth());
        stage.setMinHeight(config.getMinWindowHeight());

        // Set application icon (if available)
        try {
            var iconStream = getClass().getResourceAsStream("/icons/app-icon.png");
            if (iconStream != null) {
                Image icon = new Image(iconStream);
                stage.getIcons().add(icon);
                iconStream.close();
            } else {
                logger.debug("Application icon not found, using default");
            }
        } catch (Exception e) {
            logger.warn("Could not load application icon", e);
        }

        // Create and set scene
        Scene scene = new Scene(rootPane);

        // Load CSS styling
        try {
            String cssPath = getClass().getResource("/styles/application.css").toExternalForm();
            scene.getStylesheets().add(cssPath);
        } catch (Exception e) {
            logger.warn("Could not load CSS stylesheet", e);
        }

        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();

        // Set close request handler - prevent automatic close
        stage.setOnCloseRequest(e -> {
            e.consume(); // Prevent default close
            logger.debug("Close request intercepted");
            handleExit();
        });

        logger.info("Main window displayed");
    }

    private void updateStatus(String message) {
        Platform.runLater(() -> statusBar.setText(message));
    }

    private void changeLanguage(String languageCode) {
        MessageBundle.setLanguage(languageCode);
        // Refresh UI with new language
        refreshUITexts();
        updateStatus(MessageBundle.getMessage("status.language.changed"));
    }

    private void refreshUITexts() {
        // Update window title
        if (primaryStage != null) {
            primaryStage.setTitle(MessageBundle.getMessage("app.title"));
        }

        // Update menu items, tabs, etc.
        // This would involve updating all UI components with new text
        // For simplicity, we'll show a restart message
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(MessageBundle.getMessage("dialog.restart.title"));
        alert.setHeaderText(null);
        alert.setContentText(MessageBundle.getMessage("dialog.restart.message"));
        alert.showAndWait();
    }

    private void showAboutDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(MessageBundle.getMessage("dialog.about.title"));
        alert.setHeaderText(MessageBundle.getMessage("app.title"));
        alert.setContentText(MessageBundle.getMessage("dialog.about.text"));
        alert.showAndWait();
    }

    private void handleExit() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(MessageBundle.getMessage("dialog.confirm.exit"));
        alert.setHeaderText(null);
        alert.setContentText(MessageBundle.getMessage("dialog.confirm.exit"));

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                logger.info("Application exit requested by user");
                Platform.exit();
            }
        });
    }

    public void selectTab(int tabIndex) {
        if (tabIndex >= 0 && tabIndex < tabPane.getTabs().size()) {
            tabPane.getSelectionModel().select(tabIndex);
        }
    }

    public StatusBar getStatusBar() {
        return statusBar;
    }
}