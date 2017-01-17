package org.codehaus.plexus.compiler.eclipse;

import java.util.Arrays;
import java.util.Collection;

import org.codehaus.plexus.compiler.AbstractCompilerTest;
import org.codehaus.plexus.compiler.CompilerConfiguration;

public class EclipseCompilerErrorsAsWarningsTest extends AbstractCompilerTest
{

    protected void configureCompilerConfig( CompilerConfiguration compilerConfig )
    {
        compilerConfig.addCompilerCustomArgument( "-errorsAsWarnings", "true" );
    }

    public void setUp() throws Exception
    {
        super.setUp();

        setCompilerDebug( true );
        setCompilerDeprecationWarnings( true );
    }

    protected String getRoleHint()
    {
        return "eclipse";
    }

    protected int expectedErrors()
    {
        return 0;
    }

    protected int expectedWarnings()
    {
        return 6;
    }

    protected Collection<String> expectedOutputFiles()
    {
        return Arrays.asList( new String[] { "org/codehaus/foo/Deprecation.class",
            "org/codehaus/foo/ExternalDeps.class", "org/codehaus/foo/Person.class",
            "org/codehaus/foo/ReservedWord.class", "org/codehaus/foo/Bad.class",
            "org/codehaus/foo/UnknownSymbol.class", "org/codehaus/foo/RightClassname.class" } );
    }
}
