package com.example.analyzer.sql;

import com.example.analyzer.model.AnalysisResult;
import com.example.analyzer.model.GraphNode;
import com.example.analyzer.model.GraphRelationship;
import com.example.analyzer.sql.model.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * SQL analyzer that converts SQL structure to knowledge graph.
 */
public class SqlAnalyzer {

    private SqlDialect dialect;

    public SqlAnalyzer() {
        this(SqlDialect.GENERIC);
    }

    public SqlAnalyzer(SqlDialect dialect) {
        this.dialect = dialect;
    }

    /**
     * Analyze a SQL file and generate knowledge graph.
     */
    public AnalysisResult analyzeFile(String filePath) throws IOException {
        File file = new File(filePath);
        SqlParser parser = new SqlParser(dialect);
        SqlAnalysisResult sqlResult = parser.parseFile(filePath);
        return convertToGraph(sqlResult);
    }

    /**
     * Analyze SQL content string.
     */
    public AnalysisResult analyzeContent(String sqlContent, String sourceName) {
        SqlParser parser = new SqlParser(dialect);
        SqlAnalysisResult sqlResult = parser.parseContent(sqlContent, sourceName);
        return convertToGraph(sqlResult);
    }

    /**
     * Convert SQL analysis result to knowledge graph format.
     */
    private AnalysisResult convertToGraph(SqlAnalysisResult sqlResult) {
        AnalysisResult result = new AnalysisResult();
        result.setProjectName(sqlResult.getSourceName());
        result.setProjectPath(sqlResult.getSourcePath());

        // Create database node
        String dbName = sqlResult.getSourceName() != null ? sqlResult.getSourceName() : "database";
        GraphNode dbNode = new GraphNode("db_" + dbName, "Database", dbName, dbName);
        dbNode.addProperty("dialect", sqlResult.getDialect() != null ? 
            sqlResult.getDialect().getDisplayName() : "Generic");
        result.addNode(dbNode);

        // Process schemas
        for (SqlSchema schema : sqlResult.getSchemas().values()) {
            GraphNode schemaNode = new GraphNode(
                "schema_" + schema.getName(), 
                "Schema", 
                schema.getName(), 
                schema.getName()
            );
            schemaNode.addProperty("tableCount", schema.getTableCount());
            schemaNode.addProperty("viewCount", schema.getViewCount());
            result.addNode(schemaNode);

            // Schema belongs to database
            result.addRelationship(new GraphRelationship(
                "rel_db_schema_" + dbName + "_" + schema.getName(),
                "HAS_SCHEMA",
                "db_" + dbName,
                "schema_" + schema.getName(),
                "Database",
                "Schema"
            ));

            // Process tables
            for (SqlTable table : schema.getTables().values()) {
                createTableNodes(result, table, schema.getName());
            }

            // Process views
            for (SqlView view : schema.getViews().values()) {
                createViewNodes(result, view, schema.getName());
            }
        }

        // Process relationships (foreign keys, etc.)
        for (SqlAnalysisResult.TableRelationship rel : sqlResult.getRelationships()) {
            createRelationshipEdge(result, rel);
        }

        return result;
    }

