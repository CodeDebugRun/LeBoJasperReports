package de.reports.gui;

import de.reports.database.DatabaseConnectionInfo;
import de.reports.database.DatabaseManager;
import de.reports.i18n.MessageBundle;
import de.reports.utils.ConfigManager;
import de.reports.utils.FileUtils;
import de.reports.utils.SecurityUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class DatabaseConnectionPanel {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnectionPanel.class);

    private VBox container;
    private ComboBox<String> templateComboBox;
    private ComboBox<String> typeComboBox;
    private TextField nameField;
    private TextField hostField;
    private TextField portField;
    private TextField databaseField;
    private TextField usernameField;
    private PasswordField passwordField;
    private Button testButton;
    private Button saveButton;
    private Button loadButton;
    private Label statusLabel;

    private DatabaseManager databaseManager;
    private Consumer<Boolean> onConnectionChanged;
    private List<DatabaseConnectionInfo> savedConnections;

    public DatabaseConnectionPanel() {
        try {
            logger.info("Initializing DatabaseConnectionPanel...");
            databaseManager = new DatabaseManager();
            savedConnections = new ArrayList<>();
            initializeComponents();
            setupLayout();
            setupEventHandlers();
            loadSavedConnections();
            loadDatabaseTemplates();
            logger.info("DatabaseConnectionPanel initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize DatabaseConnectionPanel", e);
            // Create a minimal UI to show error
            container = new VBox();
            container.setPadding(new Insets(20));
            Label errorLabel = new Label("Fehler beim Laden des Datenbankpanels: " + e.getMessage());
            errorLabel.setStyle("-fx-text-fill: red;");
            container.getChildren().add(errorLabel);
        }
    }

    private void initializeComponents() {
        container = new VBox();
        container.setPadding(new Insets(20));
        container.setSpacing(15);

        // Title
        Label titleLabel = new Label(MessageBundle.getMessage("db.connection.title"));
        titleLabel.getStyleClass().add("section-title");

        // Template selection
        HBox templateBox = new HBox(10);
        templateBox.setAlignment(Pos.CENTER_LEFT);
        Label templateLabel = new Label("Vorlage:");
        templateLabel.setPrefWidth(120);
        templateComboBox = new ComboBox<>();
        templateComboBox.setPrefWidth(250);
        templateComboBox.setPromptText("Wählen Sie eine Vorlage...");
        templateBox.getChildren().addAll(templateLabel, templateComboBox);

        // Connection details
        GridPane gridPane = new GridPane();
        gridPane.setHgap(10);
        gridPane.setVgap(10);
        gridPane.setPadding(new Insets(10));

        // Connection name
        Label nameLabel = new Label(MessageBundle.getMessage("db.connection.name") + ":");
        nameField = new TextField();
        nameField.setPromptText("Meine Verbindung");
        gridPane.add(nameLabel, 0, 0);
        gridPane.add(nameField, 1, 0);

        // Database type
        Label typeLabel = new Label(MessageBundle.getMessage("db.type") + ":");
        typeComboBox = new ComboBox<>();
        typeComboBox.getItems().addAll("MySQL", "PostgreSQL", "SQL Server", "Oracle");
        typeComboBox.setValue("SQL Server");
        gridPane.add(typeLabel, 0, 1);
        gridPane.add(typeComboBox, 1, 1);

        // Host
        Label hostLabel = new Label(MessageBundle.getMessage("db.host") + ":");
        hostField = new TextField("localhost");
        hostField.setPromptText("localhost oder IP-Adresse");
        gridPane.add(hostLabel, 0, 2);
        gridPane.add(hostField, 1, 2);

        // Port
        Label portLabel = new Label(MessageBundle.getMessage("db.port") + ":");
        portField = new TextField("1433");
        portField.setPromptText("Port-Nummer");
        gridPane.add(portLabel, 0, 3);
        gridPane.add(portField, 1, 3);

        // Database
        Label databaseLabel = new Label(MessageBundle.getMessage("db.database") + ":");
        databaseField = new TextField();
        databaseField.setPromptText("Datenbankname");
        gridPane.add(databaseLabel, 0, 4);
        gridPane.add(databaseField, 1, 4);

        // Username
        Label usernameLabel = new Label(MessageBundle.getMessage("db.username") + ":");
        usernameField = new TextField();
        usernameField.setPromptText("Benutzername");
        gridPane.add(usernameLabel, 0, 5);
        gridPane.add(usernameField, 1, 5);

        // Password
        Label passwordLabel = new Label(MessageBundle.getMessage("db.password") + ":");
        passwordField = new PasswordField();
        passwordField.setPromptText("Passwort");
        gridPane.add(passwordLabel, 0, 6);
        gridPane.add(passwordField, 1, 6);

        // Make input fields expand
        ColumnConstraints col1 = new ColumnConstraints(120);
        ColumnConstraints col2 = new ColumnConstraints(250);
        col2.setHgrow(Priority.ALWAYS);
        gridPane.getColumnConstraints().addAll(col1, col2);

        // Buttons
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_LEFT);

        testButton = new Button(MessageBundle.getMessage("db.test.connection"));
        testButton.getStyleClass().add("primary-button");

        saveButton = new Button(MessageBundle.getMessage("db.save.connection"));
        loadButton = new Button(MessageBundle.getMessage("db.load.connection"));

        buttonBox.getChildren().addAll(testButton, saveButton, loadButton);

        // Status label
        statusLabel = new Label();
        statusLabel.getStyleClass().add("status-text");

        container.getChildren().addAll(titleLabel, templateBox, gridPane, buttonBox, statusLabel);
    }

    private void setupLayout() {
        container.getStyleClass().add("content-panel");
    }

    private void setupEventHandlers() {
        // Template selection
        templateComboBox.setOnAction(e -> {
            String selectedTemplate = templateComboBox.getValue();
            if (selectedTemplate != null && !selectedTemplate.isEmpty()) {
                loadTemplate(selectedTemplate);
            }
        });

        // Database type change
        typeComboBox.setOnAction(e -> {
            String selectedType = typeComboBox.getValue();
            if (selectedType != null) {
                updatePortForDatabaseType(selectedType);
            }
        });

        // Test connection
        testButton.setOnAction(e -> testConnection());

        // Save connection
        saveButton.setOnAction(e -> saveConnection());

        // Load connection
        loadButton.setOnAction(e -> loadConnection());

        // Auto-fill connection name when other fields change
        hostField.textProperty().addListener((obs, oldVal, newVal) -> updateConnectionName());
        databaseField.textProperty().addListener((obs, oldVal, newVal) -> updateConnectionName());
        typeComboBox.valueProperty().addListener((obs, oldVal, newVal) -> updateConnectionName());
    }

    private void loadDatabaseTemplates() {
        try {
            logger.debug("Loading database templates...");
            ConfigManager config = ConfigManager.getInstance();
            Map<String, String> templates = config.getAllDatabaseTemplateNames();

            if (templateComboBox != null) {
                templateComboBox.getItems().clear();
                templateComboBox.getItems().add(""); // Empty option

                if (templates != null && !templates.isEmpty()) {
                    templateComboBox.getItems().addAll(templates.keySet());
                    logger.info("Loaded {} database templates", templates.size());
                } else {
                    logger.warn("No database templates found");
                }
            }
        } catch (Exception e) {
            logger.error("Failed to load database templates", e);
            if (templateComboBox != null) {
                templateComboBox.getItems().clear();
                templateComboBox.getItems().add(""); // Empty option to prevent crash
            }
        }
    }

    private void loadTemplate(String templateName) {
        ConfigManager config = ConfigManager.getInstance();
        Map<String, Object> template = config.getDatabaseTemplate(templateName);

        if (!template.isEmpty()) {
            nameField.setText(templateName);
            typeComboBox.setValue(capitalizeFirst((String) template.get("type")));
            hostField.setText((String) template.get("host"));
            portField.setText(String.valueOf(template.get("port")));
            databaseField.setText((String) template.get("database"));

            // Load username and password from template if available
            if (template.containsKey("username")) {
                usernameField.setText((String) template.get("username"));
            }
            if (template.containsKey("password")) {
                passwordField.setText((String) template.get("password"));
            }

            // Special handling for LEBO database template
            if ("LEBO Database (from existing project)".equals(templateName)) {
                statusLabel.setText("Vorlage geladen: LEBO-Datenbank aus Ihrem bestehenden Projekt (mit Anmeldedaten)");
                statusLabel.getStyleClass().removeAll("error-text", "success-text");
                statusLabel.getStyleClass().add("info-text");
            } else {
                statusLabel.setText("Vorlage geladen: " + templateName);
                statusLabel.getStyleClass().removeAll("error-text", "success-text");
                statusLabel.getStyleClass().add("info-text");
            }
        }
    }

    private void updatePortForDatabaseType(String dbType) {
        ConfigManager config = ConfigManager.getInstance();
        int defaultPort;

        switch (dbType) {
            case "MySQL":
                defaultPort = config.getDefaultPort("mysql");
                break;
            case "PostgreSQL":
                defaultPort = config.getDefaultPort("postgresql");
                break;
            case "SQL Server":
                defaultPort = config.getDefaultPort("sqlserver");
                break;
            case "Oracle":
                defaultPort = config.getDefaultPort("oracle");
                break;
            default:
                defaultPort = 1433;
        }

        portField.setText(String.valueOf(defaultPort));
    }

    private void updateConnectionName() {
        if (nameField.getText().isEmpty() || isAutoGeneratedName(nameField.getText())) {
            String host = hostField.getText().isEmpty() ? "localhost" : hostField.getText();
            String database = databaseField.getText().isEmpty() ? "db" : databaseField.getText();
            String type = typeComboBox.getValue() != null ? typeComboBox.getValue() : "DB";
            nameField.setText(type + " - " + host + "/" + database);
        }
    }

    private boolean isAutoGeneratedName(String name) {
        return name.matches(".+ - .+/.+");
    }

    public void testConnection() {
        DatabaseConnectionInfo connInfo = createConnectionInfo();

        if (!connInfo.isValid()) {
            showError("Bitte füllen Sie alle erforderlichen Felder aus.");
            return;
        }

        statusLabel.setText("Verbindung wird getestet...");
        statusLabel.getStyleClass().removeAll("error-text", "success-text");
        statusLabel.getStyleClass().add("info-text");

        testButton.setDisable(true);

        // Test connection in background thread
        Thread testThread = new Thread(() -> {
            try {
                boolean success = databaseManager.testConnection(connInfo);

                Platform.runLater(() -> {
                    testButton.setDisable(false);

                    if (success) {
                        showSuccess("Verbindung erfolgreich!");

                        // Connect to database
                        if (databaseManager.connect(connInfo)) {
                            if (onConnectionChanged != null) {
                                onConnectionChanged.accept(true);
                            }
                        }
                    } else {
                        showError("Verbindung fehlgeschlagen. Bitte überprüfen Sie Ihre Eingaben.");
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    testButton.setDisable(false);
                    showError("Verbindungsfehler: " + e.getMessage());
                    logger.error("Connection test failed", e);
                });
            }
        });

        testThread.setDaemon(true);
        testThread.start();
    }

    private void saveConnection() {
        DatabaseConnectionInfo connInfo = createConnectionInfo();

        if (!connInfo.isValid()) {
            showError("Bitte füllen Sie alle erforderlichen Felder aus.");
            return;
        }

        // Encrypt password before saving
        connInfo.encryptPassword();

        try {
            String appDataDir = FileUtils.ensureAppDataDirectory();
            String connectionsFile = appDataDir + File.separator + "connections.json";

            // Load existing connections
            List<DatabaseConnectionInfo> connections = loadConnectionsFromFile(connectionsFile);

            // Add or update connection
            boolean updated = false;
            for (int i = 0; i < connections.size(); i++) {
                if (connections.get(i).getName().equals(connInfo.getName())) {
                    connections.set(i, connInfo);
                    updated = true;
                    break;
                }
            }

            if (!updated) {
                connections.add(connInfo);
            }

            // Save to file
            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(connections);
            FileUtils.writeToFile(connectionsFile, json);

            showSuccess("Verbindung gespeichert: " + connInfo.getName());
            savedConnections = connections;

        } catch (Exception e) {
            showError("Fehler beim Speichern: " + e.getMessage());
            logger.error("Failed to save connection", e);
        }
    }

    private void loadConnection() {
        if (savedConnections.isEmpty()) {
            loadSavedConnections();
        }

        if (savedConnections.isEmpty()) {
            showError("Keine gespeicherten Verbindungen gefunden.");
            return;
        }

        // Create selection dialog
        ChoiceDialog<DatabaseConnectionInfo> dialog = new ChoiceDialog<>();
        dialog.setTitle("Verbindung laden");
        dialog.setHeaderText("Wählen Sie eine gespeicherte Verbindung:");
        dialog.setContentText("Verbindung:");

        dialog.getItems().addAll(savedConnections);
        dialog.setSelectedItem(savedConnections.get(0));

        dialog.showAndWait().ifPresent(selected -> {
            fillFieldsFromConnection(selected);
            showSuccess("Verbindung geladen: " + selected.getName());
        });
    }

    private void loadSavedConnections() {
        try {
            String appDataDir = FileUtils.ensureAppDataDirectory();
            String connectionsFile = appDataDir + File.separator + "connections.json";

            if (FileUtils.fileExists(connectionsFile)) {
                savedConnections = loadConnectionsFromFile(connectionsFile);
                logger.info("Loaded {} saved connections", savedConnections.size());
            }
        } catch (Exception e) {
            logger.error("Failed to load saved connections", e);
            savedConnections = new ArrayList<>();
        }
    }

    private List<DatabaseConnectionInfo> loadConnectionsFromFile(String filePath) {
        try {
            String json = FileUtils.readFromFile(filePath);
            if (json != null && !json.trim().isEmpty()) {
                ObjectMapper mapper = new ObjectMapper();
                return mapper.readValue(json,
                    mapper.getTypeFactory().constructCollectionType(List.class, DatabaseConnectionInfo.class));
            }
        } catch (Exception e) {
            logger.error("Failed to load connections from file: {}", filePath, e);
        }
        return new ArrayList<>();
    }

    private void fillFieldsFromConnection(DatabaseConnectionInfo connInfo) {
        nameField.setText(connInfo.getName());
        typeComboBox.setValue(capitalizeFirst(connInfo.getType()));
        hostField.setText(connInfo.getHost());
        portField.setText(String.valueOf(connInfo.getPort()));
        databaseField.setText(connInfo.getDatabase());
        usernameField.setText(connInfo.getUsername());
        passwordField.setText(connInfo.getDecryptedPassword());
    }

    private DatabaseConnectionInfo createConnectionInfo() {
        DatabaseConnectionInfo connInfo = new DatabaseConnectionInfo();
        connInfo.setName(nameField.getText());
        connInfo.setType(typeComboBox.getValue().toLowerCase());
        connInfo.setHost(hostField.getText());

        try {
            connInfo.setPort(Integer.parseInt(portField.getText()));
        } catch (NumberFormatException e) {
            connInfo.setPort(1433);
        }

        connInfo.setDatabase(databaseField.getText());
        connInfo.setUsername(usernameField.getText());
        connInfo.setPassword(passwordField.getText());

        return connInfo;
    }

    private String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private void showSuccess(String message) {
        statusLabel.setText(message);
        statusLabel.getStyleClass().removeAll("error-text", "info-text");
        statusLabel.getStyleClass().add("success-text");
    }

    private void showError(String message) {
        statusLabel.setText(message);
        statusLabel.getStyleClass().removeAll("success-text", "info-text");
        statusLabel.getStyleClass().add("error-text");
    }

    // Getters and setters
    public Node getNode() {
        return container;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public void setOnConnectionChanged(Consumer<Boolean> onConnectionChanged) {
        this.onConnectionChanged = onConnectionChanged;
    }

    public boolean isConnected() {
        return databaseManager.isConnected();
    }
}