package org.codehaus.plexus.compiler.eclipse;

import java.util.Arrays;
import java.util.Collection;

import org.codehaus.plexus.compiler.AbstractCompilerTest;
import org.codehaus.plexus.compiler.CompilerConfiguration;
import org.junit.jupiter.api.BeforeEach;

public class EclipseCompilerErrorsAsWarningsTest extends AbstractCompilerTest {

    protected void configureCompilerConfig(CompilerConfiguration compilerConfig) {
        compilerConfig.addCompilerCustomArgument("-errorsAsWarnings", "true");
    }

    @BeforeEach
    public void setUp() throws Exception {
        setCompilerDebug(true);
        setCompilerDeprecationWarnings(true);
    }

    @Override
    protected String getRoleHint() {
        return "eclipse";
    }

    @Override
    protected int expectedErrors() {
        return 0;
    }

    @Override
    protected int expectedWarnings() {
        return 6;
    }

    @Override
    protected Collection<String> expectedOutputFiles() {
        String javaVersion = getJavaVersion();
        if (javaVersion.contains("11")
                || javaVersion.contains("17")
                || javaVersion.contains("21")
                || javaVersion.contains("25")) {
            return Arrays.asList(
                    "org/codehaus/foo/Deprecation.class",
                    "org/codehaus/foo/ExternalDeps.class",
                    "org/codehaus/foo/Person.class");
        }
        return Arrays.asList(
                "org/codehaus/foo/Deprecation.class",
                "org/codehaus/foo/ExternalDeps.class",
                "org/codehaus/foo/Person.class",
                "org/codehaus/foo/ReservedWord.class");
    }
}
