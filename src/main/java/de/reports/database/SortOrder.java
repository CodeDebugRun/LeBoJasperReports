package de.reports.database;

import java.util.Objects;

public class SortOrder {
    public enum Direction {
        ASC("ASC", "Aufsteigend"),
        DESC("DESC", "Absteigend");

        private final String sqlDirection;
        private final String displayName;

        Direction(String sqlDirection, String displayName) {
            this.sqlDirection = sqlDirection;
            this.displayName = displayName;
        }

        public String getSqlDirection() {
            return sqlDirection;
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
    private Direction direction;
    private int priority;

    public SortOrder() {
        this.direction = Direction.ASC;
        this.priority = 0;
    }

    public SortOrder(String columnName) {
        this.columnName = columnName;
        this.direction = Direction.ASC;
        this.priority = 0;
    }

    public SortOrder(String columnName, Direction direction) {
        this.columnName = columnName;
        this.direction = direction;
        this.priority = 0;
    }

    public SortOrder(String columnName, Direction direction, int priority) {
        this.columnName = columnName;
        this.direction = direction;
        this.priority = priority;
    }

    public boolean isValid() {
        return columnName != null && !columnName.trim().isEmpty() && direction != null;
    }

    public String toDisplayString() {
        return columnName + " (" + direction.getDisplayName() + ")";
    }

    public String getDirection() {
        return direction != null ? direction.name() : Direction.ASC.name();
    }

    public void setDirection(String directionString) {
        try {
            this.direction = Direction.valueOf(directionString.toUpperCase());
        } catch (IllegalArgumentException e) {
            this.direction = Direction.ASC;
        }
    }

    // Getters and Setters
    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public Direction getDirectionEnum() {
        return direction;
    }

    public void setDirectionEnum(Direction direction) {
        this.direction = direction;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SortOrder sortOrder = (SortOrder) o;
        return priority == sortOrder.priority &&
               Objects.equals(columnName, sortOrder.columnName) &&
               direction == sortOrder.direction;
    }

    @Override
    public int hashCode() {
        return Objects.hash(columnName, direction, priority);
    }

    @Override
    public String toString() {
        return toDisplayString();
    }
}