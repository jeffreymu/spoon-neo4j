package com.example.analyzer.visitor;

import com.example.analyzer.model.AnalysisResult;
import com.example.analyzer.model.GraphNode;
import com.example.analyzer.model.GraphRelationship;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.visitor.CtAbstractVisitor;

/**
 * Visitor to extract package information from Java code.
 */
public class PackageVisitor extends CtAbstractVisitor {

    private AnalysisResult result;

    public PackageVisitor(AnalysisResult result) {
        this.result = result;
    }

    @Override
    public void visitCtPackage(CtPackage ctPackage) {
        if (ctPackage.isUnnamedPackage()) {
            return;
        }

        String qualifiedName = ctPackage.getQualifiedName();
        String nodeId = "package_" + qualifiedName;

        GraphNode packageNode = new GraphNode(nodeId, "Package", ctPackage.getSimpleName(), qualifiedName);

        // Add parent package relationship
        CtPackage parentPackage = ctPackage.getDeclaringPackage();
        if (parentPackage != null && !parentPackage.isUnnamedPackage()) {
            String parentId = "package_" + parentPackage.getQualifiedName();
            result.addRelationship(new GraphRelationship(
                    "rel_parent_" + nodeId + "_" + parentId,
                    "SUBPACKAGE_OF",
                    nodeId,
                    parentId,
                    "Package",
                    "Package"
            ));
        }

        result.addNode(packageNode);
    }
}
