package de.reports.database;

import de.reports.utils.SecurityUtils;

import java.util.Objects;

public class DatabaseConnectionInfo {
    private String name;
    private String type;
    private String host;
    private int port;
    private String database;
    private String username;
    private String password;
    private String driverClass;
    private String urlTemplate;

    public DatabaseConnectionInfo() {
    }

    public DatabaseConnectionInfo(String name, String type, String host, int port, String database,
                                String username, String password) {
        this.name = name;
        this.type = type;
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
        setDriverClassFromType();
        setUrlTemplateFromType();
    }

    private void setDriverClassFromType() {
        switch (type.toLowerCase()) {
            case "mysql":
                this.driverClass = "com.mysql.cj.jdbc.Driver";
                break;
            case "postgresql":
                this.driverClass = "org.postgresql.Driver";
                break;
            case "sqlserver":
                this.driverClass = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
                break;
            case "oracle":
                this.driverClass = "oracle.jdbc.OracleDriver";
                break;
            default:
                this.driverClass = "";
        }
    }

    private void setUrlTemplateFromType() {
        switch (type.toLowerCase()) {
            case "mysql":
                this.urlTemplate = "jdbc:mysql://{host}:{port}/{database}?useSSL=false&serverTimezone=UTC";
                break;
            case "postgresql":
                this.urlTemplate = "jdbc:postgresql://{host}:{port}/{database}";
                break;
            case "sqlserver":
                this.urlTemplate = "jdbc:sqlserver://{host}:{port};databaseName={database};encrypt=false";
                break;
            case "oracle":
                this.urlTemplate = "jdbc:oracle:thin:@{host}:{port}:{database}";
                break;
            default:
                this.urlTemplate = "";
        }
    }

    public String getConnectionUrl() {
        if (urlTemplate == null || urlTemplate.isEmpty()) {
            setUrlTemplateFromType();
        }

        return urlTemplate
                .replace("{host}", host)
                .replace("{port}", String.valueOf(port))
                .replace("{database}", database);
    }

    public boolean isValid() {
        return name != null && !name.trim().isEmpty() &&
               type != null && !type.trim().isEmpty() &&
               host != null && !host.trim().isEmpty() &&
               port > 0 &&
               database != null && !database.trim().isEmpty() &&
               username != null && !username.trim().isEmpty();
    }

    public DatabaseConnectionInfo copy() {
        DatabaseConnectionInfo copy = new DatabaseConnectionInfo();
        copy.name = this.name;
        copy.type = this.type;
        copy.host = this.host;
        copy.port = this.port;
        copy.database = this.database;
        copy.username = this.username;
        copy.password = this.password;
        copy.driverClass = this.driverClass;
        copy.urlTemplate = this.urlTemplate;
        return copy;
    }

    public void encryptPassword() {
        if (password != null && !password.isEmpty() && !SecurityUtils.isEncrypted(password)) {
            this.password = SecurityUtils.encryptPassword(password);
        }
    }

    public String getDecryptedPassword() {
        if (password != null && SecurityUtils.isEncrypted(password)) {
            return SecurityUtils.decryptPassword(password);
        }
        return password;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
        setDriverClassFromType();
        setUrlTemplateFromType();
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDriverClass() {
        if (driverClass == null || driverClass.isEmpty()) {
            setDriverClassFromType();
        }
        return driverClass;
    }

    public void setDriverClass(String driverClass) {
        this.driverClass = driverClass;
    }

    public String getUrlTemplate() {
        if (urlTemplate == null || urlTemplate.isEmpty()) {
            setUrlTemplateFromType();
        }
        return urlTemplate;
    }

    public void setUrlTemplate(String urlTemplate) {
        this.urlTemplate = urlTemplate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DatabaseConnectionInfo that = (DatabaseConnectionInfo) o;
        return port == that.port &&
               Objects.equals(name, that.name) &&
               Objects.equals(type, that.type) &&
               Objects.equals(host, that.host) &&
               Objects.equals(database, that.database) &&
               Objects.equals(username, that.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type, host, port, database, username);
    }

    @Override
    public String toString() {
        return String.format("%s (%s://%s:%d/%s)", name, type, host, port, database);
    }

    public String toDisplayString() {
        return String.format("%s - %s@%s:%d/%s", name, username, host, port, database);
    }
}