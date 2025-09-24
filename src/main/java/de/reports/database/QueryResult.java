package de.reports.database;

import java.util.List;
import java.util.Map;

public class QueryResult {
    private boolean success;
    private String message;
    private List<String> columnNames;
    private List<Map<String, Object>> data;
    private long executionTime;

    public QueryResult() {
    }

    public QueryResult(boolean success, String message, List<String> columnNames, List<Map<String, Object>> data) {
        this.success = success;
        this.message = message;
        this.columnNames = columnNames;
        this.data = data;
    }

    public QueryResult(boolean success, String message, List<String> columnNames, List<Map<String, Object>> data, long executionTime) {
        this.success = success;
        this.message = message;
        this.columnNames = columnNames;
        this.data = data;
        this.executionTime = executionTime;
    }

    public int getRowCount() {
        return data != null ? data.size() : 0;
    }

    public int getColumnCount() {
        return columnNames != null ? columnNames.size() : 0;
    }

    public boolean hasData() {
        return data != null && !data.isEmpty();
    }

    public Object getValue(int rowIndex, String columnName) {
        if (data == null || rowIndex < 0 || rowIndex >= data.size()) {
            return null;
        }

        Map<String, Object> row = data.get(rowIndex);
        return row != null ? row.get(columnName) : null;
    }

    public Object getValue(int rowIndex, int columnIndex) {
        if (columnNames == null || columnIndex < 0 || columnIndex >= columnNames.size()) {
            return null;
        }

        String columnName = columnNames.get(columnIndex);
        return getValue(rowIndex, columnName);
    }

    public Map<String, Object> getRow(int rowIndex) {
        if (data == null || rowIndex < 0 || rowIndex >= data.size()) {
            return null;
        }

        return data.get(rowIndex);
    }

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<String> getColumnNames() {
        return columnNames;
    }

    public void setColumnNames(List<String> columnNames) {
        this.columnNames = columnNames;
    }

    public List<Map<String, Object>> getData() {
        return data;
    }

    public void setData(List<Map<String, Object>> data) {
        this.data = data;
    }

    public long getExecutionTime() {
        return executionTime;
    }

    public void setExecutionTime(long executionTime) {
        this.executionTime = executionTime;
    }

    @Override
    public String toString() {
        return String.format("QueryResult{success=%s, message='%s', rows=%d, columns=%d, executionTime=%dms}",
                success, message, getRowCount(), getColumnCount(), executionTime);
    }
}