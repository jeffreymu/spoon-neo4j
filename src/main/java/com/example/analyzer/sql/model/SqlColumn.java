package com.example.analyzer.sql.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a SQL column.
 */
public class SqlColumn {
    private String name;
    private String tableName;
    private String schemaName;
    private String dataType;
    private boolean nullable;
    private boolean primaryKey;
    private boolean foreignKey;
    private String foreignKeyTable;
    private String foreignKeyColumn;
    private String defaultValue;
    private boolean autoIncrement;
    private String comment;
    private List<String> constraints;

    public SqlColumn(String name) {
        this.name = name;
        this.nullable = true;
        this.primaryKey = false;
        this.foreignKey = false;
        this.autoIncrement = false;
        this.constraints = new ArrayList<>();
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

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public boolean isNullable() {
        return nullable;
    }

    public void setNullable(boolean nullable) {
        this.nullable = nullable;
    }

    public boolean isPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey(boolean primaryKey) {
        this.primaryKey = primaryKey;
    }

    public boolean isForeignKey() {
        return foreignKey;
    }

    public void setForeignKey(boolean foreignKey) {
        this.foreignKey = foreignKey;
    }

    public String getForeignKeyTable() {
        return foreignKeyTable;
    }

    public void setForeignKeyTable(String foreignKeyTable) {
        this.foreignKeyTable = foreignKeyTable;
    }

    public String getForeignKeyColumn() {
        return foreignKeyColumn;
    }

    public void setForeignKeyColumn(String foreignKeyColumn) {
        this.foreignKeyColumn = foreignKeyColumn;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public boolean isAutoIncrement() {
        return autoIncrement;
    }

    public void setAutoIncrement(boolean autoIncrement) {
        this.autoIncrement = autoIncrement;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public List<String> getConstraints() {
        return constraints;
    }

    public void addConstraint(String constraint) {
        this.constraints.add(constraint);
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
