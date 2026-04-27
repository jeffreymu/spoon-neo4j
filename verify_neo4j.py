#!/usr/bin/env python3
"""Verify Neo4j import."""

from neo4j import GraphDatabase

driver = GraphDatabase.driver('bolt://localhost:7687', auth=('neo4j', 'jeff1218'))

with driver.session() as session:
    # 查看一些示例数据
    print('=== 示例类节点 ===')
    result = session.run('MATCH (c:Class) RETURN c.name as name, c.qualifiedName as qname LIMIT 5')
    for r in result:
        print(f'  {r["name"]} ({r["qname"]})')
    
    print('\n=== 示例方法节点 ===')
    result = session.run('MATCH (m:Method) RETURN m.name as name, m.qualifiedName as qname LIMIT 5')
    for r in result:
        print(f'  {r["name"]} ({r["qname"]})')
    
    print('\n=== 方法调用关系示例 ===')
    result = session.run('MATCH (m1:Method)-[:CALLS]->(m2:Method) RETURN m1.name as caller, m2.name as callee LIMIT 5')
    for r in result:
        print(f'  {r["caller"]} -> {r["callee"]}')
    
    print('\n=== 类继承关系 ===')
    result = session.run('MATCH (c1:Class)-[:EXTENDS]->(c2:Class) RETURN c1.name as child, c2.name as parent')
    for r in result:
        print(f'  {r["child"]} extends {r["parent"]}')

driver.close()
