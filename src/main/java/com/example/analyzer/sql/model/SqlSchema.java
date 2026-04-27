package com.example.analyzer.sql.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a SQL schema/database.
 */
public class SqlSchema {
    private String name;
    private String databaseName;
    private Map<String, SqlTable> tables;
    private Map<String, SqlView> views;
    private List<String> procedures;

    public SqlSchema(String name) {
        this.name = name;
        this.tables = new HashMap<>();
        this.views = new HashMap<>();
        this.procedures = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public Map<String, SqlTable> getTables() {
        return tables;
    }

    public void addTable(SqlTable table) {
        table.setSchemaName(this.name);
        this.tables.put(table.getName(), table);
    }

    public SqlTable getTable(String name) {
        return this.tables.get(name);
    }

    public Map<String, SqlView> getViews() {
        return views;
    }

    public void addView(SqlView view) {
        view.setSchemaName(this.name);
        this.views.put(view.getName(), view);
    }

    public SqlView getView(String name) {
        return this.views.get(name);
    }

    public List<String> getProcedures() {
        return procedures;
    }

    public void addProcedure(String procedureName) {
        this.procedures.add(procedureName);
    }

    public int getTableCount() {
        return tables.size();
    }

    public int getViewCount() {
        return views.size();
    }
}
