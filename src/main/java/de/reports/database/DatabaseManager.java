package de.reports.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import de.reports.utils.ConfigManager;
import de.reports.utils.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

public class DatabaseManager {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);

    private HikariDataSource dataSource;
    private DatabaseConnectionInfo connectionInfo;
    private boolean isConnected = false;

    public DatabaseManager() {
        // Constructor
    }

    /**
     * Connect to database using connection info
     */
    public boolean connect(DatabaseConnectionInfo connInfo) {
        try {
            // Close existing connection if any
            disconnect();

            this.connectionInfo = connInfo;

            // Configure HikariCP
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(connInfo.getConnectionUrl());
            config.setUsername(connInfo.getUsername());
            config.setPassword(SecurityUtils.isEncrypted(connInfo.getPassword())
                ? SecurityUtils.decryptPassword(connInfo.getPassword())
                : connInfo.getPassword());
            config.setDriverClassName(connInfo.getDriverClass());

            // Connection pool settings
            ConfigManager configManager = ConfigManager.getInstance();
            config.setMaximumPoolSize(configManager.getMaxPoolSize());
            config.setConnectionTimeout(configManager.getConnectionTimeout());
            config.setIdleTimeout(600000); // 10 minutes
            config.setMaxLifetime(1800000); // 30 minutes
            config.setLeakDetectionThreshold(60000); // 1 minute

            // Connection validation
            config.setConnectionTestQuery("SELECT 1");
            config.setValidationTimeout(5000);

            // Additional properties
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

            dataSource = new HikariDataSource(config);

            // Test connection
            try (Connection testConnection = dataSource.getConnection()) {
                isConnected = testConnection.isValid(5);
                if (isConnected) {
                    logger.info("Database connection established successfully: {}",
                        SecurityUtils.maskPassword(connInfo.getConnectionUrl()));
                } else {
                    logger.error("Database connection test failed");
                    disconnect();
                    return false;
                }
            }

            return isConnected;

        } catch (SQLException e) {
            logger.error("Failed to connect to database", e);
            disconnect();
            return false;
        }
    }

    /**
     * Disconnect from database
     */
    public void disconnect() {
        try {
            if (dataSource != null && !dataSource.isClosed()) {
                dataSource.close();
                logger.info("Database connection closed");
            }
        } catch (Exception e) {
            logger.error("Error while disconnecting from database", e);
        } finally {
            dataSource = null;
            isConnected = false;
        }
    }

    /**
     * Test database connection
     */
    public boolean testConnection(DatabaseConnectionInfo connInfo) {
        try {
            Class.forName(connInfo.getDriverClass());

            String password = SecurityUtils.isEncrypted(connInfo.getPassword())
                ? SecurityUtils.decryptPassword(connInfo.getPassword())
                : connInfo.getPassword();

            try (Connection connection = DriverManager.getConnection(
                    connInfo.getConnectionUrl(),
                    connInfo.getUsername(),
                    password)) {

                return connection.isValid(5);
            }
        } catch (ClassNotFoundException e) {
            logger.error("Database driver not found: {}", connInfo.getDriverClass(), e);
            return false;
        } catch (SQLException e) {
            logger.error("Database connection test failed", e);
            return false;
        }
    }

    /**
     * Get all table names from the database
     */
    public List<String> getTableNames() {
        List<String> tableNames = new ArrayList<>();

        if (!isConnected || dataSource == null) {
            logger.warn("Not connected to database");
            return tableNames;
        }

        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();

            // Get only user tables (exclude views and system objects)
            String[] tableTypes = {"TABLE"};
            try (ResultSet tables = metaData.getTables(null, null, "%", tableTypes)) {
                while (tables.next()) {
                    String tableName = tables.getString("TABLE_NAME");
                    String tableType = tables.getString("TABLE_TYPE");

                    // Skip system tables
                    if (!isSystemTable(tableName, connectionInfo.getType())) {
                        tableNames.add(tableName);
                    }
                }
            }

            Collections.sort(tableNames);
            logger.info("Found {} tables/views", tableNames.size());

        } catch (SQLException e) {
            logger.error("Failed to get table names", e);
        }

        return tableNames;
    }

    /**
     * Get table column information
     */
    public List<ColumnInfo> getTableColumns(String tableName) {
        List<ColumnInfo> columns = new ArrayList<>();

        if (!isConnected || dataSource == null) {
            logger.warn("Not connected to database");
            return columns;
        }

        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();

            try (ResultSet resultSet = metaData.getColumns(null, null, tableName, "%")) {
                while (resultSet.next()) {
                    String columnName = resultSet.getString("COLUMN_NAME");
                    String dataType = resultSet.getString("TYPE_NAME");
                    int columnSize = resultSet.getInt("COLUMN_SIZE");
                    boolean nullable = resultSet.getInt("NULLABLE") == DatabaseMetaData.columnNullable;
                    String defaultValue = resultSet.getString("COLUMN_DEF");

                    ColumnInfo columnInfo = new ColumnInfo(columnName, dataType, columnSize, nullable, defaultValue);
                    columns.add(columnInfo);
                }
            }

            logger.info("Found {} columns for table {}", columns.size(), tableName);

        } catch (SQLException e) {
            logger.error("Failed to get columns for table: {}", tableName, e);
        }

        return columns;
    }

    /**
     * Get sample data from table
     */
    public List<Map<String, Object>> getSampleData(String tableName, int maxRows) {
        List<Map<String, Object>> data = new ArrayList<>();

        if (!isConnected || dataSource == null) {
            logger.warn("Not connected to database");
            return data;
        }

        String query = buildSampleDataQuery(tableName, maxRows);

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(query);
             ResultSet resultSet = statement.executeQuery()) {

            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (resultSet.next() && data.size() < maxRows) {
                Map<String, Object> row = new LinkedHashMap<>();

                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnName(i);
                    Object value = resultSet.getObject(i);
                    row.put(columnName, value);
                }

                data.add(row);
            }

            logger.info("Retrieved {} sample rows from table {}", data.size(), tableName);

        } catch (SQLException e) {
            logger.error("Failed to get sample data from table: {}", tableName, e);
        }

        return data;
    }

    /**
     * Get record count for a table
     */
    public long getRecordCount(String tableName) {
        if (!isConnected || dataSource == null) {
            logger.warn("Not connected to database");
            return 0;
        }

        String query = "SELECT COUNT(*) FROM " + tableName;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(query);
             ResultSet resultSet = statement.executeQuery()) {

            if (resultSet.next()) {
                long count = resultSet.getLong(1);
                logger.info("Table {} has {} records", tableName, count);
                return count;
            }

        } catch (SQLException e) {
            logger.error("Failed to get record count for table: {}", tableName, e);
        }

        return 0;
    }

    /**
     * Execute query and return ResultSet data
     */
    public QueryResult executeQuery(String query) {
        if (!isConnected || dataSource == null) {
            logger.warn("Not connected to database");
            return new QueryResult(false, "Not connected to database", null, null);
        }

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(query);
             ResultSet resultSet = statement.executeQuery()) {

            List<Map<String, Object>> data = new ArrayList<>();
            List<String> columnNames = new ArrayList<>();

            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();

            // Get column names
            for (int i = 1; i <= columnCount; i++) {
                columnNames.add(metaData.getColumnName(i));
            }

            // Get data
            while (resultSet.next()) {
                Map<String, Object> row = new LinkedHashMap<>();

                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnName(i);
                    Object value = resultSet.getObject(i);
                    row.put(columnName, value);
                }

                data.add(row);
            }

            logger.info("Query executed successfully, returned {} rows", data.size());
            return new QueryResult(true, "Success", columnNames, data);

        } catch (SQLException e) {
            String errorMsg = "Failed to execute query: " + e.getMessage();
            logger.error(errorMsg, e);
            return new QueryResult(false, errorMsg, null, null);
        }
    }

    // Helper methods

    private boolean isSystemTable(String tableName, String dbType) {
        String lowerTableName = tableName.toLowerCase();

        switch (dbType.toLowerCase()) {
            case "mysql":
                return lowerTableName.startsWith("information_schema") ||
                       lowerTableName.startsWith("performance_schema") ||
                       lowerTableName.startsWith("mysql") ||
                       lowerTableName.startsWith("sys");

            case "postgresql":
                return lowerTableName.startsWith("information_schema") ||
                       lowerTableName.startsWith("pg_");

            case "sqlserver":
                return lowerTableName.startsWith("sys") ||
                       lowerTableName.startsWith("information_schema") ||
                       lowerTableName.startsWith("msreplication") ||
                       lowerTableName.startsWith("mspeer") ||
                       lowerTableName.startsWith("msdistribution") ||
                       lowerTableName.startsWith("mssubscription") ||
                       lowerTableName.startsWith("msmerge") ||
                       lowerTableName.startsWith("mssnapshot") ||
                       lowerTableName.startsWith("mslog") ||
                       lowerTableName.startsWith("msdb") ||
                       lowerTableName.startsWith("master") ||
                       lowerTableName.startsWith("model") ||
                       lowerTableName.startsWith("tempdb") ||
                       lowerTableName.startsWith("trace_xe") ||
                       lowerTableName.startsWith("fn_") ||
                       lowerTableName.startsWith("dm_") ||
                       lowerTableName.equals("dtproperties") ||
                       lowerTableName.equals("spt_fallback_db") ||
                       lowerTableName.equals("spt_fallback_dev") ||
                       lowerTableName.equals("spt_fallback_usg") ||
                       lowerTableName.equals("spt_monitor") ||
                       lowerTableName.equals("MSreplication_options");

            case "oracle":
                return lowerTableName.startsWith("sys") ||
                       lowerTableName.startsWith("dba_") ||
                       lowerTableName.startsWith("all_") ||
                       lowerTableName.startsWith("user_");

            default:
                return false;
        }
    }

    private String buildSampleDataQuery(String tableName, int maxRows) {
        String dbType = connectionInfo.getType().toLowerCase();

        switch (dbType) {
            case "mysql":
            case "postgresql":
                return "SELECT * FROM " + tableName + " LIMIT " + maxRows;

            case "sqlserver":
                return "SELECT TOP " + maxRows + " * FROM " + tableName;

            case "oracle":
                return "SELECT * FROM " + tableName + " WHERE ROWNUM <= " + maxRows;

            default:
                return "SELECT * FROM " + tableName;
        }
    }

    // Getters
    public boolean isConnected() {
        return isConnected && dataSource != null && !dataSource.isClosed();
    }

    public DatabaseConnectionInfo getConnectionInfo() {
        return connectionInfo;
    }

    public DataSource getDataSource() {
        return dataSource;
    }
}