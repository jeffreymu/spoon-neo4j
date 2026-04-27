package com.example.analyzer.visitor;

import com.example.analyzer.model.AnalysisResult;
import com.example.analyzer.model.GraphRelationship;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.CtAbstractVisitor;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Visitor to extract type dependencies (imports, usages) from Java code.
 */
public class DependencyVisitor extends CtAbstractVisitor {

    private AnalysisResult result;
    private Set<String> processedDependencies;

    public DependencyVisitor(AnalysisResult result) {
        this.result = result;
        this.processedDependencies = new HashSet<>();
    }

    @Override
    public <T> void visitCtClass(spoon.reflect.declaration.CtClass<T> ctClass) {
        processTypeDependencies(ctClass);
    }

    @Override
    public <T> void visitCtInterface(spoon.reflect.declaration.CtInterface<T> ctInterface) {
        processTypeDependencies(ctInterface);
    }

    @Override
    public <T extends Enum<?>> void visitCtEnum(spoon.reflect.declaration.CtEnum<T> ctEnum) {
        processTypeDependencies(ctEnum);
    }

    @Override
    public <T extends java.lang.annotation.Annotation> void visitCtAnnotationType(
            spoon.reflect.declaration.CtAnnotationType<T> annotationType) {
        processTypeDependencies(annotationType);
    }

    private void processTypeDependencies(CtType<?> type) {
        if (type.isImplicit()) {
            return;
        }

        String sourceId = getTypeId(type);
        String sourceType = getTypeNodeType(type);

        // Get all referenced types
        Collection<CtTypeReference<?>> referencedTypes = type.getReferencedTypes();
        for (CtTypeReference<?> typeRef : referencedTypes) {
            if (shouldSkipType(typeRef)) {
                continue;
            }

            String targetId = getTypeId(typeRef);
            String targetType = getTypeNodeType(typeRef);

            if (sourceId.equals(targetId)) {
                continue; // Skip self-references
            }

            String depId = "rel_depends_" + sourceId + "_" + targetId;
            if (processedDependencies.contains(depId)) {
                continue;
            }
            processedDependencies.add(depId);

            result.addRelationship(new GraphRelationship(
                    depId,
                    "DEPENDS_ON",
                    sourceId,
                    targetId,
                    sourceType,
                    targetType
            ));
        }
    }

    private boolean shouldSkipType(CtTypeReference<?> typeRef) {
        if (typeRef == null || typeRef.getQualifiedName() == null) {
            return true;
        }

        String qualifiedName = typeRef.getQualifiedName();

        // Skip primitive types and common Java types
        if (qualifiedName.startsWith("java.lang.") ||
            qualifiedName.equals("void") ||
            typeRef.isPrimitive()) {
            return true;
        }

        return false;
    }

    private String getTypeId(CtType<?> type) {
        if (type.isInterface()) {
            return "interface_" + type.getQualifiedName();
        } else if (type.isEnum()) {
            return "enum_" + type.getQualifiedName();
        } else if (type.isAnnotationType()) {
            return "annotation_" + type.getQualifiedName();
        }
        return "class_" + type.getQualifiedName();
    }

    private String getTypeId(CtTypeReference<?> typeRef) {
        if (typeRef.isInterface()) {
            return "interface_" + typeRef.getQualifiedName();
        } else if (typeRef.isEnum()) {
            return "enum_" + typeRef.getQualifiedName();
        } else if (typeRef.isAnnotationType()) {
            return "annotation_" + typeRef.getQualifiedName();
        }
        return "class_" + typeRef.getQualifiedName();
    }

    private String getTypeNodeType(CtType<?> type) {
        if (type.isInterface()) {
            return "Interface";
        } else if (type.isEnum()) {
            return "Enum";
        } else if (type.isAnnotationType()) {
            return "Annotation";
        }
        return "Class";
    }

    private String getTypeNodeType(CtTypeReference<?> typeRef) {
        if (typeRef.isInterface()) {
            return "Interface";
        } else if (typeRef.isEnum()) {
            return "Enum";
        } else if (typeRef.isAnnotationType()) {
            return "Annotation";
        }
        return "Class";
    }
}
