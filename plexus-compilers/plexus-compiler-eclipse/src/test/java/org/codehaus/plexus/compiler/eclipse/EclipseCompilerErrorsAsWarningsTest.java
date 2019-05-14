package org.codehaus.plexus.compiler.eclipse;

import org.codehaus.plexus.compiler.CompilerConfiguration;

/**
 * Test for errors being reported as warnings.
 * 
 * @author <a href="jfaust@tsunamit.com">Jason Faust</a>
 * @since 2.8.6
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
