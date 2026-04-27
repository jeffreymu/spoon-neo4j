package com.example.analyzer.sql.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a SQL view.
 */
public class SqlView {
    private String name;
    private String schemaName;
    private String databaseName;
    private String definition;
    private List<String> referencedTables;
    private String comment;

    public SqlView(String name) {
        this.name = name;
        this.referencedTables = new ArrayList<>();
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

    public String getDefinition() {
        return definition;
    }

    public void setDefinition(String definition) {
        this.definition = definition;
    }

    public List<String> getReferencedTables() {
        return referencedTables;
    }

    public void addReferencedTable(String tableName) {
        if (!this.referencedTables.contains(tableName)) {
            this.referencedTables.add(tableName);
        }
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
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
}
