package com.example.analyzer.visitor;

import com.example.analyzer.model.AnalysisResult;
import com.example.analyzer.model.GraphRelationship;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.CtScanner;

import java.util.HashSet;
import java.util.Set;

/**
 * Visitor to extract method call relationships from Java code.
 */
public class CallGraphVisitor extends CtScanner {

    private AnalysisResult result;
    private Set<String> processedCalls;
    private CtMethod<?> currentMethod;

    public CallGraphVisitor(AnalysisResult result) {
        this.result = result;
        this.processedCalls = new HashSet<>();
    }

    @Override
    public <T> void visitCtMethod(CtMethod<T> method) {
        if (method.isImplicit()) {
            return;
        }
        
        CtMethod<?> previousMethod = currentMethod;
        currentMethod = method;
        
        // Scan the method body
        super.visitCtMethod(method);
        
        currentMethod = previousMethod;
    }

    @Override
    public <T> void visitCtInvocation(CtInvocation<T> invocation) {
        if (currentMethod == null) {
            super.visitCtInvocation(invocation);
            return;
        }

        CtExecutableReference<?> executableRef = invocation.getExecutable();
        if (executableRef == null) {
            super.visitCtInvocation(invocation);
            return;
        }

        String callerId = generateMethodId(currentMethod);
        String calleeId = generateCalleeId(executableRef);

        if (!callerId.equals(calleeId)) {
            String callId = "rel_calls_" + callerId + "_" + calleeId;
            if (!processedCalls.contains(callId)) {
                processedCalls.add(callId);

                result.addRelationship(new GraphRelationship(
                        callId,
                        "CALLS",
                        callerId,
                        calleeId,
                        "Method",
                        "Method"
                ));
            }
        }

        // Continue scanning nested invocations
        super.visitCtInvocation(invocation);
    }

    private String generateMethodId(CtMethod<?> method) {
        CtType<?> declaringType = method.getDeclaringType();
        String className = declaringType != null ? declaringType.getQualifiedName() : "unknown";
        return "method_" + className + "_" + method.getSimpleName() + "_" + method.getParameters().size();
    }

    private String generateCalleeId(CtExecutableReference<?> execRef) {
        CtTypeReference<?> declaringType = execRef.getDeclaringType();
        String className = declaringType != null ? declaringType.getQualifiedName() : "unknown";
        int paramCount = execRef.getParameters() != null ? execRef.getParameters().size() : 0;
        return "method_" + className + "_" + execRef.getSimpleName() + "_" + paramCount;
    }
}
