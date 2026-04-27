package com.example.analyzer.visitor;

import com.example.analyzer.model.AnalysisResult;
import com.example.analyzer.model.GraphNode;
import com.example.analyzer.model.GraphRelationship;
import spoon.reflect.declaration.*;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.CtAbstractVisitor;

import java.util.List;

/**
 * Visitor to extract class/interface/enum information from Java code.
 */
public class ClassVisitor extends CtAbstractVisitor {

    private AnalysisResult result;

    public ClassVisitor(AnalysisResult result) {
        this.result = result;
    }

    @Override
    public <T> void visitCtClass(CtClass<T> ctClass) {
        if (ctClass.isImplicit()) {
            return;
        }

        String qualifiedName = ctClass.getQualifiedName();
        String nodeId = "class_" + qualifiedName;

        GraphNode classNode = new GraphNode(nodeId, "Class", ctClass.getSimpleName(), qualifiedName);
        classNode.addProperty("modifiers", getModifiers(ctClass));
        classNode.addProperty("isAbstract", ctClass.isAbstract());
        classNode.addProperty("isFinal", ctClass.isFinal());
        classNode.addProperty("isStatic", ctClass.isStatic());

        // Add package relationship
        CtPackage ctPackage = ctClass.getPackage();
        if (ctPackage != null) {
            String packageId = "package_" + ctPackage.getQualifiedName();
            result.addRelationship(new GraphRelationship(
                    "rel_" + nodeId + "_" + packageId,
                    "BELONGS_TO",
                    nodeId,
                    packageId,
                    "Class",
                    "Package"
            ));
        }

        // Add superclass relationship
        CtTypeReference<?> superClass = ctClass.getSuperclass();
        if (superClass != null && !superClass.getQualifiedName().equals("java.lang.Object")) {
            String superNodeId = "class_" + superClass.getQualifiedName();
            result.addRelationship(new GraphRelationship(
                    "rel_extends_" + nodeId + "_" + superNodeId,
                    "EXTENDS",
                    nodeId,
                    superNodeId,
                    "Class",
                    "Class"
            ));
        }

        // Add interface implementations
        for (CtTypeReference<?> interfaceRef : ctClass.getSuperInterfaces()) {
            String interfaceId = "interface_" + interfaceRef.getQualifiedName();
            result.addRelationship(new GraphRelationship(
                    "rel_implements_" + nodeId + "_" + interfaceId,
                    "IMPLEMENTS",
                    nodeId,
                    interfaceId,
                    "Class",
                    "Interface"
            ));
        }

        result.addNode(classNode);
    }

    @Override
    public <T> void visitCtInterface(CtInterface<T> ctInterface) {
        if (ctInterface.isImplicit()) {
            return;
        }

        String qualifiedName = ctInterface.getQualifiedName();
        String nodeId = "interface_" + qualifiedName;

        GraphNode interfaceNode = new GraphNode(nodeId, "Interface", ctInterface.getSimpleName(), qualifiedName);
        interfaceNode.addProperty("modifiers", getModifiers(ctInterface));

        // Add package relationship
        CtPackage ctPackage = ctInterface.getPackage();
        if (ctPackage != null) {
            String packageId = "package_" + ctPackage.getQualifiedName();
            result.addRelationship(new GraphRelationship(
                    "rel_" + nodeId + "_" + packageId,
                    "BELONGS_TO",
                    nodeId,
                    packageId,
                    "Interface",
                    "Package"
            ));
        }

        // Add extended interfaces
        for (CtTypeReference<?> superInterface : ctInterface.getSuperInterfaces()) {
            String superInterfaceId = "interface_" + superInterface.getQualifiedName();
            result.addRelationship(new GraphRelationship(
                    "rel_extends_" + nodeId + "_" + superInterfaceId,
                    "EXTENDS",
                    nodeId,
                    superInterfaceId,
                    "Interface",
                    "Interface"
            ));
        }

        result.addNode(interfaceNode);
    }

    @Override
    public <T extends Enum<?>> void visitCtEnum(CtEnum<T> ctEnum) {
        if (ctEnum.isImplicit()) {
            return;
        }

        String qualifiedName = ctEnum.getQualifiedName();
        String nodeId = "enum_" + qualifiedName;

        GraphNode enumNode = new GraphNode(nodeId, "Enum", ctEnum.getSimpleName(), qualifiedName);
        enumNode.addProperty("modifiers", getModifiers(ctEnum));

        // Add enum constants
        List<CtEnumValue<?>> enumValues = ctEnum.getEnumValues();
        enumNode.addProperty("enumConstants", enumValues.size());

        // Add package relationship
        CtPackage ctPackage = ctEnum.getPackage();
        if (ctPackage != null) {
            String packageId = "package_" + ctPackage.getQualifiedName();
            result.addRelationship(new GraphRelationship(
                    "rel_" + nodeId + "_" + packageId,
                    "BELONGS_TO",
                    nodeId,
                    packageId,
                    "Enum",
                    "Package"
            ));
        }

        result.addNode(enumNode);
    }

    @Override
    public <T extends java.lang.annotation.Annotation> void visitCtAnnotationType(CtAnnotationType<T> annotationType) {
        if (annotationType.isImplicit()) {
            return;
        }

        String qualifiedName = annotationType.getQualifiedName();
        String nodeId = "annotation_" + qualifiedName;

        GraphNode annotationNode = new GraphNode(nodeId, "Annotation", annotationType.getSimpleName(), qualifiedName);
        annotationNode.addProperty("modifiers", getModifiers(annotationType));

        // Add package relationship
        CtPackage ctPackage = annotationType.getPackage();
        if (ctPackage != null) {
            String packageId = "package_" + ctPackage.getQualifiedName();
            result.addRelationship(new GraphRelationship(
                    "rel_" + nodeId + "_" + packageId,
                    "BELONGS_TO",
                    nodeId,
                    packageId,
                    "Annotation",
                    "Package"
            ));
        }

        result.addNode(annotationNode);
    }

    private String getModifiers(CtType<?> type) {
        StringBuilder modifiers = new StringBuilder();
        if (type.isPublic()) modifiers.append("public ");
        if (type.isProtected()) modifiers.append("protected ");
        if (type.isPrivate()) modifiers.append("private ");
        if (type.isAbstract()) modifiers.append("abstract ");
        if (type.isStatic()) modifiers.append("static ");
        if (type.isFinal()) modifiers.append("final ");
        return modifiers.toString().trim();
    }
}
