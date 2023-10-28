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
        return 6;
    }

    @Override
    protected int expectedWarnings() {
        return 1;
    }

    @Override
    protected Collection<String> expectedOutputFiles() {
        return Arrays.asList(
                "org/codehaus/foo/Deprecation.class",
                "org/codehaus/foo/ExternalDeps.class",
                "org/codehaus/foo/Person.class",
                "org/codehaus/foo/ReservedWord.class");
    }
}
