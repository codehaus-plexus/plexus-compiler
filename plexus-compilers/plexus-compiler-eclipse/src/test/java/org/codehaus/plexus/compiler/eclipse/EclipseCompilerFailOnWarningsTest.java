package org.codehaus.plexus.compiler.eclipse;

import java.util.Arrays;
import java.util.Collection;

import org.codehaus.plexus.compiler.AbstractCompilerTest;
import org.codehaus.plexus.compiler.CompilerConfiguration;

public class EclipseCompilerFailOnWarningsTest extends AbstractCompilerTest {

    protected void configureCompilerConfig(CompilerConfiguration compilerConfig) {
        compilerConfig.setFailOnWarning(true);
    }

    @Override
    protected String getRoleHint() {
        return "eclipse";
    }

    @Override
    protected int expectedErrors() {
        return 5;
    }

    @Override
    protected int expectedWarnings() {
        return 0;
    }

    @Override
    protected Collection<String> expectedOutputFiles() {
        String javaVersion = getJavaVersion();
        if (javaVersion.contains("9.0")
                || javaVersion.contains("11")
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
