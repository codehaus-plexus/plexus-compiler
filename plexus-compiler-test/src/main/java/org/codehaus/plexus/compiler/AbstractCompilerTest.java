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
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.test.ArtifactTestCase;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.io.xpp3.SettingsXpp3Reader;

import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.ReaderFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

/**
 * @version $Id$
 */
public abstract class AbstractCompilerTest
    extends ArtifactTestCase
{
    private boolean compilerDebug = false;

    private boolean compilerDeprecationWarnings = false;

    protected abstract String getRoleHint();

    protected void setCompilerDebug( boolean flag )
    {
        compilerDebug = flag;
    }

    protected void setCompilerDeprecationWarnings( boolean flag )
    {
        compilerDeprecationWarnings = flag;
    }


    protected List getClasspath()
        throws Exception
    {
        List cp = new ArrayList();

        File file = getLocalArtifactPath( "commons-lang", "commons-lang", "2.0", "jar" );

        assertTrue( "test prerequisite: commons-lang library must be available in local repository, expected "
                    + file.getAbsolutePath(), file.canRead() );

        cp.add( file.getAbsolutePath() );

        return cp;
    }

    public void testCompilingSources()
        throws Exception
    {
        List messages = new ArrayList();
        Collection files = new TreeSet();

        for ( Iterator it = getCompilerConfigurations().iterator(); it.hasNext(); )
        {
            CompilerConfiguration compilerConfig = (CompilerConfiguration) it.next();
            File outputDir = new File( compilerConfig.getOutputLocation() );

            Compiler compiler = (Compiler) lookup( Compiler.ROLE, getRoleHint() );

            messages.addAll( compiler.compile( compilerConfig ) );

            if ( outputDir.isDirectory() )
            {
                files.addAll( normalizePaths( FileUtils.getFileNames( outputDir, null, null, false ) ) );
            }
        }

        int numCompilerErrors = compilerErrorCount( messages );

        int numCompilerWarnings = messages.size() - numCompilerErrors;

        if ( expectedErrors() != numCompilerErrors )
        {
            System.err.println( numCompilerErrors + " error(s) found:" );
            for ( Iterator it = messages.iterator(); it.hasNext(); )
            {
                CompilerError error = (CompilerError) it.next();

                if ( !error.isError() )
                {
                    continue;
                }

                System.err.println( "----" );
                System.err.println( error.getFile() );
                System.err.println( error.getMessage() );
                System.err.println( "----" );
            }

            assertEquals( "Wrong number of compilation errors.",
                          expectedErrors(),
                          numCompilerErrors );
        }

        if ( expectedWarnings() != numCompilerWarnings )
        {
            System.err.println( numCompilerWarnings + " warning(s) found:" );
            for ( Iterator it = messages.iterator(); it.hasNext(); )
            {
                CompilerError error = (CompilerError) it.next();

                if ( error.isError() )
                {
                    continue;
                }

                System.err.println( "----" );
                System.err.println( error.getFile() );
                System.err.println( error.getMessage() );
                System.err.println( "----" );
            }

            assertEquals( "Wrong number of compilation warnings.",
                          expectedWarnings(),
                          numCompilerWarnings );
        }

        assertEquals( new TreeSet( normalizePaths( expectedOutputFiles() ) ), files );
    }

    private List getCompilerConfigurations()
        throws Exception
    {
        String sourceDir = getBasedir() + "/src/test-input/src/main";

        List filenames = FileUtils.getFileNames( new File( sourceDir ), "**/*.java", null, false, true );
        Collections.sort( filenames );

        List compilerConfigurations = new ArrayList();

        int index = 0;
        for ( Iterator it = filenames.iterator(); it.hasNext(); index++ )
        {
            String filename = (String) it.next();

            CompilerConfiguration compilerConfig = new CompilerConfiguration();

            compilerConfig.setDebug( compilerDebug );

            compilerConfig.setShowDeprecation( compilerDeprecationWarnings );

            compilerConfig.setClasspathEntries( getClasspath() );

            compilerConfig.addSourceLocation( sourceDir );

            compilerConfig.setOutputLocation( getBasedir() + "/target/" + getRoleHint() + "/classes-" + index );

            compilerConfig.addInclude( filename );

            compilerConfigurations.add( compilerConfig );
        }

        return compilerConfigurations;
    }

    private List normalizePaths( Collection relativePaths )
    {
        List normalizedPaths = new ArrayList();
        for ( Iterator it = relativePaths.iterator(); it.hasNext(); )
        {
            normalizedPaths.add( it.next().toString().replace( File.separatorChar, '/' ) );
        }
        return normalizedPaths;
    }

    protected int compilerErrorCount( List messages )
    {
        int count = 0;

        for ( int i = 0; i < messages.size(); i++ )
        {
            count += ( (CompilerError) messages.get( i ) ).isError() ? 1 : 0;
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

    protected Collection expectedOutputFiles()
    {
        return Collections.EMPTY_LIST;
    }

    protected File getLocalArtifactPath( String groupId, String artifactId, String version, String type )
    {
        VersionRange versionRange = VersionRange.createFromVersion( version );

        Artifact artifact =
            new DefaultArtifact( groupId, artifactId, versionRange, Artifact.SCOPE_COMPILE, type, null,
                                 new DefaultArtifactHandler( type ) );

        return getLocalArtifactPath( artifact );
    }

// TODO: Temporarily duplicated code from maven-artifact-test:2.0.10 until released to fix lookup of local repo

    private ArtifactRepository localRepository;

    protected File getLocalArtifactPath( Artifact artifact )
    {
        return new File( localRepository.getBasedir(), localRepository.pathOf( artifact ) );
    }

    protected void setUp()
        throws Exception
    {
        super.setUp();

        String localRepo = System.getProperty( "maven.repo.local" );

        if ( localRepo == null )
        {
            File settingsFile = new File( System.getProperty( "user.home" ), ".m2/settings.xml" );
            if ( settingsFile.exists() )
            {
                Settings settings = new SettingsXpp3Reader().read( ReaderFactory.newXmlReader( settingsFile ) );
                localRepo = settings.getLocalRepository();
            }
        }

        if ( localRepo == null )
        {
            localRepo = System.getProperty( "user.home" ) + "/.m2/repository";
        }

        ArtifactRepositoryLayout repositoryLayout = (ArtifactRepositoryLayout) container.lookup(
            ArtifactRepositoryLayout.ROLE, "default" );

        localRepository = new DefaultArtifactRepository( "local", "file://" + localRepo, repositoryLayout );
    }

// END DUPLICATED CODE

}
