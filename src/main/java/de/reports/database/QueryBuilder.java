package de.reports.database;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class QueryBuilder {
    private static final Logger logger = LoggerFactory.getLogger(QueryBuilder.class);

    private String tableName;
    private List<String> selectedColumns;
    private List<FilterCondition> filterConditions;
    private List<SortOrder> sortOrders;
    private List<String> groupByColumns;
    private String dbType;
    private int limitRows = 0;

    public QueryBuilder(String tableName, String dbType) {
        this.tableName = tableName;
        this.dbType = dbType != null ? dbType.toLowerCase() : "mysql";
    }

    public QueryBuilder selectColumns(List<String> columns) {
        this.selectedColumns = columns;
        return this;
    }

    public QueryBuilder where(List<FilterCondition> conditions) {
        this.filterConditions = conditions;
        return this;
    }

    public QueryBuilder orderBy(List<SortOrder> orders) {
        this.sortOrders = orders;
        return this;
    }

    public QueryBuilder groupBy(List<String> columns) {
        this.groupByColumns = columns;
        return this;
    }

    public QueryBuilder limit(int rows) {
        this.limitRows = rows;
        return this;
    }

    public String build() {
        StringBuilder query = new StringBuilder("SELECT ");

        // SELECT clause
        if (selectedColumns == null || selectedColumns.isEmpty()) {
            query.append("*");
        } else {
            query.append(String.join(", ", selectedColumns));
        }

        // FROM clause
        query.append(" FROM ").append(tableName);

        // WHERE clause
        if (filterConditions != null && !filterConditions.isEmpty()) {
            query.append(" WHERE ");
            for (int i = 0; i < filterConditions.size(); i++) {
                if (i > 0) {
                    query.append(" AND ");
                }
                query.append(buildFilterCondition(filterConditions.get(i)));
            }
        }

        // GROUP BY clause
        if (groupByColumns != null && !groupByColumns.isEmpty()) {
            query.append(" GROUP BY ");
            query.append(String.join(", ", groupByColumns));
        }

        // ORDER BY clause
        if (sortOrders != null && !sortOrders.isEmpty()) {
            query.append(" ORDER BY ");
            for (int i = 0; i < sortOrders.size(); i++) {
                if (i > 0) {
                    query.append(", ");
                }
                SortOrder sortOrder = sortOrders.get(i);
                query.append(sortOrder.getColumnName())
                     .append(" ")
                     .append(sortOrder.getDirection());
            }
        }

        // LIMIT clause (database specific)
        if (limitRows > 0) {
            query.append(buildLimitClause(limitRows));
        }

        String finalQuery = query.toString();
        logger.debug("Generated query: {}", finalQuery);
        return finalQuery;
    }

    private String buildFilterCondition(FilterCondition condition) {
        String column = condition.getColumnName();
        String operator = condition.getOperator();
        String value = condition.getValue();

        switch (operator.toUpperCase()) {
            case "EQUALS":
            case "=":
                return column + " = '" + escapeValue(value) + "'";

            case "NOT_EQUALS":
            case "!=":
            case "<>":
                return column + " != '" + escapeValue(value) + "'";

            case "CONTAINS":
            case "LIKE":
                return column + " LIKE '%" + escapeValue(value) + "%'";

            case "STARTS_WITH":
                return column + " LIKE '" + escapeValue(value) + "%'";

            case "ENDS_WITH":
                return column + " LIKE '%" + escapeValue(value) + "'";

            case "GREATER_THAN":
            case ">":
                return column + " > " + formatValue(value, condition.getDataType());

            case "GREATER_THAN_OR_EQUAL":
            case ">=":
                return column + " >= " + formatValue(value, condition.getDataType());

            case "LESS_THAN":
            case "<":
                return column + " < " + formatValue(value, condition.getDataType());

            case "LESS_THAN_OR_EQUAL":
            case "<=":
                return column + " <= " + formatValue(value, condition.getDataType());

            case "IS_NULL":
                return column + " IS NULL";

            case "IS_NOT_NULL":
                return column + " IS NOT NULL";

            case "IN":
                return column + " IN (" + formatInValues(value) + ")";

            case "NOT_IN":
                return column + " NOT IN (" + formatInValues(value) + ")";

            case "BETWEEN":
                String[] betweenValues = value.split(",");
                if (betweenValues.length == 2) {
                    return column + " BETWEEN " + formatValue(betweenValues[0].trim(), condition.getDataType()) +
                           " AND " + formatValue(betweenValues[1].trim(), condition.getDataType());
                }
                break;

            default:
                logger.warn("Unknown operator: {}", operator);
                return column + " = '" + escapeValue(value) + "'";
        }

        return column + " = '" + escapeValue(value) + "'";
    }

    private String formatValue(String value, String dataType) {
        if (value == null || value.trim().isEmpty()) {
            return "NULL";
        }

        value = value.trim();

        // For numeric types, don't add quotes
        if (dataType != null) {
            String lowerDataType = dataType.toLowerCase();
            if (lowerDataType.contains("int") ||
                lowerDataType.contains("decimal") ||
                lowerDataType.contains("numeric") ||
                lowerDataType.contains("float") ||
                lowerDataType.contains("double") ||
                lowerDataType.contains("real")) {
                return value;
            }
        }

        // For other types, add quotes and escape
        return "'" + escapeValue(value) + "'";
    }

    private String formatInValues(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "NULL";
        }

        String[] values = value.split(",");
        StringBuilder formatted = new StringBuilder();

        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                formatted.append(", ");
            }
            formatted.append("'").append(escapeValue(values[i].trim())).append("'");
        }

        return formatted.toString();
    }

    private String escapeValue(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("'", "''");
    }

    private String buildLimitClause(int rows) {
        switch (dbType) {
            case "mysql":
            case "postgresql":
                return " LIMIT " + rows;

            case "sqlserver":
                // For SQL Server, we need to modify the query structure
                // This is a simplified approach
                return "";

            case "oracle":
                // For Oracle, we need to modify the query structure
                // This is a simplified approach
                return "";

            default:
                return " LIMIT " + rows;
        }
    }

    public String buildCountQuery() {
        StringBuilder query = new StringBuilder("SELECT COUNT(*)");

        // FROM clause
        query.append(" FROM ").append(tableName);

        // WHERE clause
        if (filterConditions != null && !filterConditions.isEmpty()) {
            query.append(" WHERE ");
            for (int i = 0; i < filterConditions.size(); i++) {
                if (i > 0) {
                    query.append(" AND ");
                }
                query.append(buildFilterCondition(filterConditions.get(i)));
            }
        }

        String countQuery = query.toString();
        logger.debug("Generated count query: {}", countQuery);
        return countQuery;
    }

    public String buildSqlServerTopQuery(int topRows) {
        StringBuilder query = new StringBuilder("SELECT TOP ").append(topRows).append(" ");

        // SELECT clause
        if (selectedColumns == null || selectedColumns.isEmpty()) {
            query.append("*");
        } else {
            query.append(String.join(", ", selectedColumns));
        }

        // FROM clause
        query.append(" FROM ").append(tableName);

        // WHERE clause
        if (filterConditions != null && !filterConditions.isEmpty()) {
            query.append(" WHERE ");
            for (int i = 0; i < filterConditions.size(); i++) {
                if (i > 0) {
                    query.append(" AND ");
                }
                query.append(buildFilterCondition(filterConditions.get(i)));
            }
        }

        // GROUP BY clause
        if (groupByColumns != null && !groupByColumns.isEmpty()) {
            query.append(" GROUP BY ");
            query.append(String.join(", ", groupByColumns));
        }

        // ORDER BY clause
        if (sortOrders != null && !sortOrders.isEmpty()) {
            query.append(" ORDER BY ");
            for (int i = 0; i < sortOrders.size(); i++) {
                if (i > 0) {
                    query.append(", ");
                }
                SortOrder sortOrder = sortOrders.get(i);
                query.append(sortOrder.getColumnName())
                     .append(" ")
                     .append(sortOrder.getDirection());
            }
        }

        String finalQuery = query.toString();
        logger.debug("Generated SQL Server TOP query: {}", finalQuery);
        return finalQuery;
    }

    // Static helper methods for common queries
    public static String buildSimpleSelectQuery(String tableName, int maxRows, String dbType) {
        QueryBuilder builder = new QueryBuilder(tableName, dbType);
        if (maxRows > 0) {
            builder.limit(maxRows);
        }
        return builder.build();
    }

    public static String buildSelectWithColumnsQuery(String tableName, List<String> columns, String dbType) {
        return new QueryBuilder(tableName, dbType)
                .selectColumns(columns)
                .build();
    }
}