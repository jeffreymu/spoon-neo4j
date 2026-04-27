package com.example.analyzer.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains the complete analysis result including all nodes and relationships.
 */
public class AnalysisResult {
    private List<GraphNode> nodes;
    private List<GraphRelationship> relationships;
    private String projectName;
    private String projectPath;

    public AnalysisResult() {
        this.nodes = new ArrayList<>();
        this.relationships = new ArrayList<>();
    }

    public void addNode(GraphNode node) {
        this.nodes.add(node);
    }

    public void addRelationship(GraphRelationship relationship) {
        this.relationships.add(relationship);
    }

    public List<GraphNode> getNodes() {
        return nodes;
    }

    public List<GraphRelationship> getRelationships() {
        return relationships;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getProjectPath() {
        return projectPath;
    }

    public void setProjectPath(String projectPath) {
        this.projectPath = projectPath;
    }

    public int getNodeCount() {
        return nodes.size();
    }

    public int getRelationshipCount() {
        return relationships.size();
    }
}
