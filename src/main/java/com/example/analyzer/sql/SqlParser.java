package com.example.analyzer.sql;

import com.example.analyzer.sql.model.*;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.*;
import net.sf.jsqlparser.statement.create.table.*;
import net.sf.jsqlparser.statement.create.view.*;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.schema.Table;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * SQL parser for extracting database structure from SQL files.
 */
public class SqlParser {

    private SqlDialect dialect;
    private SqlAnalysisResult result;

    public SqlParser() {
        this(SqlDialect.GENERIC);
    }

    public SqlParser(SqlDialect dialect) {
        this.dialect = dialect;
    }

    public SqlAnalysisResult parseFile(String filePath) throws IOException {
        File file = new File(filePath);
        String content = readFileContent(file);
        return parseContent(content, file.getName());
    }

    public SqlAnalysisResult parseContent(String sqlContent, String sourceName) {
        this.result = new SqlAnalysisResult();
        this.result.setSourceName(sourceName);
        this.result.setDialect(dialect);

        String[] statements = splitStatements(sqlContent);

        for (String sql : statements) {
            sql = sql.trim();
            if (sql.isEmpty()) {
                continue;
            }

            try {
                Statement statement = CCJSqlParserUtil.parse(sql);
                processStatement(statement);
            } catch (JSQLParserException e) {
                System.err.println("Warning: Could not parse statement: " + 
                    sql.substring(0, Math.min(50, sql.length())) + "...");
            }
        }

        return result;
    }

    private String[] splitStatements(String sqlContent) {
        List<String> statements = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inBlock = false;
        String delimiter = ";";

        String[] lines = sqlContent.split("\n");
        for (String line : lines) {
            String trimmedLine = line.trim().toUpperCase();
            
            if (trimmedLine.startsWith("DELIMITER ")) {
                if (current.length() > 0) {
                    statements.add(current.toString());
                    current = new StringBuilder();
                }
                delimiter = line.trim().substring(10).trim();
                continue;
            }

            if (trimmedLine.contains("BEGIN") || trimmedLine.contains("DECLARE")) {
                inBlock = true;
            }
            if (trimmedLine.equals("END") || trimmedLine.equals("END;")) {
                inBlock = false;
                current.append(line).append("\n");
                statements.add(current.toString());
                current = new StringBuilder();
                continue;
            }

            current.append(line).append("\n");

            if (!inBlock && line.trim().endsWith(delimiter)) {
                String stmt = current.toString().trim();
                if (stmt.endsWith(delimiter)) {
                    stmt = stmt.substring(0, stmt.length() - delimiter.length()).trim();
                }
                if (!stmt.isEmpty()) {
                    statements.add(stmt);
                }
                current = new StringBuilder();
            }
        }

        if (current.length() > 0) {
            String stmt = current.toString().trim();
            if (!stmt.isEmpty()) {
                statements.add(stmt);
            }
        }

        return statements.toArray(new String[0]);
    }

    private void processStatement(Statement statement) {
        if (statement instanceof CreateTable) {
            processCreateTable((CreateTable) statement);
        } else if (statement instanceof CreateView) {
            processCreateView((CreateView) statement);
        } else if (statement instanceof Select) {
            processSelect((Select) statement);
        }
    }

