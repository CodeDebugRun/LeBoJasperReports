package de.reports.database;

import java.util.Objects;

public class ColumnInfo {
    private String name;
    private String dataType;
    private int size;
    private boolean nullable;
    private String defaultValue;
    private boolean isPrimaryKey;
    private boolean isAutoIncrement;

    public ColumnInfo() {
    }

    public ColumnInfo(String name, String dataType, int size, boolean nullable, String defaultValue) {
        this.name = name;
        this.dataType = dataType;
        this.size = size;
        this.nullable = nullable;
        this.defaultValue = defaultValue;
        this.isPrimaryKey = false;
        this.isAutoIncrement = false;
    }

    public ColumnInfo(String name, String dataType, int size, boolean nullable, String defaultValue,
                     boolean isPrimaryKey, boolean isAutoIncrement) {
        this.name = name;
        this.dataType = dataType;
        this.size = size;
        this.nullable = nullable;
        this.defaultValue = defaultValue;
        this.isPrimaryKey = isPrimaryKey;
        this.isAutoIncrement = isAutoIncrement;
    }

    /**
     * Get Java class type for the column data type
     */
    public Class<?> getJavaType() {
        String lowerDataType = dataType.toLowerCase();

        // Numeric types
        if (lowerDataType.contains("int") || lowerDataType.contains("integer")) {
            return Integer.class;
        }
        if (lowerDataType.contains("bigint") || lowerDataType.contains("long")) {
            return Long.class;
        }
        if (lowerDataType.contains("smallint") || lowerDataType.contains("tinyint")) {
            return Short.class;
        }
        if (lowerDataType.contains("decimal") || lowerDataType.contains("numeric")) {
            return java.math.BigDecimal.class;
        }
        if (lowerDataType.contains("float") || lowerDataType.contains("real")) {
            return Float.class;
        }
        if (lowerDataType.contains("double")) {
            return Double.class;
        }

        // Date and time types
        if (lowerDataType.contains("date")) {
            return java.sql.Date.class;
        }
        if (lowerDataType.contains("time")) {
            return java.sql.Time.class;
        }
        if (lowerDataType.contains("timestamp") || lowerDataType.contains("datetime")) {
            return java.sql.Timestamp.class;
        }

        // Boolean
        if (lowerDataType.contains("bit") || lowerDataType.contains("boolean")) {
            return Boolean.class;
        }

        // Binary types
        if (lowerDataType.contains("blob") || lowerDataType.contains("binary") ||
            lowerDataType.contains("varbinary") || lowerDataType.contains("image")) {
            return byte[].class;
        }

        // Default to String for text types
        return String.class;
    }

    /**
     * Check if this is a numeric column
     */
    public boolean isNumeric() {
        String lowerDataType = dataType.toLowerCase();
        return lowerDataType.contains("int") ||
               lowerDataType.contains("decimal") ||
               lowerDataType.contains("numeric") ||
               lowerDataType.contains("float") ||
               lowerDataType.contains("double") ||
               lowerDataType.contains("real") ||
               lowerDataType.contains("money");
    }

    /**
     * Check if this is a date/time column
     */
    public boolean isDateTime() {
        String lowerDataType = dataType.toLowerCase();
        return lowerDataType.contains("date") ||
               lowerDataType.contains("time") ||
               lowerDataType.contains("timestamp");
    }

    /**
     * Check if this is a text column
     */
    public boolean isText() {
        String lowerDataType = dataType.toLowerCase();
        return lowerDataType.contains("char") ||
               lowerDataType.contains("text") ||
               lowerDataType.contains("varchar") ||
               lowerDataType.contains("nvarchar") ||
               lowerDataType.contains("clob");
    }

    /**
     * Check if this is a boolean column
     */
    public boolean isBoolean() {
        String lowerDataType = dataType.toLowerCase();
        return lowerDataType.contains("bit") ||
               lowerDataType.contains("boolean") ||
               lowerDataType.equals("bool");
    }

    /**
     * Get display name with data type
     */
    public String getDisplayName() {
        StringBuilder sb = new StringBuilder(name);
        sb.append(" (").append(dataType);
        if (size > 0 && !dataType.toLowerCase().contains("text")) {
            sb.append("(").append(size).append(")");
        }
        sb.append(")");

        if (isPrimaryKey) {
            sb.append(" [PK]");
        }
        if (isAutoIncrement) {
            sb.append(" [AUTO]");
        }
        if (!nullable) {
            sb.append(" [NOT NULL]");
        }

        return sb.toString();
    }

    /**
     * Get short display name
     */
    public String getShortDisplayName() {
        return name + " (" + dataType + ")";
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public boolean isNullable() {
        return nullable;
    }

    public void setNullable(boolean nullable) {
        this.nullable = nullable;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public boolean isPrimaryKey() {
        return isPrimaryKey;
    }

    public void setPrimaryKey(boolean primaryKey) {
        isPrimaryKey = primaryKey;
    }

    public boolean isAutoIncrement() {
        return isAutoIncrement;
    }

    public void setAutoIncrement(boolean autoIncrement) {
        isAutoIncrement = autoIncrement;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ColumnInfo that = (ColumnInfo) o;
        return Objects.equals(name, that.name) &&
               Objects.equals(dataType, that.dataType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, dataType);
    }

    @Override
    public String toString() {
        return getDisplayName();
    }
}