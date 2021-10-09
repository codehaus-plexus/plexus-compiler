package org.codehaus.plexus.compiler.eclipse;

import org.codehaus.plexus.compiler.AbstractCompilerTest;
import org.codehaus.plexus.compiler.CompilerConfiguration;
import org.junit.jupiter.api.BeforeEach;

import java.util.Arrays;
import java.util.Collection;

public class EclipseCompilerErrorsAsWarningsTest extends AbstractCompilerTest
{

    protected void configureCompilerConfig( CompilerConfiguration compilerConfig )
    {
        compilerConfig.addCompilerCustomArgument( "-errorsAsWarnings", "true" );
    }

    @BeforeEach
    public void setUp() throws Exception
    {
        setCompilerDebug( true );
        setCompilerDeprecationWarnings( true );
    }

    @Override
    protected String getRoleHint()
    {
        return "eclipse";
    }

    @Override
    protected int expectedErrors()
    {
        return 0;
    }

    @Override
    protected int expectedWarnings()
    {
        return 6;
    }

    @Override
    protected Collection<String> expectedOutputFiles()
    {
        return Arrays.asList( new String[] {
            "org/codehaus/foo/Deprecation.class",
            "org/codehaus/foo/ExternalDeps.class",
            "org/codehaus/foo/Person.class",
            "org/codehaus/foo/ReservedWord.class",
            //"org/codehaus/foo/Bad.class",             // This one has no class file generated as it's one big issue
            //"org/codehaus/foo/UnknownSymbol.class",
            //"org/codehaus/foo/RightClassname.class"
        });
    }
}
