package org.codehaus.plexus.compiler;

import org.codehaus.plexus.PlexusTestCase;

import java.util.List;
import java.util.Properties;
import java.io.FileInputStream;
import java.io.File;

public abstract class AbstractCompilerTest
    extends PlexusTestCase
{
    private Compiler compiler = null;

    private String[] classpathElements;

    private String[] sourceDirectories;

    private String destinationDirectory;

    private String mavenRepoLocal;

    protected String roleHint;

    public AbstractCompilerTest( String s )
    {
        super( s );
    }

    protected abstract String getRoleHint();

    public void setUp()
        throws Exception
    {
        super.setUp();

        basedir = System.getProperty( "basedir" );

        compiler = (Compiler) lookup( Compiler.ROLE, getRoleHint() );

        Properties buildProperties = new Properties();

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
            mavenRepoLocal = new File( System.getProperty( "user.home" ), ".maven/repository" ).getPath();
        }

        classpathElements = new String[]{mavenRepoLocal + "/commons-lang/jars/commons-lang-2.0.jar"};

        sourceDirectories = new String[]{basedir + "/src/test-input/src/main"};

        destinationDirectory = basedir + "/target/" + getRoleHint() + "/classes";
    }

    public void testCompilingSources()
        throws Exception
    {
        List messages = compiler.compile( classpathElements, sourceDirectories, destinationDirectory );

        assertEquals( "Expected number of compilation errors is 1", expectedErrors(), messages.size() );
    }
    
    protected int expectedErrors()
    {
        return 1;
    }
}
