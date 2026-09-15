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
        // assert is a keyword since Java 1.4, so ReservedWord does not compile on a modern source level
        if (getJavaFeatureVersion() >= 9) {
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
