package org.codehaus.plexus.compiler.eclipse;

import org.codehaus.plexus.compiler.CompilerConfiguration;
import org.junit.jupiter.api.BeforeEach;

/**
 * Test for errors being reported as warnings.
 *
 * @author <a href="jfaust@tsunamit.com">Jason Faust</a>
 * @since 2.14.0
 */
public class EclipseCompilerErrorsAsWarningsTest extends AbstractEclipseCompilerTest {

    public EclipseCompilerErrorsAsWarningsTest() {
        super(0, 6, 1);
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

    protected void configureCompilerConfig(CompilerConfiguration compilerConfig) {
        super.configureCompilerConfig(compilerConfig);
        compilerConfig.addCompilerCustomArgument("-errorsAsWarnings", "true");
    }
}
