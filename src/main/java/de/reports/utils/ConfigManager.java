package de.reports.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {
    private static final Logger logger = LoggerFactory.getLogger(ConfigManager.class);
    private static final String CONFIG_FILE = "/config/application.json";
    private static final String DB_TEMPLATES_FILE = "/config/database_templates.json";

    private static ConfigManager instance;
    private static JsonNode config;
    private static JsonNode dbTemplates;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private ConfigManager() {
        loadConfiguration();
        loadDatabaseTemplates();
    }

    public static synchronized ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }

    private void loadConfiguration() {
        try (InputStream inputStream = getClass().getResourceAsStream(CONFIG_FILE)) {
            if (inputStream != null) {
                config = objectMapper.readTree(inputStream);
                logger.info("Configuration loaded successfully");
            } else {
                logger.error("Configuration file not found: {}", CONFIG_FILE);
                throw new RuntimeException("Configuration file not found");
            }
        } catch (IOException e) {
            logger.error("Failed to load configuration", e);
            throw new RuntimeException("Failed to load configuration", e);
        }
    }

    private void loadDatabaseTemplates() {
        try (InputStream inputStream = getClass().getResourceAsStream(DB_TEMPLATES_FILE)) {
            if (inputStream != null) {
                dbTemplates = objectMapper.readTree(inputStream);
                logger.info("Database templates loaded successfully");
            } else {
                logger.error("Database templates file not found: {}", DB_TEMPLATES_FILE);
            }
        } catch (IOException e) {
            logger.error("Failed to load database templates", e);
        }
    }

    public String getStringProperty(String path) {
        return getStringProperty(path, null);
    }

    public String getStringProperty(String path, String defaultValue) {
        try {
            String[] parts = path.split("\\.");
            JsonNode current = config;

            for (String part : parts) {
                current = current.get(part);
                if (current == null) {
                    return defaultValue;
                }
            }

            return current.asText();
        } catch (Exception e) {
            logger.warn("Failed to get property: {}", path, e);
            return defaultValue;
        }
    }

    public int getIntProperty(String path) {
        return getIntProperty(path, 0);
    }

    public int getIntProperty(String path, int defaultValue) {
        try {
            String[] parts = path.split("\\.");
            JsonNode current = config;

            for (String part : parts) {
                current = current.get(part);
                if (current == null) {
                    return defaultValue;
                }
            }

            return current.asInt();
        } catch (Exception e) {
            logger.warn("Failed to get int property: {}", path, e);
            return defaultValue;
        }
    }

    public boolean getBooleanProperty(String path) {
        return getBooleanProperty(path, false);
    }

    public boolean getBooleanProperty(String path, boolean defaultValue) {
        try {
            String[] parts = path.split("\\.");
            JsonNode current = config;

            for (String part : parts) {
                current = current.get(part);
                if (current == null) {
                    return defaultValue;
                }
            }

            return current.asBoolean();
        } catch (Exception e) {
            logger.warn("Failed to get boolean property: {}", path, e);
            return defaultValue;
        }
    }

    public Map<String, Object> getDatabaseTemplate(String templateName) {
        if (dbTemplates == null) {
            return new HashMap<>();
        }

        try {
            JsonNode templates = dbTemplates.get("templates");
            if (templates != null && templates.isArray()) {
                for (JsonNode template : templates) {
                    if (templateName.equals(template.get("name").asText())) {
                        Map<String, Object> result = new HashMap<>();
                        result.put("name", template.get("name").asText());
                        result.put("type", template.get("type").asText());
                        result.put("host", template.get("host").asText());
                        result.put("port", template.get("port").asInt());
                        result.put("database", template.get("database").asText());
                        result.put("driverClass", template.get("driverClass").asText());
                        result.put("urlTemplate", template.get("urlTemplate").asText());
                        return result;
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to get database template: {}", templateName, e);
        }

        return new HashMap<>();
    }

    public Map<String, String> getAllDatabaseTemplateNames() {
        Map<String, String> templateNames = new HashMap<>();

        if (dbTemplates == null) {
            return templateNames;
        }

        try {
            JsonNode templates = dbTemplates.get("templates");
            if (templates != null && templates.isArray()) {
                for (JsonNode template : templates) {
                    String name = template.get("name").asText();
                    String type = template.get("type").asText();
                    templateNames.put(name, type);
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to get database template names", e);
        }

        return templateNames;
    }

    public int getDefaultPort(String dbType) {
        return getIntProperty("database.defaultPort." + dbType.toLowerCase(), 1433);
    }

    // Application properties
    public String getApplicationName() {
        return getStringProperty("application.name", "JasperReports Desktop");
    }

    public String getApplicationVersion() {
        return getStringProperty("application.version", "1.0.0");
    }

    public String getDefaultLanguage() {
        return getStringProperty("application.defaultLanguage", "de");
    }

    // UI properties
    public int getWindowWidth() {
        return getIntProperty("ui.windowWidth", 1200);
    }

    public int getWindowHeight() {
        return getIntProperty("ui.windowHeight", 800);
    }

    public int getMinWindowWidth() {
        return getIntProperty("ui.minWindowWidth", 800);
    }

    public int getMinWindowHeight() {
        return getIntProperty("ui.minWindowHeight", 600);
    }

    // Database properties
    public int getConnectionTimeout() {
        return getIntProperty("database.connectionTimeout", 30000);
    }

    public int getQueryTimeout() {
        return getIntProperty("database.queryTimeout", 60000);
    }

    public int getMaxPoolSize() {
        return getIntProperty("database.maxPoolSize", 10);
    }

    // Report properties
    public String getDefaultPageSize() {
        return getStringProperty("reports.defaultPageSize", "A4");
    }

    public String getDefaultOrientation() {
        return getStringProperty("reports.defaultOrientation", "PORTRAIT");
    }

    public String getDefaultFont() {
        return getStringProperty("reports.defaultFont", "Arial");
    }

    public int getDefaultFontSize() {
        return getIntProperty("reports.defaultFontSize", 10);
    }

    public int getMaxRecordsPerReport() {
        return getIntProperty("reports.maxRecordsPerReport", 50000);
    }

    public int getPreviewRecords() {
        return getIntProperty("reports.previewRecords", 10);
    }

    public String getTempDirectory() {
        return getStringProperty("reports.tempDirectory", "./temp");
    }

    // Security properties
    public boolean isEncryptPasswords() {
        return getBooleanProperty("security.encryptPasswords", true);
    }

    public int getSessionTimeout() {
        return getIntProperty("security.sessionTimeout", 3600000);
    }
}