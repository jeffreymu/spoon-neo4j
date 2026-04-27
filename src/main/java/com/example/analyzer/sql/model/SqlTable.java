package com.example.analyzer.sql.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a SQL table.
 */
public class SqlTable {
    private String name;
    private String schemaName;
    private String databaseName;
    private String comment;
    private Map<String, SqlColumn> columns;
    private List<SqlIndex> indexes;
    private List<String> primaryKeyColumns;
    private String tableType; // TABLE, VIEW, etc.

    public SqlTable(String name) {
        this.name = name;
        this.columns = new HashMap<>();
        this.indexes = new ArrayList<>();
        this.primaryKeyColumns = new ArrayList<>();
        this.tableType = "TABLE";
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Map<String, SqlColumn> getColumns() {
        return columns;
    }

    public void addColumn(SqlColumn column) {
        column.setTableName(this.name);
        column.setSchemaName(this.schemaName);
        this.columns.put(column.getName(), column);
    }

    public SqlColumn getColumn(String name) {
        return this.columns.get(name);
    }

    public List<SqlIndex> getIndexes() {
        return indexes;
    }

    public void addIndex(SqlIndex index) {
        index.setTableName(this.name);
        index.setSchemaName(this.schemaName);
        this.indexes.add(index);
    }

    public List<String> getPrimaryKeyColumns() {
        return primaryKeyColumns;
    }

    public void addPrimaryKeyColumn(String columnName) {
        this.primaryKeyColumns.add(columnName);
        SqlColumn column = this.columns.get(columnName);
        if (column != null) {
            column.setPrimaryKey(true);
        }
    }

    public String getTableType() {
        return tableType;
    }

    public void setTableType(String tableType) {
        this.tableType = tableType;
    }

    public String getQualifiedName() {
        StringBuilder sb = new StringBuilder();
        if (databaseName != null && !databaseName.isEmpty()) {
            sb.append(databaseName).append(".");
        }
        if (schemaName != null && !schemaName.isEmpty()) {
            sb.append(schemaName).append(".");
        }
        sb.append(name);
        return sb.toString();
    }

    public int getColumnCount() {
        return columns.size();
    }
}