    private void processCreateTable(CreateTable createTable) {
        Table table = createTable.getTable();
        String schemaName = table.getSchemaName() != null ? table.getSchemaName() : "public";
        String tableName = table.getName();

        SqlSchema schema = result.getOrCreateSchema(schemaName);
        SqlTable sqlTable = new SqlTable(tableName);
        sqlTable.setSchemaName(schemaName);

        if (createTable.getColumnDefinitions() != null) {
            for (ColumnDefinition colDef : createTable.getColumnDefinitions()) {
                SqlColumn column = new SqlColumn(colDef.getColumnName());
                column.setDataType(colDef.getColDataType().toString());
                
                if (colDef.getColumnSpecs() != null) {
                    List<String> specs = colDef.getColumnSpecs();
                    for (int i = 0; i < specs.size(); i++) {
                        String spec = specs.get(i).toUpperCase();
                        if ("NOT".equals(spec) && i + 1 < specs.size() && 
                            "NULL".equals(specs.get(i + 1).toUpperCase())) {
                            column.setNullable(false);
                        }
                        if ("AUTO_INCREMENT".equals(spec) || "AUTOINCREMENT".equals(spec)) {
                            column.setAutoIncrement(true);
                        }
                        if ("PRIMARY".equals(spec) && i + 1 < specs.size() && 
                            "KEY".equals(specs.get(i + 1).toUpperCase())) {
                            column.setPrimaryKey(true);
                            sqlTable.addPrimaryKeyColumn(column.getName());
                        }
                        if ("DEFAULT".equals(spec) && i + 1 < specs.size()) {
                            column.setDefaultValue(specs.get(i + 1));
                        }
                    }
                }
                
                sqlTable.addColumn(column);
            }
        }

        if (createTable.getIndexes() != null) {
            for (Index index : createTable.getIndexes()) {
                if ("PRIMARY KEY".equalsIgnoreCase(index.getType())) {
                    for (String colName : index.getColumnsNames()) {
                        sqlTable.addPrimaryKeyColumn(colName.replace("`", "").replace("\"", ""));
                    }
                }
            }
        }

        schema.addTable(sqlTable);
    }

    private void processCreateView(CreateView createView) {
        Table view = createView.getView();
        String schemaName = view.getSchemaName() != null ? view.getSchemaName() : "public";
        String viewName = view.getName();

        SqlSchema schema = result.getOrCreateSchema(schemaName);
        SqlView sqlView = new SqlView(viewName);
        sqlView.setSchemaName(schemaName);

        Select select = createView.getSelect();
        if (select != null && select.getSelectBody() != null) {
            sqlView.setDefinition(select.getSelectBody().toString());
            extractTablesFromSelect(select.getSelectBody(), sqlView);
        }

        schema.addView(sqlView);
    }

    private void processSelect(Select select) {
        if (select.getSelectBody() != null) {
            extractTablesFromSelect(select.getSelectBody(), null);
        }
    }

    private void extractTablesFromSelect(SelectBody selectBody, SqlView view) {
        if (selectBody instanceof PlainSelect) {
            PlainSelect plainSelect = (PlainSelect) selectBody;
            
            if (plainSelect.getFromItem() != null) {
                extractTableFromFromItem(plainSelect.getFromItem(), view);
            }
            
            if (plainSelect.getJoins() != null) {
                for (Join join : plainSelect.getJoins()) {
                    if (join.getRightItem() != null) {
                        extractTableFromFromItem(join.getRightItem(), view);
                    }
                }
            }
            
            if (plainSelect.getSelectItems() != null) {
                for (SelectItem item : plainSelect.getSelectItems()) {
                    if (item instanceof SubSelect) {
                        extractTablesFromSelect(((SubSelect) item).getSelectBody(), view);
                    }
                }
            }
        } else if (selectBody instanceof SetOperationList) {
            SetOperationList setOp = (SetOperationList) selectBody;
            if (setOp.getSelects() != null) {
                for (Object select : setOp.getSelects()) {
                    if (select instanceof SelectBody) {
                        extractTablesFromSelect((SelectBody) select, view);
                    }
                }
            }
        }
    }

    private void extractTableFromFromItem(FromItem fromItem, SqlView view) {
        if (fromItem instanceof Table) {
            Table table = (Table) fromItem;
            String tableName = table.getName();
            if (view != null) {
                view.addReferencedTable(tableName);
            }
        } else if (fromItem instanceof SubSelect) {
            extractTablesFromSelect(((SubSelect) fromItem).getSelectBody(), view);
        }
    }

    private String readFileContent(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] data = new byte[(int) file.length()];
            fis.read(data);
            return new String(data, StandardCharsets.UTF_8);
        }
    }
}
