package com.example;

import com.example.analyzer.ProjectAnalyzer;
import com.example.analyzer.exporter.Neo4jExporter;
import com.example.analyzer.exporter.Neo4jImporter;
import com.example.analyzer.model.AnalysisResult;
import com.example.analyzer.sql.SqlAnalyzer;
import com.example.analyzer.sql.SqlDialect;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.util.concurrent.Callable;

/**
 * Main application for analyzing Java projects and SQL files.
 * Generates knowledge graph data for Neo4j import.
 */
@Command(name = "code-analyzer", mixinStandardHelpOptions = true, version = "1.0",
        description = "Analyzes Java projects and SQL files, generates Neo4j-compatible knowledge graph data.",
        subcommands = {App.SqlCommand.class})
public class App implements Callable<Integer> {

    @Option(names = {"-p", "--path"}, description = "Path to the Java project to analyze")
    private String projectPath;

    @Option(names = {"-o", "--output"}, description = "Output directory for generated files", defaultValue = "./output")
    private String outputDir;

    @Option(names = {"-f", "--format"}, description = "Output format: cypher, csv, json, or all", defaultValue = "all")
    private String format;

    @Option(names = {"-v", "--verbose"}, description = "Enable verbose output")
    private boolean verbose;

    @Option(names = {"--import-neo4j"}, description = "Import directly to Neo4j after analysis")
    private boolean importToNeo4j;

    @Option(names = {"--neo4j-uri"}, description = "Neo4j URI", defaultValue = "bolt://localhost:7687")
    private String neo4jUri;

    @Option(names = {"--neo4j-user"}, description = "Neo4j username", defaultValue = "neo4j")
    private String neo4jUser;

