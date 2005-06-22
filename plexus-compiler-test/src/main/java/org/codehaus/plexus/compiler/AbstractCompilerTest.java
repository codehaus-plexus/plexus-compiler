package org.codehaus.plexus.compiler;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.test.ArtifactTestCase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public abstract class AbstractCompilerTest
    extends ArtifactTestCase
{
    private Compiler compiler = null;

    protected String roleHint;

    private CompilerConfiguration compilerConfig;

    public AbstractCompilerTest()
    {
        super();
    }

    protected abstract String getRoleHint();

    public void setUp() throws Exception
    {
        super.setUp();

        String basedir = getBasedir();

        basedir = System.getProperty( "basedir" );

        compiler = (Compiler) lookup( Compiler.ROLE, getRoleHint() );

        compilerConfig = new CompilerConfiguration();

        compilerConfig.setClasspathEntries( getClasspath() );

        compilerConfig.addSourceLocation( basedir + "/src/test-input/src/main" );

        compilerConfig.setOutputLocation( basedir + "/target/" + getRoleHint() + "/classes" );

        compilerConfig.addInclude( "**/*.java" );
    }

    protected List getClasspath()
        throws Exception
    {
        Artifact artifact = new DefaultArtifact( "commons-lang", "commons-lang", "2.0", Artifact.SCOPE_RUNTIME, "jar", null, new DefaultArtifactHandler( "jar" ) );

        List cp = new ArrayList();

        cp.add( getLocalArtifactPath( artifact ) );

        return cp;
    }

    protected CompilerConfiguration getCompilerConfiguration()
    {
        return compilerConfig;
    }
    
    public void testCompilingSources() throws Exception
    {
        List messages = compiler.compile( compilerConfig );

        for ( Iterator iter = messages.iterator(); iter.hasNext(); )
        {
            System.out.println( iter.next() );
        }
        assertEquals( "Expected number of compilation errors is" + expectedErrors(), expectedErrors(), messages.size() );
    }

    protected int expectedErrors()
    {
        return 1;
    }
}
