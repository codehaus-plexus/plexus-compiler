package org.codehaus.plexus.compiler;

import org.codehaus.plexus.PlexusTestCase;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.io.FileInputStream;
import java.io.File;

public abstract class AbstractCompilerTest
    extends PlexusTestCase
{
    private Compiler compiler = null;

    private String mavenRepoLocal;

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

        Properties buildProperties = new Properties();

        // ----------------------------------------------------------------------
        // We'll need a default pattern for searching for maven.repo.local but
        // this will do for now. jvz.
        // ----------------------------------------------------------------------

        try
        {
            buildProperties.load( new FileInputStream( new File( System.getProperty( "user.home" ), "build.properties" ) ) );

            mavenRepoLocal = buildProperties.getProperty( "maven.repo.local" );
        }
        catch ( Exception e )
        {
            // do nothing
        }

        if ( mavenRepoLocal == null )
        {
            mavenRepoLocal = System.getProperty( "maven.repo.local" );
        }

        if ( mavenRepoLocal == null )
        {
            mavenRepoLocal = new File( System.getProperty( "user.home" ), ".maven/repository" ).getPath();
        }

        compilerConfig = new CompilerConfiguration();

        compilerConfig.setClasspathEntries( getClasspath() );

        compilerConfig.addSourceLocation( basedir + "/src/test-input/src/main" );

        compilerConfig.setOutputLocation( basedir + "/target/" + getRoleHint() + "/classes" );

        compilerConfig.addInclude( "**/*.java" );
    }

    protected String getMavenRepoLocal()
    {
        return mavenRepoLocal;
    }

    protected List getClasspath()
    {
        return Collections.singletonList( mavenRepoLocal + "/commons-lang/jars/commons-lang-2.0.jar" );
    }

    public void testCompilingSources() throws Exception
    {
        List messages = compiler.compile( compilerConfig );

        assertEquals( "Expected number of compilation errors is 1", expectedErrors(), messages.size() );
    }

    protected int expectedErrors()
    {
        return 1;
    }
}