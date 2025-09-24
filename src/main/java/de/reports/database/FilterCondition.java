package de.reports.database;

import java.util.Objects;

public class FilterCondition {
    public enum Operator {
        EQUALS("=", "Gleich"),
        NOT_EQUALS("!=", "Ungleich"),
        CONTAINS("LIKE", "Enthält"),
        STARTS_WITH("STARTS_WITH", "Beginnt mit"),
        ENDS_WITH("ENDS_WITH", "Endet mit"),
        GREATER_THAN(">", "Größer als"),
        GREATER_THAN_OR_EQUAL(">=", "Größer oder gleich"),
        LESS_THAN("<", "Kleiner als"),
        LESS_THAN_OR_EQUAL("<=", "Kleiner oder gleich"),
        IS_NULL("IS NULL", "Ist NULL"),
        IS_NOT_NULL("IS NOT NULL", "Ist nicht NULL"),
        IN("IN", "In Liste"),
        NOT_IN("NOT IN", "Nicht in Liste"),
        BETWEEN("BETWEEN", "Zwischen");

        private final String sqlOperator;
        private final String displayName;

        Operator(String sqlOperator, String displayName) {
            this.sqlOperator = sqlOperator;
            this.displayName = displayName;
        }

        public String getSqlOperator() {
            return sqlOperator;
        }

        public String getDisplayName() {
            return displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    public enum LogicalOperator {
        AND("AND", "UND"),
        OR("OR", "ODER");

        private final String sqlOperator;
        private final String displayName;

        LogicalOperator(String sqlOperator, String displayName) {
            this.sqlOperator = sqlOperator;
            this.displayName = displayName;
        }

        public String getSqlOperator() {
            return sqlOperator;
        }

        public String getDisplayName() {
            return displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private String columnName;
    private String dataType;
    private Operator operator;
    private String value;
    private LogicalOperator logicalOperator;

    public FilterCondition() {
        this.logicalOperator = LogicalOperator.AND;
    }

    public FilterCondition(String columnName, Operator operator, String value) {
        this.columnName = columnName;
        this.operator = operator;
        this.value = value;
        this.logicalOperator = LogicalOperator.AND;
    }

    public FilterCondition(String columnName, String dataType, Operator operator, String value) {
        this.columnName = columnName;
        this.dataType = dataType;
        this.operator = operator;
        this.value = value;
        this.logicalOperator = LogicalOperator.AND;
    }

    public FilterCondition(String columnName, String dataType, Operator operator, String value, LogicalOperator logicalOperator) {
        this.columnName = columnName;
        this.dataType = dataType;
        this.operator = operator;
        this.value = value;
        this.logicalOperator = logicalOperator;
    }

    public boolean isValid() {
        if (columnName == null || columnName.trim().isEmpty()) {
            return false;
        }

        if (operator == null) {
            return false;
        }

        // Some operators don't need a value
        if (operator == Operator.IS_NULL || operator == Operator.IS_NOT_NULL) {
            return true;
        }

        return value != null && !value.trim().isEmpty();
    }

    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();

        if (logicalOperator != LogicalOperator.AND) {
            sb.append(logicalOperator.getDisplayName()).append(" ");
        }

        sb.append(columnName)
          .append(" ")
          .append(operator.getDisplayName());

        if (operator != Operator.IS_NULL && operator != Operator.IS_NOT_NULL) {
            sb.append(" ").append(value);
        }

        return sb.toString();
    }

    public String getOperator() {
        return operator != null ? operator.name() : "";
    }

    public void setOperator(String operatorString) {
        try {
            this.operator = Operator.valueOf(operatorString.toUpperCase());
        } catch (IllegalArgumentException e) {
            this.operator = Operator.EQUALS;
        }
    }

    // Getters and Setters
    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public Operator getOperatorEnum() {
        return operator;
    }

    public void setOperatorEnum(Operator operator) {
        this.operator = operator;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public LogicalOperator getLogicalOperator() {
        return logicalOperator;
    }

    public void setLogicalOperator(LogicalOperator logicalOperator) {
        this.logicalOperator = logicalOperator;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FilterCondition that = (FilterCondition) o;
        return Objects.equals(columnName, that.columnName) &&
               Objects.equals(dataType, that.dataType) &&
               operator == that.operator &&
               Objects.equals(value, that.value) &&
               logicalOperator == that.logicalOperator;
    }

    @Override
    public int hashCode() {
        return Objects.hash(columnName, dataType, operator, value, logicalOperator);
    }

    @Override
    public String toString() {
        return toDisplayString();
    }
}