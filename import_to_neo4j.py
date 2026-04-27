#!/usr/bin/env python3
"""
Import analysis results to Neo4j database.
Usage: python import_to_neo4j.py <json_file> <neo4j_uri> <username> <password> [database]
"""

import json
import sys
from neo4j import GraphDatabase

def create_constraints(tx):
    """Create unique constraints for node types."""
    # Java node types
    java_node_types = ["Package", "Class", "Interface", "Enum", "Annotation", "Method", "Constructor", "Field"]
    # SQL node types
    sql_node_types = ["Database", "Schema", "Table", "Column", "View", "Index"]
    
    all_types = java_node_types + sql_node_types
    
    for node_type in all_types:
        tx.run(f"CREATE CONSTRAINT IF NOT EXISTS FOR (n:{node_type}) REQUIRE n.id IS UNIQUE")

def create_node(tx, node):
    """Create a node in Neo4j."""
    node_type = node["type"]
    params = {
        "id": node["id"],
        "name": node["name"],
        "qualifiedName": node["qualifiedName"]
    }
    
    # Add properties
    if "properties" in node and node["properties"]:
        for key, value in node["properties"].items():
            params[key] = value
    
    # Build SET clause
    set_clauses = ["n.name = $name", "n.qualifiedName = $qualifiedName"]
    if "properties" in node and node["properties"]:
        for key in node["properties"].keys():
            set_clauses.append(f"n.{key} = ${key}")
    
    cypher = f"MERGE (n:{node_type} {{id: $id}}) SET {', '.join(set_clauses)}"
    tx.run(cypher, params)

def create_relationship(tx, rel):
    """Create a relationship in Neo4j."""
    cypher = f"""
    MATCH (source:{rel['sourceType']} {{id: $sourceId}})
    MATCH (target:{rel['targetType']} {{id: $targetId}})
    MERGE (source)-[:{rel['type']}]->(target)
    """
    tx.run(cypher, sourceId=rel["sourceId"], targetId=rel["targetId"])

def database_exists(driver, database_name):
    """Check if a database exists."""
    with driver.session(database="system") as session:
        result = session.run("SHOW DATABASES WHERE name = $name", name=database_name)
        return len(list(result)) > 0

def create_database(driver, database_name):
    """Create a new database if it doesn't exist."""
    with driver.session(database="system") as session:
        # Check if database exists
        result = session.run("SHOW DATABASES WHERE name = $name", name=database_name)
        if len(list(result)) == 0:
            print(f"Creating database '{database_name}'...")
            session.run(f"CREATE DATABASE `{database_name}`")
            print(f"Database '{database_name}' created successfully.")
        else:
            print(f"Database '{database_name}' already exists. Data will be added to existing data.")

def import_data(json_file, uri, username, password, database="neo4j"):
    """Import data from JSON file to Neo4j."""
    print(f"Loading data from {json_file}...")
    with open(json_file, 'r', encoding='utf-8') as f:
        data = json.load(f)
    
    print(f"Project: {data.get('projectName', 'unknown')}")
    print(f"Nodes: {data.get('nodeCount', 0)}")
    print(f"Relationships: {data.get('relationshipCount', 0)}")
    print(f"Target database: {database}")
    
    driver = GraphDatabase.driver(uri, auth=(username, password))
    
    try:
        # Create database if it doesn't exist (only for Neo4j Enterprise)
        # For Neo4j Community, we can only use the default 'neo4j' database
        if database != "neo4j":
            try:
                create_database(driver, database)
            except Exception as e:
                print(f"Note: Could not create database '{database}'. This might be Neo4j Community edition.")
                print(f"Using default 'neo4j' database instead.")
                database = "neo4j"
        
        with driver.session(database=database) as session:
            # Create constraints
            print("\nCreating constraints...")
            session.execute_write(create_constraints)
            
            # Import nodes
            print(f"Importing {len(data['nodes'])} nodes...")
            for i, node in enumerate(data["nodes"]):
                session.execute_write(create_node, node)
                if (i + 1) % 100 == 0:
                    print(f"  Imported {i + 1} nodes...")
            
            # Import relationships
            print(f"Importing {len(data['relationships'])} relationships...")
            for i, rel in enumerate(data["relationships"]):
                session.execute_write(create_relationship, rel)
                if (i + 1) % 100 == 0:
                    print(f"  Imported {i + 1} relationships...")
            
            # Verify import
            print("\nVerifying import...")
            result = session.run("MATCH (n) RETURN count(n) as count")
            node_count = result.single()["count"]
            
            result = session.run("MATCH ()-[r]->() RETURN count(r) as count")
            rel_count = result.single()["count"]
            
            print(f"\n=== Database Verification ===")
            print(f"Database: {database}")
            print(f"Total nodes: {node_count}")
            print(f"Total relationships: {rel_count}")
            
            # Count by node type
            print("\nNodes by type:")
            result = session.run("""
                MATCH (n)
                WITH labels(n) as lbls, count(n) as cnt
                UNWIND lbls as label
                RETURN label, sum(cnt) as count
                ORDER BY count DESC
            """)
            for record in result:
                print(f"  - {record['label']}: {record['count']}")
            
            # Count by relationship type
            print("\nRelationships by type:")
            result = session.run("""
                MATCH ()-[r]->()
                RETURN type(r) as type, count(r) as count
                ORDER BY count DESC
            """)
            for record in result:
                print(f"  - {record['type']}: {record['count']}")
            
    finally:
        driver.close()
    
    print("\nImport completed successfully!")

if __name__ == "__main__":
    if len(sys.argv) < 5:
        print("Usage: python import_to_neo4j.py <json_file> <neo4j_uri> <username> <password> [database]")
        print("Example: python import_to_neo4j.py output/analysis_result.json bolt://localhost:7687 neo4j password myproject")
        print("\nNote: Database parameter is optional. Default is 'neo4j'.")
        print("      Creating new databases requires Neo4j Enterprise edition.")
        print("      For Neo4j Community, only the default 'neo4j' database is available.")
        sys.exit(1)
    
    json_file = sys.argv[1]
    uri = sys.argv[2]
    username = sys.argv[3]
    password = sys.argv[4]
    database = sys.argv[5] if len(sys.argv) > 5 else "neo4j"
    
    import_data(json_file, uri, username, password, database)
