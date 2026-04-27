package com.example.analyzer.sql.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a SQL index.
 */
public class SqlIndex {
    private String name;
    private String tableName;
    private String schemaName;
    private List<String> columns;
    private boolean unique;
    private String type;

    public SqlIndex(String name) {
        this.name = name;
        this.columns = new ArrayList<>();
        this.unique = false;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    public List<String> getColumns() {
        return columns;
    }

    public void addColumn(String column) {
        this.columns.add(column);
    }

    public boolean isUnique() {
        return unique;
    }

    public void setUnique(boolean unique) {
        this.unique = unique;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getQualifiedName() {
        if (schemaName != null && !schemaName.isEmpty()) {
            return schemaName + "." + tableName + "." + name;
        } else if (tableName != null && !tableName.isEmpty()) {
            return tableName + "." + name;
        }
        return name;
    }
}
