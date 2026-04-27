package com.example;

import com.example.analyzer.ProjectAnalyzer;
import com.example.analyzer.model.AnalysisResult;
import com.example.analyzer.model.GraphNode;
import com.example.analyzer.model.GraphRelationship;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Java Project Analyzer.
 */
class AppTest {

    @TempDir
    Path tempDir;

    private File testProjectDir;

    @BeforeEach
    void setUp() throws Exception {
        // Create a simple test project structure
        testProjectDir = tempDir.toFile();
        
        File srcDir = new File(testProjectDir, "src/main/java/com/test");
        srcDir.mkdirs();
        
        // Create a simple test class
        File testClass = new File(srcDir, "TestClass.java");
        java.io.FileWriter writer = new java.io.FileWriter(testClass);
        writer.write("package com.test;\n\n");
        writer.write("public class TestClass {\n");
        writer.write("    private String name;\n");
        writer.write("    \n");
        writer.write("    public TestClass(String name) {\n");
        writer.write("        this.name = name;\n");
        writer.write("    }\n");
        writer.write("    \n");
        writer.write("    public String getName() {\n");
        writer.write("        return name;\n");
        writer.write("    }\n");
        writer.write("    \n");
        writer.write("    public void printName() {\n");
        writer.write("        System.out.println(getName());\n");
        writer.write("    }\n");
        writer.write("}\n");
        writer.close();
    }

    @Test
    void testAnalyzeProject() {
        ProjectAnalyzer analyzer = new ProjectAnalyzer(testProjectDir.getAbsolutePath());
        AnalysisResult result = analyzer.analyze();

        assertNotNull(result);
        assertEquals(testProjectDir.getName(), result.getProjectName());
        assertTrue(result.getNodeCount() > 0);
    }

    @Test
    void testPackageDetection() {
        ProjectAnalyzer analyzer = new ProjectAnalyzer(testProjectDir.getAbsolutePath());
        AnalysisResult result = analyzer.analyze();

        boolean hasPackage = result.getNodes().stream()
                .anyMatch(n -> n.getType().equals("Package"));
        assertTrue(hasPackage, "Should detect at least one package");
    }

    @Test
    void testClassDetection() {
        ProjectAnalyzer analyzer = new ProjectAnalyzer(testProjectDir.getAbsolutePath());
        AnalysisResult result = analyzer.analyze();

        boolean hasClass = result.getNodes().stream()
                .anyMatch(n -> n.getType().equals("Class"));
        assertTrue(hasClass, "Should detect at least one class");
    }

    @Test
    void testMethodDetection() {
        ProjectAnalyzer analyzer = new ProjectAnalyzer(testProjectDir.getAbsolutePath());
        AnalysisResult result = analyzer.analyze();

        boolean hasMethod = result.getNodes().stream()
                .anyMatch(n -> n.getType().equals("Method"));
        assertTrue(hasMethod, "Should detect at least one method");
    }

    @Test
    void testFieldDetection() {
        ProjectAnalyzer analyzer = new ProjectAnalyzer(testProjectDir.getAbsolutePath());
        AnalysisResult result = analyzer.analyze();

        boolean hasField = result.getNodes().stream()
                .anyMatch(n -> n.getType().equals("Field"));
        assertTrue(hasField, "Should detect at least one field");
    }

    @Test
    void testRelationships() {
        ProjectAnalyzer analyzer = new ProjectAnalyzer(testProjectDir.getAbsolutePath());
        AnalysisResult result = analyzer.analyze();

        assertTrue(result.getRelationshipCount() > 0, "Should have relationships");
    }

    @Test
    void testBelongsToRelationship() {
        ProjectAnalyzer analyzer = new ProjectAnalyzer(testProjectDir.getAbsolutePath());
        AnalysisResult result = analyzer.analyze();

        boolean hasBelongsTo = result.getRelationships().stream()
                .anyMatch(r -> r.getType().equals("BELONGS_TO"));
        assertTrue(hasBelongsTo, "Should have BELONGS_TO relationships");
    }

    @Test
    void testDeclaresRelationship() {
        ProjectAnalyzer analyzer = new ProjectAnalyzer(testProjectDir.getAbsolutePath());
        AnalysisResult result = analyzer.analyze();

        boolean hasDeclares = result.getRelationships().stream()
                .anyMatch(r -> r.getType().equals("DECLARES"));
        assertTrue(hasDeclares, "Should have DECLARES relationships");
    }

    @Test
    void testCallsRelationship() {
        ProjectAnalyzer analyzer = new ProjectAnalyzer(testProjectDir.getAbsolutePath());
        AnalysisResult result = analyzer.analyze();

        boolean hasCalls = result.getRelationships().stream()
                .anyMatch(r -> r.getType().equals("CALLS"));
        assertTrue(hasCalls, "Should have CALLS relationships (printName calls getName)");
    }
}