    private void createTableNodes(AnalysisResult result, SqlTable table, String schemaName) {
        // Create table node
        String tableId = "table_" + schemaName + "_" + table.getName();
        GraphNode tableNode = new GraphNode(tableId, "Table", table.getName(), table.getQualifiedName());
        tableNode.addProperty("columnCount", table.getColumnCount());
        tableNode.addProperty("tableType", table.getTableType());
        if (table.getComment() != null) {
            tableNode.addProperty("comment", table.getComment());
        }
        result.addNode(tableNode);

        // Table belongs to schema
        result.addRelationship(new GraphRelationship(
            "rel_schema_table_" + schemaName + "_" + table.getName(),
            "HAS_TABLE",
            "schema_" + schemaName,
            tableId,
            "Schema",
            "Table"
        ));

        // Create column nodes
        for (SqlColumn column : table.getColumns().values()) {
            String columnId = "column_" + schemaName + "_" + table.getName() + "_" + column.getName();
            GraphNode columnNode = new GraphNode(columnId, "Column", column.getName(), column.getQualifiedName());
            columnNode.addProperty("dataType", column.getDataType());
            columnNode.addProperty("nullable", column.isNullable());
            columnNode.addProperty("primaryKey", column.isPrimaryKey());
            columnNode.addProperty("foreignKey", column.isForeignKey());
            columnNode.addProperty("autoIncrement", column.isAutoIncrement());
            if (column.getDefaultValue() != null) {
                columnNode.addProperty("defaultValue", column.getDefaultValue());
            }
            if (column.getComment() != null) {
                columnNode.addProperty("comment", column.getComment());
            }
            result.addNode(columnNode);

            // Column belongs to table
            result.addRelationship(new GraphRelationship(
                "rel_table_column_" + table.getName() + "_" + column.getName(),
                "HAS_COLUMN",
                tableId,
                columnId,
                "Table",
                "Column"
            ));

            // Foreign key relationship
            if (column.isForeignKey() && column.getForeignKeyTable() != null) {
                String targetTableId = "table_" + schemaName + "_" + column.getForeignKeyTable();
                String targetColumnId = "column_" + schemaName + "_" + 
                    column.getForeignKeyTable() + "_" + column.getForeignKeyColumn();
                
                result.addRelationship(new GraphRelationship(
                    "rel_fk_" + columnId + "_" + targetColumnId,
                    "FOREIGN_KEY",
                    columnId,
                    targetColumnId,
                    "Column",
                    "Column"
                ));
            }
        }

        // Create index nodes
        for (SqlIndex index : table.getIndexes()) {
            String indexId = "index_" + schemaName + "_" + table.getName() + "_" + index.getName();
            GraphNode indexNode = new GraphNode(indexId, "Index", index.getName(), index.getQualifiedName());
            indexNode.addProperty("unique", index.isUnique());
            indexNode.addProperty("columns", String.join(", ", index.getColumns()));
            if (index.getType() != null) {
                indexNode.addProperty("type", index.getType());
            }
            result.addNode(indexNode);

            // Index belongs to table
            result.addRelationship(new GraphRelationship(
                "rel_table_index_" + table.getName() + "_" + index.getName(),
                "HAS_INDEX",
                tableId,
                indexId,
                "Table",
                "Index"
            ));
        }
    }

    private void createViewNodes(AnalysisResult result, SqlView view, String schemaName) {
        // Create view node
        String viewId = "view_" + schemaName + "_" + view.getName();
        GraphNode viewNode = new GraphNode(viewId, "View", view.getName(), view.getQualifiedName());
        if (view.getDefinition() != null) {
            viewNode.addProperty("definition", view.getDefinition());
        }
        if (view.getComment() != null) {
            viewNode.addProperty("comment", view.getComment());
        }
        result.addNode(viewNode);

        // View belongs to schema
        result.addRelationship(new GraphRelationship(
            "rel_schema_view_" + schemaName + "_" + view.getName(),
            "HAS_VIEW",
            "schema_" + schemaName,
            viewId,
            "Schema",
            "View"
        ));

        // View references tables
        for (String refTable : view.getReferencedTables()) {
            String tableId = "table_" + schemaName + "_" + refTable;
            result.addRelationship(new GraphRelationship(
                "rel_view_ref_" + view.getName() + "_" + refTable,
                "REFERENCES",
                viewId,
                tableId,
                "View",
                "Table"
            ));
        }
    }

    private void createRelationshipEdge(AnalysisResult result, SqlAnalysisResult.TableRelationship rel) {
        String sourceTableId = "table_" + rel.getSourceSchema() + "_" + rel.getSourceTable();
        String targetTableId = "table_" + rel.getTargetSchema() + "_" + rel.getTargetTable();
        String sourceColumnId = "column_" + rel.getSourceSchema() + "_" + 
            rel.getSourceTable() + "_" + rel.getSourceColumn();
        String targetColumnId = "column_" + rel.getTargetSchema() + "_" + 
            rel.getTargetTable() + "_" + rel.getTargetColumn();

        // Table-level relationship
        result.addRelationship(new GraphRelationship(
            "rel_table_" + rel.getType() + "_" + sourceTableId + "_" + targetTableId,
            rel.getType(),
            sourceTableId,
            targetTableId,
            "Table",
            "Table"
        ));
    }
}
