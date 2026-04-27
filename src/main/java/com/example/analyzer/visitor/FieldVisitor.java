package com.example.analyzer.visitor;

import com.example.analyzer.model.AnalysisResult;
import com.example.analyzer.model.GraphNode;
import com.example.analyzer.model.GraphRelationship;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.CtAbstractVisitor;

/**
 * Visitor to extract field information from Java code.
 */
public class FieldVisitor extends CtAbstractVisitor {

    private AnalysisResult result;

    public FieldVisitor(AnalysisResult result) {
        this.result = result;
    }

    @Override
    public <T> void visitCtField(CtField<T> field) {
        if (field.isImplicit()) {
            return;
        }

        String fieldId = generateFieldId(field);
        String fieldName = field.getSimpleName();

        GraphNode fieldNode = new GraphNode(fieldId, "Field", fieldName, fieldName);
        fieldNode.addProperty("type", field.getType() != null ? field.getType().getQualifiedName() : "unknown");
        fieldNode.addProperty("isStatic", field.isStatic());
        fieldNode.addProperty("isFinal", field.isFinal());
        fieldNode.addProperty("isPublic", field.isPublic());
        fieldNode.addProperty("isPrivate", field.isPrivate());
        fieldNode.addProperty("isProtected", field.isProtected());
        fieldNode.addProperty("isTransient", field.isTransient());
        fieldNode.addProperty("isVolatile", field.isVolatile());

        // Add relationship to declaring class
        CtType<?> declaringType = field.getDeclaringType();
        if (declaringType != null) {
            String parentType = declaringType.isInterface() ? "Interface" : "Class";
            String parentId = (declaringType.isInterface() ? "interface_" : "class_") + declaringType.getQualifiedName();
            result.addRelationship(new GraphRelationship(
                    "rel_has_field_" + parentId + "_" + fieldId,
                    "HAS_FIELD",
                    parentId,
                    fieldId,
                    parentType,
                    "Field"
            ));
        }

        // Add field type relationship
        CtTypeReference<?> fieldType = field.getType();
        if (fieldType != null) {
            String fieldTypeId = getTypeId(fieldType);
            result.addRelationship(new GraphRelationship(
                    "rel_field_type_" + fieldId + "_" + fieldTypeId,
                    "HAS_TYPE",
                    fieldId,
                    fieldTypeId,
                    "Field",
                    getTypeNodeType(fieldType)
            ));
        }

        result.addNode(fieldNode);
    }

    private String generateFieldId(CtField<?> field) {
        CtType<?> declaringType = field.getDeclaringType();
        String className = declaringType != null ? declaringType.getQualifiedName() : "unknown";
        return "field_" + className + "_" + field.getSimpleName();
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