    @Option(names = {"--neo4j-password"}, description = "Neo4j password")
    private String neo4jPassword;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new App()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        // Default: analyze Java project
        return analyzeJava();
    }

    private Integer analyzeJava() throws Exception {
        try {
            // Validate input path
            File projectDir = new File(projectPath);
            if (!projectDir.exists() || !projectDir.isDirectory()) {
                System.err.println("Error: Project path does not exist or is not a directory: " + projectPath);
                return 1;
            }

            // Create output directory
            File output = new File(outputDir);
            if (!output.exists()) {
                output.mkdirs();
            }

            System.out.println("========================================");
            System.out.println("Java Project Analyzer (Spoon-based)");
            System.out.println("========================================");
            System.out.println("Project path: " + projectPath);
            System.out.println("Output directory: " + outputDir);
            System.out.println("Output format: " + format);
            if (importToNeo4j) {
                System.out.println("Import to Neo4j: " + neo4jUri);
            }
            System.out.println();

            // Analyze the project
            System.out.println("Analyzing project...");
            long startTime = System.currentTimeMillis();

            ProjectAnalyzer analyzer = new ProjectAnalyzer(projectPath);
            AnalysisResult result = analyzer.analyze();

            long endTime = System.currentTimeMillis();
            System.out.println("Analysis completed in " + (endTime - startTime) + " ms");
            System.out.println();

            // Print summary
            printSummary(result);

            // Export results
            exportResults(result);

            // Import to Neo4j if requested
            if (importToNeo4j) {
                importToNeo4j(result);
            }

            System.out.println("\n========================================");
            System.out.println("Analysis completed successfully!");
            System.out.println("========================================");

            return 0;

        } catch (Exception e) {
            System.err.println("Error during analysis: " + e.getMessage());
            if (verbose) {
                e.printStackTrace();
            }
            return 1;
        }
    }

    private void exportResults(AnalysisResult result) throws Exception {
        System.out.println("\nExporting results...");
        Neo4jExporter exporter = new Neo4jExporter(result);

        String formatLower = format.toLowerCase();
        if (formatLower.equals("all") || formatLower.equals("cypher")) {
            String cypherPath = outputDir + "/neo4j_import.cypher";
            exporter.exportToCypher(cypherPath);
            System.out.println("  - Cypher script: " + cypherPath);
        }

        if (formatLower.equals("all") || formatLower.equals("csv")) {
            exporter.exportToCsv(outputDir);
            System.out.println("  - CSV files: " + outputDir + "/nodes.csv, " + outputDir + "/relationships.csv");
        }

        if (formatLower.equals("all") || formatLower.equals("json")) {
            String jsonPath = outputDir + "/analysis_result.json";
            exporter.exportToJson(jsonPath);
            System.out.println("  - JSON file: " + jsonPath);
        }
    }

    private void importToNeo4j(AnalysisResult result) throws Exception {
        if (neo4jPassword == null || neo4jPassword.isEmpty()) {
            System.err.println("Error: Neo4j password is required for import. Use --neo4j-password option.");
            return;
        }
        
        System.out.println("\n========================================");
        System.out.println("Importing to Neo4j...");
        System.out.println("========================================");
        
        try (Neo4jImporter importer = new Neo4jImporter(neo4jUri, neo4jUser, neo4jPassword)) {
            importer.importAnalysis(result);
            importer.verifyImport();
        } catch (Exception e) {
            System.err.println("Error importing to Neo4j: " + e.getMessage());
            if (verbose) {
                e.printStackTrace();
            }
            throw e;
        }
    }

    private void printSummary(AnalysisResult result) {
        System.out.println("=== Analysis Summary ===");
        System.out.println("Project: " + result.getProjectName());
        System.out.println("Total nodes: " + result.getNodeCount());
        System.out.println("Total relationships: " + result.getRelationshipCount());
        System.out.println();

        // Count by type
        java.util.Map<String, Integer> nodeTypeCounts = new java.util.HashMap<>();
        for (com.example.analyzer.model.GraphNode node : result.getNodes()) {
            nodeTypeCounts.merge(node.getType(), 1, Integer::sum);
        }

        System.out.println("Nodes by type:");
        for (java.util.Map.Entry<String, Integer> entry : nodeTypeCounts.entrySet()) {
            System.out.println("  - " + entry.getKey() + ": " + entry.getValue());
        }

        // Count relationships by type
        java.util.Map<String, Integer> relTypeCounts = new java.util.HashMap<>();
        for (com.example.analyzer.model.GraphRelationship rel : result.getRelationships()) {
            relTypeCounts.merge(rel.getType(), 1, Integer::sum);
        }

        System.out.println("\nRelationships by type:");
        for (java.util.Map.Entry<String, Integer> entry : relTypeCounts.entrySet()) {
            System.out.println("  - " + entry.getKey() + ": " + entry.getValue());
        }
    }

    /**
     * SQL analysis subcommand.
     */
    @Command(name = "sql", description = "Analyze SQL files")
    public static class SqlCommand implements Callable<Integer> {

        @Option(names = {"-p", "--path"}, description = "Path to the SQL file or directory", required = true)
        private String sqlPath;

        @Option(names = {"-o", "--output"}, description = "Output directory for generated files", defaultValue = "./output")
        private String outputDir;

        @Option(names = {"-f", "--format"}, description = "Output format: cypher, csv, json, or all", defaultValue = "all")
        private String format;

        @Option(names = {"-d", "--dialect"}, description = "SQL dialect: mysql, oracle, generic", defaultValue = "generic")
        private String dialect;

        @Option(names = {"-v", "--verbose"}, description = "Enable verbose output")
        private boolean verbose;

        @Option(names = {"--import-neo4j"}, description = "Import directly to Neo4j after analysis")
        private boolean importToNeo4j;

        @Option(names = {"--neo4j-uri"}, description = "Neo4j URI", defaultValue = "bolt://localhost:7687")
        private String neo4jUri;

        @Option(names = {"--neo4j-user"}, description = "Neo4j username", defaultValue = "neo4j")
        private String neo4jUser;

        @Option(names = {"--neo4j-password"}, description = "Neo4j password")
        private String neo4jPassword;

        @Override
        public Integer call() throws Exception {
            try {
                // Validate input path
                File sqlFile = new File(sqlPath);
                if (!sqlFile.exists()) {
                    System.err.println("Error: SQL path does not exist: " + sqlPath);
                    return 1;
                }

                // Create output directory
                File output = new File(outputDir);
                if (!output.exists()) {
                    output.mkdirs();
                }

                // Parse dialect
                SqlDialect sqlDialect;
                switch (dialect.toLowerCase()) {
                    case "mysql":
                        sqlDialect = SqlDialect.MYSQL;
                        break;
                    case "oracle":
                        sqlDialect = SqlDialect.ORACLE;
                        break;
                    default:
                        sqlDialect = SqlDialect.GENERIC;
                }

                System.out.println("========================================");
                System.out.println("SQL Analyzer");
                System.out.println("========================================");
                System.out.println("SQL path: " + sqlPath);
                System.out.println("Dialect: " + sqlDialect.getDisplayName());
                System.out.println("Output directory: " + outputDir);
                System.out.println("Output format: " + format);
                if (importToNeo4j) {
                    System.out.println("Import to Neo4j: " + neo4jUri);
                }
                System.out.println();

                // Analyze SQL
                System.out.println("Analyzing SQL...");
                long startTime = System.currentTimeMillis();

                SqlAnalyzer analyzer = new SqlAnalyzer(sqlDialect);
                AnalysisResult result;

                if (sqlFile.isDirectory()) {
                    // Analyze all SQL files in directory
                    result = new AnalysisResult();
                    result.setProjectName(sqlFile.getName());
                    result.setProjectPath(sqlPath);

                    File[] sqlFiles = sqlFile.listFiles((dir, name) -> 
                        name.toLowerCase().endsWith(".sql"));
                    
                    if (sqlFiles != null) {
                        for (File file : sqlFiles) {
                            System.out.println("  Processing: " + file.getName());
                            try {
                                AnalysisResult fileResult = analyzer.analyzeFile(file.getAbsolutePath());
                                // Merge results
                                for (com.example.analyzer.model.GraphNode node : fileResult.getNodes()) {
                                    result.addNode(node);
                                }
                                for (com.example.analyzer.model.GraphRelationship rel : fileResult.getRelationships()) {
                                    result.addRelationship(rel);
                                }
                            } catch (Exception e) {
                                System.err.println("  Error processing " + file.getName() + ": " + e.getMessage());
                            }
                        }
                    }
                } else {
                    result = analyzer.analyzeFile(sqlPath);
                }

                long endTime = System.currentTimeMillis();
                System.out.println("Analysis completed in " + (endTime - startTime) + " ms");
                System.out.println();

                // Print summary
                printSummary(result);

                // Export results
                exportResults(result);

                // Import to Neo4j if requested
                if (importToNeo4j) {
                    importToNeo4j(result);
                }

                System.out.println("\n========================================");
                System.out.println("Analysis completed successfully!");
                System.out.println("========================================");

                return 0;

            } catch (Exception e) {
                System.err.println("Error during analysis: " + e.getMessage());
                if (verbose) {
                    e.printStackTrace();
                }
                return 1;
            }
        }

        private void exportResults(AnalysisResult result) throws Exception {
            System.out.println("\nExporting results...");
            Neo4jExporter exporter = new Neo4jExporter(result);

            String formatLower = format.toLowerCase();
            if (formatLower.equals("all") || formatLower.equals("cypher")) {
                String cypherPath = outputDir + "/neo4j_import.cypher";
                exporter.exportToCypher(cypherPath);
                System.out.println("  - Cypher script: " + cypherPath);
            }

            if (formatLower.equals("all") || formatLower.equals("csv")) {
                exporter.exportToCsv(outputDir);
                System.out.println("  - CSV files: " + outputDir + "/nodes.csv, " + outputDir + "/relationships.csv");
            }

            if (formatLower.equals("all") || formatLower.equals("json")) {
                String jsonPath = outputDir + "/analysis_result.json";
                exporter.exportToJson(jsonPath);
                System.out.println("  - JSON file: " + jsonPath);
            }
        }

        private void importToNeo4j(AnalysisResult result) throws Exception {
            if (neo4jPassword == null || neo4jPassword.isEmpty()) {
                System.err.println("Error: Neo4j password is required for import. Use --neo4j-password option.");
                return;
            }
            
            System.out.println("\n========================================");
            System.out.println("Importing to Neo4j...");
            System.out.println("========================================");
            
            try (Neo4jImporter importer = new Neo4jImporter(neo4jUri, neo4jUser, neo4jPassword)) {
                importer.importAnalysis(result);
                importer.verifyImport();
            } catch (Exception e) {
                System.err.println("Error importing to Neo4j: " + e.getMessage());
                if (verbose) {
                    e.printStackTrace();
                }
                throw e;
            }
        }

        private void printSummary(AnalysisResult result) {
            System.out.println("=== Analysis Summary ===");
            System.out.println("Source: " + result.getProjectName());
            System.out.println("Total nodes: " + result.getNodeCount());
            System.out.println("Total relationships: " + result.getRelationshipCount());
            System.out.println();

            // Count by type
            java.util.Map<String, Integer> nodeTypeCounts = new java.util.HashMap<>();
            for (com.example.analyzer.model.GraphNode node : result.getNodes()) {
                nodeTypeCounts.merge(node.getType(), 1, Integer::sum);
            }

            System.out.println("Nodes by type:");
            for (java.util.Map.Entry<String, Integer> entry : nodeTypeCounts.entrySet()) {
                System.out.println("  - " + entry.getKey() + ": " + entry.getValue());
            }

            // Count relationships by type
            java.util.Map<String, Integer> relTypeCounts = new java.util.HashMap<>();
            for (com.example.analyzer.model.GraphRelationship rel : result.getRelationships()) {
                relTypeCounts.merge(rel.getType(), 1, Integer::sum);
            }

            System.out.println("\nRelationships by type:");
            for (java.util.Map.Entry<String, Integer> entry : relTypeCounts.entrySet()) {
                System.out.println("  - " + entry.getKey() + ": " + entry.getValue());
            }
        }
    }
}
