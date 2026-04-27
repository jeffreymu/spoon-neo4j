package com.example.analyzer.visitor;

import com.example.analyzer.model.AnalysisResult;
import com.example.analyzer.model.GraphNode;
import com.example.analyzer.model.GraphRelationship;
import spoon.reflect.declaration.*;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.CtAbstractVisitor;

import java.util.Set;

/**
 * Visitor to extract method information from Java code.
 */
public class MethodVisitor extends CtAbstractVisitor {

    private AnalysisResult result;

    public MethodVisitor(AnalysisResult result) {
        this.result = result;
    }

    @Override
    public <T> void visitCtMethod(CtMethod<T> method) {
        if (method.isImplicit()) {
            return;
        }

        String methodId = generateMethodId(method);
        String methodName = method.getSimpleName();

        GraphNode methodNode = new GraphNode(methodId, "Method", methodName, method.getSignature());
        methodNode.addProperty("returnType", method.getType() != null ? method.getType().getQualifiedName() : "void");
        methodNode.addProperty("isStatic", method.isStatic());
        methodNode.addProperty("isPublic", method.isPublic());
        methodNode.addProperty("isPrivate", method.isPrivate());
        methodNode.addProperty("isProtected", method.isProtected());
        methodNode.addProperty("isAbstract", method.isAbstract());
        methodNode.addProperty("isFinal", method.isFinal());
        methodNode.addProperty("isSynchronized", method.isSynchronized());
        methodNode.addProperty("lineNumber", method.getPosition() != null ? method.getPosition().getLine() : -1);

        // Add parameters count
        methodNode.addProperty("parameterCount", method.getParameters().size());

        // Add relationship to declaring class
        CtType<?> declaringType = method.getDeclaringType();
        if (declaringType != null) {
            String parentType = declaringType.isInterface() ? "Interface" : "Class";
            String parentId = (declaringType.isInterface() ? "interface_" : "class_") + declaringType.getQualifiedName();
            result.addRelationship(new GraphRelationship(
                    "rel_declares_" + parentId + "_" + methodId,
                    "DECLARES",
                    parentId,
                    methodId,
                    parentType,
                    "Method"
            ));
        }

        // Add parameter types
        for (CtParameter<?> param : method.getParameters()) {
            CtTypeReference<?> paramType = param.getType();
            if (paramType != null) {
                String paramTypeId = getTypeId(paramType);
                result.addRelationship(new GraphRelationship(
                        "rel_param_" + methodId + "_" + paramTypeId,
                        "HAS_PARAMETER",
                        methodId,
                        paramTypeId,
                        "Method",
                        getTypeNodeType(paramType)
                ));
            }
        }

        // Add return type relationship
        CtTypeReference<?> returnType = method.getType();
        if (returnType != null && !returnType.getQualifiedName().equals("void")) {
            String returnTypeId = getTypeId(returnType);
            result.addRelationship(new GraphRelationship(
                    "rel_returns_" + methodId + "_" + returnTypeId,
                    "RETURNS",
                    methodId,
                    returnTypeId,
                    "Method",
                    getTypeNodeType(returnType)
            ));
        }

        // Add thrown exceptions
        Set<CtTypeReference<? extends Throwable>> thrownTypes = method.getThrownTypes();
        for (CtTypeReference<? extends Throwable> thrownType : thrownTypes) {
            String exceptionId = "class_" + thrownType.getQualifiedName();
            result.addRelationship(new GraphRelationship(
                    "rel_throws_" + methodId + "_" + exceptionId,
                    "THROWS",
                    methodId,
                    exceptionId,
                    "Method",
                    "Class"
            ));
        }

        result.addNode(methodNode);
    }

    @Override
    public <T> void visitCtConstructor(CtConstructor<T> constructor) {
        if (constructor.isImplicit()) {
            return;
        }

        String constructorId = generateConstructorId(constructor);

        GraphNode constructorNode = new GraphNode(constructorId, "Constructor", "<init>", constructor.getSignature());
        constructorNode.addProperty("isPublic", constructor.isPublic());
        constructorNode.addProperty("isPrivate", constructor.isPrivate());
        constructorNode.addProperty("isProtected", constructor.isProtected());
        constructorNode.addProperty("parameterCount", constructor.getParameters().size());
        constructorNode.addProperty("lineNumber", constructor.getPosition() != null ? constructor.getPosition().getLine() : -1);

        // Add relationship to declaring class
        CtType<?> declaringType = constructor.getDeclaringType();
        if (declaringType != null) {
            String parentId = "class_" + declaringType.getQualifiedName();
            result.addRelationship(new GraphRelationship(
                    "rel_has_constructor_" + parentId + "_" + constructorId,
                    "HAS_CONSTRUCTOR",
                    parentId,
                    constructorId,
                    "Class",
                    "Constructor"
            ));
        }

        // Add parameter types
        for (CtParameter<?> param : constructor.getParameters()) {
            CtTypeReference<?> paramType = param.getType();
            if (paramType != null) {
                String paramTypeId = getTypeId(paramType);
                result.addRelationship(new GraphRelationship(
                        "rel_param_" + constructorId + "_" + paramTypeId,
                        "HAS_PARAMETER",
                        constructorId,
                        paramTypeId,
                        "Constructor",
                        getTypeNodeType(paramType)
                ));
            }
        }

        result.addNode(constructorNode);
    }

    private String generateMethodId(CtMethod<?> method) {
        CtType<?> declaringType = method.getDeclaringType();
        String className = declaringType != null ? declaringType.getQualifiedName() : "unknown";
        return "method_" + className + "_" + method.getSimpleName() + "_" + method.getParameters().size();
    }

    private <T> String generateConstructorId(CtConstructor<T> constructor) {
        CtType<?> declaringType = constructor.getDeclaringType();
        String className = declaringType != null ? declaringType.getQualifiedName() : "unknown";
        return "constructor_" + className + "_" + constructor.getParameters().size();
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
