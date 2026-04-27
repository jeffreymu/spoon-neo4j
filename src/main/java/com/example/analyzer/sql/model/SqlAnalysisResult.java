package com.example.analyzer.sql.model;

import com.example.analyzer.sql.SqlDialect;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Contains the complete SQL analysis result.
 */
public class SqlAnalysisResult {
    private String sourceName;
    private String sourcePath;
    private SqlDialect dialect;
    private Map<String, SqlSchema> schemas;
    private List<TableRelationship> relationships;

    public SqlAnalysisResult() {
        this.schemas = new HashMap<>();
        this.relationships = new ArrayList<>();
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public void setSourcePath(String sourcePath) {
        this.sourcePath = sourcePath;
    }

    public SqlDialect getDialect() {
        return dialect;
    }

    public void setDialect(SqlDialect dialect) {
        this.dialect = dialect;
    }

    public Map<String, SqlSchema> getSchemas() {
        return schemas;
    }

    public void addSchema(SqlSchema schema) {
        this.schemas.put(schema.getName(), schema);
    }

    public SqlSchema getOrCreateSchema(String name) {
        return this.schemas.computeIfAbsent(name, SqlSchema::new);
    }

    public List<TableRelationship> getRelationships() {
        return relationships;
    }

    public void addRelationship(TableRelationship relationship) {
        this.relationships.add(relationship);
    }

    public int getTableCount() {
        return schemas.values().stream().mapToInt(s -> s.getTableCount()).sum();
    }

    public int getViewCount() {
        return schemas.values().stream().mapToInt(s -> s.getViewCount()).sum();
    }

    public int getColumnCount() {
        return schemas.values().stream()
                .flatMap(s -> s.getTables().values().stream())
                .mapToInt(t -> t.getColumnCount())
                .sum();
    }

    public int getRelationshipCount() {
        return relationships.size();
    }

    /**
     * Represents a relationship between tables (foreign key, join, etc.)
     */
    public static class TableRelationship {
        private String type;
        private String sourceSchema;
        private String sourceTable;
        private String sourceColumn;
        private String targetSchema;
        private String targetTable;
        private String targetColumn;
        private String constraintName;

        public TableRelationship(String type, String sourceTable, String sourceColumn, 
                                 String targetTable, String targetColumn) {
            this.type = type;
            this.sourceTable = sourceTable;
            this.sourceColumn = sourceColumn;
            this.targetTable = targetTable;
            this.targetColumn = targetColumn;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getSourceSchema() {
            return sourceSchema;
        }

        public void setSourceSchema(String sourceSchema) {
            this.sourceSchema = sourceSchema;
        }

        public String getSourceTable() {
            return sourceTable;
        }

        public void setSourceTable(String sourceTable) {
            this.sourceTable = sourceTable;
        }

        public String getSourceColumn() {
            return sourceColumn;
        }

        public void setSourceColumn(String sourceColumn) {
            this.sourceColumn = sourceColumn;
        }

        public String getTargetSchema() {
            return targetSchema;
        }

        public void setTargetSchema(String targetSchema) {
            this.targetSchema = targetSchema;
        }

        public String getTargetTable() {
            return targetTable;
        }

        public void setTargetTable(String targetTable) {
            this.targetTable = targetTable;
        }

        public String getTargetColumn() {
            return targetColumn;
        }

        public void setTargetColumn(String targetColumn) {
            this.targetColumn = targetColumn;
        }

        public String getConstraintName() {
            return constraintName;
        }

        public void setConstraintName(String constraintName) {
            this.constraintName = constraintName;
        }
    }
}
