package org.codehaus.plexus.compiler.eclipse;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.codehaus.plexus.compiler.CompilerConfiguration;
import org.codehaus.plexus.compiler.CompilerResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EclipseJavaCompilerTest {
    @TempDir
    Path tempDir;

    @ParameterizedTest
    @MethodSource("sources")
    void testReorderedSources(List<String> expected, List<String> inputSources) {
        List<String> resorted = EclipseJavaCompiler.resortSourcesToPutModuleInfoFirst(inputSources);

        assertEquals(expected, resorted);
    }

    @Test
    void procNoneDisablesAnnotationProcessorDiscoveryWithoutExplicitProcessors() throws Exception {
        Path sourceDirectory = tempDir.resolve("src");
        Path sourceFile = sourceDirectory.resolve("demo/Example.java");
        Files.createDirectories(sourceFile.getParent());
        Files.writeString(sourceFile, "package demo;\n\npublic class Example {}\n");

        Path outputDirectory = tempDir.resolve("classes");
        Files.createDirectories(outputDirectory);
        Path serviceFile = outputDirectory.resolve("META-INF/services/javax.annotation.processing.Processor");
        Files.createDirectories(serviceFile.getParent());
        Files.writeString(serviceFile, "demo.MissingProcessor\n");

        CompilerConfiguration config = new CompilerConfiguration();
        config.setSourceVersion("17");
        config.addSourceLocation(sourceDirectory.toString());
        config.setOutputLocation(outputDirectory.toString());
        config.setProc("none");

        CompilerResult result = new EclipseJavaCompiler().performCompile(config);

        assertTrue(result.isSuccess(), () -> "Compilation failed: " + result.getCompilerMessages());
        assertTrue(Files.exists(outputDirectory.resolve("demo/Example.class")), "Sources should still be compiled");
    }

    @Test
    void procOnlyIsPassedToEcjWithoutExplicitProcessors() throws Exception {
        Path sourceDirectory = tempDir.resolve("src");
        Path sourceFile = sourceDirectory.resolve("demo/Only.java");
        Files.createDirectories(sourceFile.getParent());
        Files.writeString(sourceFile, "package demo;\n\npublic class Only {}\n");

        Path outputDirectory = tempDir.resolve("only-classes");
        Files.createDirectories(outputDirectory);

        CompilerConfiguration config = new CompilerConfiguration();
        config.setSourceVersion("17");
        config.addSourceLocation(sourceDirectory.toString());
        config.setOutputLocation(outputDirectory.toString());
        config.setProc("only");

        CompilerResult result = new EclipseJavaCompiler().performCompile(config);

        assertTrue(result.isSuccess(), () -> "Compilation failed: " + result.getCompilerMessages());
        assertFalse(
                Files.exists(outputDirectory.resolve("demo/Only.class")),
                "-proc:only should run annotation processing without compiling sources");
    }

    static Stream<Arguments> sources() {
        List<String> expectedOrder = asList("module-info.java", "plexus/A.java", "plexus/B.java", "eclipse/A.java");

        List<String> moduleInfoAlreadyFirst =
                asList("module-info.java", "plexus/A.java", "plexus/B.java", "eclipse/A.java");

        List<String> moduleInfoSomewhereInTheMiddle =
                asList("plexus/A.java", "module-info.java", "plexus/B.java", "eclipse/A.java");

        List<String> moduleInfoAsLast = asList("plexus/A.java", "plexus/B.java", "eclipse/A.java", "module-info.java");

        return Stream.of(
                Arguments.arguments(expectedOrder, moduleInfoAlreadyFirst),
                Arguments.arguments(expectedOrder, moduleInfoSomewhereInTheMiddle),
                Arguments.arguments(expectedOrder, moduleInfoAsLast));
    }
}
