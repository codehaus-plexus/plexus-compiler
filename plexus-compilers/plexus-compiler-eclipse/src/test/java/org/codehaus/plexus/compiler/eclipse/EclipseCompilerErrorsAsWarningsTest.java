package org.codehaus.plexus.compiler.eclipse;

import org.codehaus.plexus.compiler.CompilerConfiguration;

/**
 * @author <a href="jason.faust@gmail.com">Jason Faust</a>
 */
public class EclipseCompilerErrorsAsWarningsTest extends AbstractEclipseCompilerTest {

    public EclipseCompilerErrorsAsWarningsTest() {
        super(0, 6, 1);
    }

    @Override
    protected void configureCompilerConfig(CompilerConfiguration compilerConfig) {
        super.configureCompilerConfig(compilerConfig);
        compilerConfig.addCompilerCustomArgument("-errorsAsWarnings", "true");
    }

}
