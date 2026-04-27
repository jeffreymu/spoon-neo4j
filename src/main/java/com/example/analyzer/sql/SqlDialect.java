package com.example.analyzer.sql;

/**
 * SQL dialect enumeration.
 */
public enum SqlDialect {
    MYSQL("MySQL"),
    ORACLE("Oracle"),
    GENERIC("Generic");

    private final String displayName;

    SqlDialect(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
