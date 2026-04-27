package com.example.analyzer.model;

/**
 * Represents a relationship between two nodes in the knowledge graph.
 */
public class GraphRelationship {
    private String id;
    private String type;
    private String sourceId;
    private String targetId;
    private String sourceType;
    private String targetType;

    public GraphRelationship(String id, String type, String sourceId, String targetId, 
                             String sourceType, String targetType) {
        this.id = id;
        this.type = type;
        this.sourceId = sourceId;
        this.targetId = targetId;
        this.sourceType = sourceType;
        this.targetType = targetType;
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getSourceId() {
        return sourceId;
    }

    public String getTargetId() {
        return targetId;
    }

    public String getSourceType() {
        return sourceType;
    }

    public String getTargetType() {
        return targetType;
    }

    @Override
    public String toString() {
        return "GraphRelationship{" +
                "type='" + type + '\'' +
                ", sourceId='" + sourceId + '\'' +
                ", targetId='" + targetId + '\'' +
                '}';
    }
}
