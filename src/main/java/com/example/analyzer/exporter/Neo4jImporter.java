package com.example.analyzer.exporter;

import com.example.analyzer.model.AnalysisResult;
import com.example.analyzer.model.GraphNode;
import com.example.analyzer.model.GraphRelationship;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.neo4j.driver.Result;
import org.neo4j.driver.Record;

import java.util.Map;

/**
 * Imports analysis results directly into Neo4j database.
 */
public class Neo4jImporter implements AutoCloseable {

    private final Driver driver;
    private final String database;

    public Neo4jImporter(String uri, String username, String password) {
        this(uri, username, password, "neo4j");
    }

    public Neo4jImporter(String uri, String username, String password, String database) {
        this.driver = GraphDatabase.driver(uri, AuthTokens.basic(username, password));
        this.database = database;
    }

    /**
     * Import analysis result into Neo4j.
     */
    public void importAnalysis(AnalysisResult result) {
        try (Session session = driver.session()) {
            // Clear existing data (optional)
            clearDatabase(session);
            
            System.out.println("Creating constraints...");
            createConstraints(session);

            System.out.println("Importing " + result.getNodeCount() + " nodes...");
            int nodeCount = 0;
            for (GraphNode node : result.getNodes()) {
                createNode(session, node);
                nodeCount++;
                if (nodeCount % 100 == 0) {
                    System.out.println("  Imported " + nodeCount + " nodes...");
                }
            }

            System.out.println("Importing " + result.getRelationshipCount() + " relationships...");
            int relCount = 0;
            for (GraphRelationship rel : result.getRelationships()) {
                createRelationship(session, rel);
                relCount++;
                if (relCount % 100 == 0) {
                    System.out.println("  Imported " + relCount + " relationships...");
                }
            }

            System.out.println("Import completed successfully!");
        }
    }

    private void clearDatabase(Session session) {
        System.out.println("Clearing existing data...");
        session.run("MATCH (n) DETACH DELETE n");
    }

    private void createConstraints(Session session) {
        // Create unique constraint on node id
        session.run("CREATE CONSTRAINT IF NOT EXISTS FOR (n:Node) REQUIRE n.id IS UNIQUE");
        
        // Create constraints for each node type
        String[] nodeTypes = {"Package", "Class", "Interface", "Enum", "Annotation", "Method", "Constructor", "Field"};
        for (String type : nodeTypes) {
            session.run("CREATE CONSTRAINT IF NOT EXISTS FOR (n:" + type + ") REQUIRE n.id IS UNIQUE");
        }
    }

    private void createNode(Session session, GraphNode node) {
        StringBuilder cypher = new StringBuilder();
        cypher.append("MERGE (n:").append(node.getType()).append(" {id: $id}) ");
        cypher.append("SET n.name = $name, n.qualifiedName = $qualifiedName ");
        
        // Add properties
        Map<String, Object> params = new java.util.HashMap<>();
        params.put("id", node.getId());
        params.put("name", node.getName());
        params.put("qualifiedName", node.getQualifiedName());
        
        int propIndex = 0;
        for (Map.Entry<String, Object> prop : node.getProperties().entrySet()) {
            String paramName = "prop" + propIndex;
            cypher.append(", n.").append(prop.getKey()).append(" = $").append(paramName).append(" ");
            params.put(paramName, prop.getValue());
            propIndex++;
        }

        session.run(cypher.toString(), params);
    }

    private void createRelationship(Session session, GraphRelationship rel) {
        String cypher = String.format(
                "MATCH (source:%s {id: $sourceId}) " +
                "MATCH (target:%s {id: $targetId}) " +
                "MERGE (source)-[:%s]->(target)",
                rel.getSourceType(), rel.getTargetType(), rel.getType()
        );

        Map<String, Object> params = new java.util.HashMap<>();
        params.put("sourceId", rel.getSourceId());
        params.put("targetId", rel.getTargetId());

        session.run(cypher, params);
    }

    /**
     * Verify the import by counting nodes and relationships.
     */
    public void verifyImport() {
        try (Session session = driver.session()) {
            Result nodeResult = session.run("MATCH (n) RETURN count(n) as count");
            long nodeCount = nodeResult.single().get("count").asLong();

            Result relResult = session.run("MATCH ()-[r]->() RETURN count(r) as count");
            long relCount = relResult.single().get("count").asLong();

            System.out.println("\n=== Database Verification ===");
            System.out.println("Total nodes: " + nodeCount);
            System.out.println("Total relationships: " + relCount);

            // Count by node type
            System.out.println("\nNodes by type:");
            Result typeResult = session.run(
                "MATCH (n) " +
                "WITH labels(n) as lbls, count(n) as cnt " +
                "UNWIND lbls as label " +
                "RETURN label, sum(cnt) as count " +
                "ORDER BY count DESC"
            );
            while (typeResult.hasNext()) {
                Record record = typeResult.next();
                System.out.println("  - " + record.get("label").asString() + ": " + record.get("count").asLong());
            }

            // Count by relationship type
            System.out.println("\nRelationships by type:");
            Result relTypeResult = session.run(
                "MATCH ()-[r]->() " +
                "RETURN type(r) as type, count(r) as count " +
                "ORDER BY count DESC"
            );
            while (relTypeResult.hasNext()) {
                Record record = relTypeResult.next();
                System.out.println("  - " + record.get("type").asString() + ": " + record.get("count").asLong());
            }
        }
    }

    @Override
    public void close() {
        driver.close();
    }
}
