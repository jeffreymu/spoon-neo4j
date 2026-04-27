#!/usr/bin/env python3
"""
Export Neo4j knowledge graph data to Markdown format for graphify skill.
Usage: python export_neo4j_to_markdown.py <neo4j_uri> <username> <password> <output_file>
"""

import sys
from neo4j import GraphDatabase
from datetime import datetime

def export_to_markdown(uri, username, password, output_file):
    """Export Neo4j data to Markdown format."""
    driver = GraphDatabase.driver(uri, auth=(username, password))
    
    markdown_content = []
    
    try:
        with driver.session() as session:
            # Header
            markdown_content.append("# Java Project Knowledge Graph\n")
            markdown_content.append(f"Generated: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")
            markdown_content.append("---\n")
            
            # Statistics
            result = session.run("MATCH (n) RETURN count(n) as count")
            node_count = result.single()["count"]
            
            result = session.run("MATCH ()-[r]->() RETURN count(r) as count")
            rel_count = result.single()["count"]
            
            markdown_content.append(f"\n## Statistics\n")
            markdown_content.append(f"- Total Nodes: {node_count}\n")
            markdown_content.append(f"- Total Relationships: {rel_count}\n")
            
            # Packages
            markdown_content.append("\n## Packages\n")
            result = session.run("""
                MATCH (p:Package)
                OPTIONAL MATCH (p)<-[:BELONGS_TO]-(c)
                RETURN p.name as name, p.qualifiedName as qname, count(c) as memberCount
                ORDER BY p.qualifiedName
            """)
            for record in result:
                markdown_content.append(f"\n### Package: `{record['qname']}`\n")
                markdown_content.append(f"- Members: {record['memberCount']}\n")
            
            # Classes
            markdown_content.append("\n## Classes\n")
            result = session.run("""
                MATCH (c:Class)
                OPTIONAL MATCH (c)-[:EXTENDS]->(parent)
                OPTIONAL MATCH (c)-[:IMPLEMENTS]->(iface)
                OPTIONAL MATCH (c)-[:BELONGS_TO]->(pkg)
                RETURN c.name as name, c.qualifiedName as qname, 
                       parent.name as parentClass,
                       collect(iface.name) as interfaces,
                       pkg.qualifiedName as packageName,
                       c.isAbstract as isAbstract, c.isFinal as isFinal
                ORDER BY c.qualifiedName
            """)
            for record in result:
                markdown_content.append(f"\n### Class: `{record['name']}`\n")
                markdown_content.append(f"- **Qualified Name**: `{record['qname']}`\n")
                if record['packageName']:
                    markdown_content.append(f"- **Package**: `{record['packageName']}`\n")
                if record['parentClass']:
                    markdown_content.append(f"- **Extends**: `{record['parentClass']}`\n")
                if record['interfaces'] and record['interfaces'][0]:
                    markdown_content.append(f"- **Implements**: {', '.join([f'`{i}`' for i in record['interfaces']])}\n")
                if record['isAbstract']:
                    markdown_content.append(f"- **Abstract**: Yes\n")
                if record['isFinal']:
                    markdown_content.append(f"- **Final**: Yes\n")
                
                # Fields
                field_result = session.run("""
                    MATCH (c:Class {qualifiedName: $qname})-[:HAS_FIELD]->(f:Field)
                    RETURN f.name as name, f.type as type, f.isStatic as isStatic, f.isFinal as isFinal
                    ORDER BY f.name
                """, qname=record['qname'])
                fields = list(field_result)
                if fields:
                    markdown_content.append(f"\n**Fields:**\n")
                    for f in fields:
                        modifiers = []
                        if f['isStatic']:
                            modifiers.append("static")
                        if f['isFinal']:
                            modifiers.append("final")
                        mod_str = " ".join(modifiers) + " " if modifiers else ""
                        markdown_content.append(f"- `{mod_str}{f['type']} {f['name']}`\n")
                
                # Methods
                method_result = session.run("""
                    MATCH (c:Class {qualifiedName: $qname})-[:DECLARES]->(m:Method)
                    RETURN m.name as name, m.qualifiedName as signature, m.returnType as returnType,
                           m.isStatic as isStatic, m.isPublic as isPublic, m.isPrivate as isPrivate,
                           m.isAbstract as isAbstract, m.isFinal as isFinal, m.parameterCount as paramCount
                    ORDER BY m.name
                """, qname=record['qname'])
                methods = list(method_result)
                if methods:
                    markdown_content.append(f"\n**Methods:**\n")
                    for m in methods:
                        modifiers = []
                        if m['isPublic']:
                            modifiers.append("public")
                        elif m['isPrivate']:
                            modifiers.append("private")
                        if m['isStatic']:
                            modifiers.append("static")
                        if m['isAbstract']:
                            modifiers.append("abstract")
                        if m['isFinal']:
                            modifiers.append("final")
                        mod_str = " ".join(modifiers) + " " if modifiers else ""
                        ret = m['returnType'] if m['returnType'] else "void"
                        markdown_content.append(f"- `{mod_str}{ret} {m['name']}(...)`\n")
                
                # Constructors
                ctor_result = session.run("""
                    MATCH (c:Class {qualifiedName: $qname})-[:HAS_CONSTRUCTOR]->(ctor:Constructor)
                    RETURN ctor.qualifiedName as signature, ctor.parameterCount as paramCount
                    ORDER BY ctor.parameterCount
                """, qname=record['qname'])
                ctors = list(ctor_result)
                if ctors:
                    markdown_content.append(f"\n**Constructors:**\n")
                    for ctor in ctors:
                        markdown_content.append(f"- `{ctor['signature']}`\n")
            
            # Interfaces
            markdown_content.append("\n## Interfaces\n")
            result = session.run("""
                MATCH (i:Interface)
                OPTIONAL MATCH (i)-[:BELONGS_TO]->(pkg)
                OPTIONAL MATCH (i)-[:EXTENDS]->(parent)
                RETURN i.name as name, i.qualifiedName as qname, 
                       pkg.qualifiedName as packageName,
                       parent.name as parentInterface
                ORDER BY i.qualifiedName
            """)
            interfaces = list(result)
            if interfaces:
                for record in interfaces:
                    markdown_content.append(f"\n### Interface: `{record['name']}`\n")
                    markdown_content.append(f"- **Qualified Name**: `{record['qname']}`\n")
                    if record['packageName']:
                        markdown_content.append(f"- **Package**: `{record['packageName']}`\n")
                    if record['parentInterface']:
                        markdown_content.append(f"- **Extends**: `{record['parentInterface']}`\n")
            else:
                markdown_content.append("\n*No interfaces found.*\n")
            
            # Method Call Graph
            markdown_content.append("\n## Method Call Graph\n")
            markdown_content.append("\nThis section shows the method invocation relationships.\n")
            result = session.run("""
                MATCH (m1:Method)-[:CALLS]->(m2:Method)
                OPTIONAL MATCH (c1)-[:DECLARES]->(m1)
                OPTIONAL MATCH (c2)-[:DECLARES]->(m2)
                RETURN c1.name as callerClass, m1.name as callerMethod, 
                       c2.name as calleeClass, m2.name as calleeMethod
                ORDER BY callerClass, callerMethod, calleeClass, calleeMethod
                LIMIT 100
            """)
            calls = list(result)
            if calls:
                markdown_content.append("\n| Caller | Method | Callee | Method |\n")
                markdown_content.append("|--------|--------|--------|--------|\n")
                for call in calls:
                    markdown_content.append(f"| `{call['callerClass']}` | `{call['callerMethod']}` | `{call['calleeClass']}` | `{call['calleeMethod']}` |\n")
            else:
                markdown_content.append("\n*No method calls found.*\n")
            
            # Dependencies
            markdown_content.append("\n## Type Dependencies\n")
            result = session.run("""
                MATCH (t1)-[:DEPENDS_ON]->(t2)
                WHERE labels(t1)[0] IN ['Class', 'Interface'] AND labels(t2)[0] IN ['Class', 'Interface']
                RETURN labels(t1)[0] as sourceType, t1.name as sourceName, 
                       labels(t2)[0] as targetType, t2.name as targetName
                ORDER BY sourceName, targetName
                LIMIT 100
            """)
            deps = list(result)
            if deps:
                markdown_content.append("\n| Source | Type | Target | Type |\n")
                markdown_content.append("|--------|------|--------|------|\n")
                for dep in deps:
                    markdown_content.append(f"| `{dep['sourceName']}` | {dep['sourceType']} | `{dep['targetName']}` | {dep['targetType']} |\n")
            else:
                markdown_content.append("\n*No dependencies found.*\n")
            
            # Package Structure
            markdown_content.append("\n## Package Structure\n")
            markdown_content.append("\n```\n")
            result = session.run("""
                MATCH (p:Package)
                OPTIONAL MATCH (p)<-[:BELONGS_TO]-(c:Class)
                OPTIONAL MATCH (p)<-[:BELONGS_TO]-(i:Interface)
                RETURN p.qualifiedName as package, 
                       collect(DISTINCT c.name) as classes,
                       collect(DISTINCT i.name) as interfaces
                ORDER BY p.qualifiedName
            """)
            for record in result:
                pkg = record['package']
                classes = [c for c in record['classes'] if c]
                ifaces = [i for i in record['interfaces'] if i]
                markdown_content.append(f"{pkg}/\n")
                for c in classes:
                    markdown_content.append(f"  ├── {c}.java\n")
                for i in ifaces:
                    markdown_content.append(f"  ├── {i}.java (interface)\n")
            markdown_content.append("```\n")
            
    finally:
        driver.close()
    
    # Write to file
    with open(output_file, 'w', encoding='utf-8') as f:
        f.writelines(markdown_content)
    
    print(f"Export completed: {output_file}")
    print(f"Total content: {len(markdown_content)} lines")

if __name__ == "__main__":
    if len(sys.argv) < 5:
        print("Usage: python export_neo4j_to_markdown.py <neo4j_uri> <username> <password> <output_file>")
        print("Example: python export_neo4j_to_markdown.py bolt://localhost:7687 neo4j password output.md")
        sys.exit(1)
    
    uri = sys.argv[1]
    username = sys.argv[2]
    password = sys.argv[3]
    output_file = sys.argv[4]
    
    export_to_markdown(uri, username, password, output_file)
