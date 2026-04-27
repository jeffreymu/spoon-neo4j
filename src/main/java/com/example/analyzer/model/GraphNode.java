package com.example.analyzer.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a node in the knowledge graph.
 */
public class GraphNode {
    private String id;
    private String type;
    private String name;
    private String qualifiedName;
    private Map<String, Object> properties;

    public GraphNode(String id, String type, String name, String qualifiedName) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.qualifiedName = qualifiedName;
        this.properties = new HashMap<>();
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getQualifiedName() {
        return qualifiedName;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void addProperty(String key, Object value) {
        if (value != null) {
            this.properties.put(key, value);
        }
    }

    @Override
    public String toString() {
        return "GraphNode{" +
                "id='" + id + '\'' +
                ", type='" + type + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
