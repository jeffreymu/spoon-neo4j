package com.example.analyzer;

import com.example.analyzer.model.AnalysisResult;
import com.example.analyzer.visitor.*;
import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.*;
import spoon.support.compiler.FileSystemFolder;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Main analyzer class that uses Spoon to analyze Java projects.
 */
public class ProjectAnalyzer {

    private String projectPath;
    private String projectName;
    private boolean includeJavaLibraries;

    public ProjectAnalyzer(String projectPath) {
        this(projectPath, false);
    }

    public ProjectAnalyzer(String projectPath, boolean includeJavaLibraries) {
        this.projectPath = projectPath;
        this.projectName = extractProjectName(projectPath);
        this.includeJavaLibraries = includeJavaLibraries;
    }

    public AnalysisResult analyze() {
        AnalysisResult result = new AnalysisResult();
        result.setProjectName(projectName);
        result.setProjectPath(projectPath);

        Launcher launcher = new Launcher();
        launcher.addInputResource(projectPath);

        // Configure Spoon
        launcher.getEnvironment().setAutoImports(true);
        launcher.getEnvironment().setNoClasspath(true);
        launcher.getEnvironment().setCommentEnabled(true);
        launcher.getEnvironment().setComplianceLevel(8);

        // Build the model
        launcher.buildModel();
        CtModel model = launcher.getModel();

        // Process all elements
        processPackages(model, result);
        processTypes(model, result);
        processMethods(model, result);
        processFields(model, result);
        processCallGraph(model, result);
        processDependencies(model, result);

        return result;
    }

    private void processPackages(CtModel model, AnalysisResult result) {
        PackageVisitor visitor = new PackageVisitor(result);
        for (CtPackage pkg : model.getAllPackages()) {
            pkg.accept(visitor);
        }
    }

    private void processTypes(CtModel model, AnalysisResult result) {
        ClassVisitor visitor = new ClassVisitor(result);
        for (CtType<?> type : model.getAllTypes()) {
            type.accept(visitor);
        }
    }

    private void processMethods(CtModel model, AnalysisResult result) {
        MethodVisitor visitor = new MethodVisitor(result);
        for (CtType<?> type : model.getAllTypes()) {
            for (CtMethod<?> method : type.getMethods()) {
                method.accept(visitor);
            }
            if (type instanceof CtClass) {
                for (CtConstructor<?> constructor : ((CtClass<?>) type).getConstructors()) {
                    constructor.accept(visitor);
                }
            }
        }
    }

    private void processFields(CtModel model, AnalysisResult result) {
        FieldVisitor visitor = new FieldVisitor(result);
        for (CtType<?> type : model.getAllTypes()) {
            if (type instanceof CtClass) {
                for (CtField<?> field : ((CtClass<?>) type).getFields()) {
                    field.accept(visitor);
                }
            }
        }
    }

    private void processCallGraph(CtModel model, AnalysisResult result) {
        CallGraphVisitor visitor = new CallGraphVisitor(result);
        for (CtType<?> type : model.getAllTypes()) {
            visitor.scan(type);
        }
    }

    private void processDependencies(CtModel model, AnalysisResult result) {
        DependencyVisitor visitor = new DependencyVisitor(result);
        for (CtType<?> type : model.getAllTypes()) {
            type.accept(visitor);
        }
    }

    private String extractProjectName(String path) {
        Path p = Paths.get(path);
        return p.getFileName() != null ? p.getFileName().toString() : "unknown-project";
    }
}
