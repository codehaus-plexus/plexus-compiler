package org.codehaus.plexus.compiler;

/**
 * The MIT License
 *
 * Copyright (c) 2004, The Codehaus
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.test.ArtifactTestCase;
import org.apache.maven.artifact.versioning.VersionRange;

import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

/**
 *
 */
public abstract class AbstractCompilerTest
    extends ArtifactTestCase
{
    private boolean compilerDebug = false;

    private boolean compilerDeprecationWarnings = false;

    private boolean forceJavacCompilerUse = false;

    protected abstract String getRoleHint();

    protected void setCompilerDebug( boolean flag )
    {
        compilerDebug = flag;
    }

    protected void setCompilerDeprecationWarnings( boolean flag )
    {
        compilerDeprecationWarnings = flag;
    }

    public void setForceJavacCompilerUse( boolean forceJavacCompilerUse )
    {
        this.forceJavacCompilerUse = forceJavacCompilerUse;
    }

    protected List<String> getClasspath()
        throws Exception
    {
        List<String> cp = new ArrayList<>();

        File file = getLocalArtifactPath( "commons-lang", "commons-lang", "2.0", "jar" );

        assertTrue( "test prerequisite: commons-lang library must be available in local repository, expected "
                        + file.getAbsolutePath(), file.canRead() );

        cp.add( file.getAbsolutePath() );

        return cp;
    }

    protected void configureCompilerConfig( CompilerConfiguration compilerConfig )
    {

    }

    public void testCompilingSources()
        throws Exception
    {
        List<CompilerMessage> messages = new ArrayList<>();
        Collection<String> files = new TreeSet<>();

        for ( CompilerConfiguration compilerConfig : getCompilerConfigurations() )
        {
            File outputDir = new File( compilerConfig.getOutputLocation() );

            Compiler compiler = (Compiler) lookup( Compiler.ROLE, getRoleHint() );

            messages.addAll( compiler.performCompile( compilerConfig ).getCompilerMessages() );

            if ( outputDir.isDirectory() )
            {
                files.addAll( normalizePaths( FileUtils.getFileNames( outputDir, null, null, false ) ) );
            }
        }

        int numCompilerErrors = compilerErrorCount( messages );

        int numCompilerWarnings = messages.size() - numCompilerErrors;

        int expectedErrors = expectedErrors();

        if ( expectedErrors != numCompilerErrors )
        {
            System.out.println( numCompilerErrors + " error(s) found:" );
            List<String> errors = new ArrayList<>();
            for ( CompilerMessage error : messages )
            {
                if ( !error.isError() )
                {
                    continue;
                }

                System.out.println( "----" );
                System.out.println( error.getFile() );
                System.out.println( error.getMessage() );
                System.out.println( "----" );
                errors.add( error.getMessage() );
            }

            assertEquals( "Wrong number of compilation errors (" + numCompilerErrors + "/" + expectedErrors //
                              + ") : " + displayLines( errors ), expectedErrors, numCompilerErrors );
        }

        int expectedWarnings = expectedWarnings();
        if ( expectedWarnings != numCompilerWarnings )
        {
            List<String> warnings = new ArrayList<>();
            System.out.println( numCompilerWarnings + " warning(s) found:" );
            for ( CompilerMessage error : messages )
            {
                if ( error.isError() )
                {
                    continue;
                }

                System.out.println( "----" );
                System.out.println( error.getFile() );
                System.out.println( error.getMessage() );
                System.out.println( "----" );
                warnings.add( error.getMessage() );
            }

            assertEquals( "Wrong number (" + numCompilerWarnings + "/"
                              + expectedWarnings + ") of compilation warnings: " + displayLines( warnings ), //
                          expectedWarnings, numCompilerWarnings);
        }

        assertEquals( new TreeSet<>( normalizePaths( expectedOutputFiles() ) ), files );
    }

    protected String displayLines( List<String> warnings)
    {
        // with java8 could be as simple as String.join(System.lineSeparator(), warnings)
        StringBuilder sb = new StringBuilder( System.lineSeparator() );
        for ( String warning : warnings )
        {
            sb.append( '-' ).append( warning ).append( System.lineSeparator() );
        }
        return sb.toString();
    }

    private List<CompilerConfiguration> getCompilerConfigurations()
        throws Exception
    {
        String sourceDir = getBasedir() + "/src/test-input/src/main";

        List<String> filenames =
            FileUtils.getFileNames( new File( sourceDir ), "**/*.java", null, false, true );
        Collections.sort( filenames );

        List<CompilerConfiguration> compilerConfigurations = new ArrayList<>();

        int index = 0;
        for ( Iterator<String> it = filenames.iterator(); it.hasNext(); index++ )
        {
            String filename = it.next();

            CompilerConfiguration compilerConfig = new CompilerConfiguration();

            compilerConfig.setDebug( compilerDebug );

            compilerConfig.setShowDeprecation( compilerDeprecationWarnings );

            compilerConfig.setClasspathEntries( getClasspath() );

            compilerConfig.addSourceLocation( sourceDir );

            compilerConfig.setOutputLocation( getBasedir() + "/target/" + getRoleHint() + "/classes-" + index );

            FileUtils.deleteDirectory( compilerConfig.getOutputLocation() );

            compilerConfig.addInclude( filename );

            compilerConfig.setForceJavacCompilerUse( this.forceJavacCompilerUse );

            configureCompilerConfig( compilerConfig );

            String target = getTargetVersion();
            if( StringUtils.isNotEmpty( target) )
            {
                compilerConfig.setTargetVersion( target );
            }

            String source = getSourceVersion();
            if( StringUtils.isNotEmpty( source) )
            {
                compilerConfig.setSourceVersion( source );
            }

            compilerConfigurations.add( compilerConfig );

        }

        return compilerConfigurations;
    }

    public String getTargetVersion()
    {
        return null;
    }

    public String getSourceVersion()
    {
        return null;
    }


    private List<String> normalizePaths( Collection<String> relativePaths )
    {
        List<String> normalizedPaths = new ArrayList<>();
        for ( String relativePath : relativePaths )
        {
            normalizedPaths.add( relativePath.replace( File.separatorChar, '/' ) );
        }
        return normalizedPaths;
    }

    protected int compilerErrorCount( List<CompilerMessage> messages )
    {
        int count = 0;

        for ( CompilerMessage message : messages )
        {
            count += message.isError() ? 1 : 0;
        }

        return count;
    }

    protected int expectedErrors()
    {
        return 1;
    }

    protected int expectedWarnings()
    {
        return 0;
    }

    protected Collection<String> expectedOutputFiles()
    {
        return Collections.emptyList();
    }

    protected File getLocalArtifactPath( String groupId, String artifactId, String version, String type )
    {
        VersionRange versionRange = VersionRange.createFromVersion( version );

        Artifact artifact = new DefaultArtifact( groupId, artifactId, versionRange, Artifact.SCOPE_COMPILE, type, null,
                                                 new DefaultArtifactHandler( type ) );

        return getLocalArtifactPath( artifact );
    }

    protected String getJavaVersion()
    {

        String javaVersion = System.getProperty( "java.version" );
        String realJavaVersion = javaVersion;

        int dotIdx = javaVersion.indexOf( "." );
        if ( dotIdx > -1 )
        {
            int lastDot = dotIdx;

            // find the next dot, so we can trim up to this point.
            dotIdx = javaVersion.indexOf( ".", lastDot + 1 );
            if ( dotIdx > lastDot )
            {
                javaVersion = javaVersion.substring( 0, dotIdx );
            }
        }

        System.out.println( "java.version is: " + realJavaVersion + "\ntrimmed java version is: " + javaVersion
                                + "\ncomparison: \"1.5\".compareTo( \"" + javaVersion + "\" ) == " + ( "1.5".compareTo(
            javaVersion ) ) + "\n" );

        return javaVersion;
    }

}
