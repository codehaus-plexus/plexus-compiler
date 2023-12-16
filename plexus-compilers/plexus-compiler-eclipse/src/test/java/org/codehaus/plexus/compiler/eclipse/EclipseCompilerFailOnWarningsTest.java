package org.codehaus.plexus.compiler.eclipse;

import java.util.Arrays;

import org.codehaus.plexus.compiler.CompilerConfiguration;

public class EclipseCompilerFailOnWarningsTest extends AbstractEclipseCompilerTest {

    public EclipseCompilerFailOnWarningsTest() {
        super(
                4,
                2,
                1,
                Arrays.asList(
                        "org/codehaus/foo/Deprecation.class",
                        "org/codehaus/foo/ExternalDeps.class",
                        "org/codehaus/foo/Info.class",
                        "org/codehaus/foo/Person.class",
                        "org/codehaus/foo/ReservedWord.class"));
    }

    @Override
    protected void configureCompilerConfig(CompilerConfiguration compilerConfig) {
        compilerConfig.setFailOnWarning(true);
    }

    @Override
    protected String getRoleHint() {
        return "eclipse";
    }
}
