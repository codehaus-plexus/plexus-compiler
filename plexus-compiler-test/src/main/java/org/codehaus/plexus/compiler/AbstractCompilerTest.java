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
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.test.ArtifactTestCase;

import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @version $Id$
 */
public abstract class AbstractCompilerTest
    extends ArtifactTestCase
{
    private Compiler compiler;

    private boolean compilerDebug = false;

    protected abstract String getRoleHint();

    public void setUp()
        throws Exception
    {
        super.setUp();
    }

    protected void setCompilerDebug( boolean flag )
    {
        compilerDebug = flag;
    }

    protected List getClasspath()
        throws Exception
    {
        VersionRange versionRange = VersionRange.createFromVersion( "2.0" );

        Artifact artifact = new DefaultArtifact( "commons-lang",
                                                 "commons-lang",
                                                 versionRange,
                                                 Artifact.SCOPE_RUNTIME,
                                                 "jar",
                                                 null,
                                                 new DefaultArtifactHandler( "jar" ) );

        List cp = new ArrayList();

        cp.add( getLocalArtifactPath( artifact ).getAbsolutePath() );

        return cp;
    }

    public void testCompilingSources()
        throws Exception
    {
        List messages = new ArrayList();

        for ( Iterator it = getCompilerConfigurations().iterator(); it.hasNext(); )
        {
            CompilerConfiguration compilerConfig = (CompilerConfiguration) it.next();

            compiler = (Compiler) lookup( Compiler.ROLE, getRoleHint() );

            messages.addAll( compiler.compile( compilerConfig ) );
        }

        int numCompilerErrors = compilerErrorCount( messages );

        int numCompilerWarnings = messages.size() - numCompilerErrors;

        assertEquals( "Wrong number of compilation errors.",
                      expectedErrors(),
                      numCompilerErrors );

        assertEquals( "Wrong number of compilation warnings.",
                      expectedWarnings(),
                      numCompilerWarnings );
    }


    private List getCompilerConfigurations()
        throws Exception
    {
        String sourceDir = getBasedir() + "/src/test-input/src/main";

        List filenames = FileUtils.getFileNames( new File( sourceDir ), "**/*.java", null, false, true );

        List compilerConfigurations = new ArrayList();

        for ( Iterator it = filenames.iterator(); it.hasNext(); )
        {
            String filename = (String) it.next();

            CompilerConfiguration compilerConfig = new CompilerConfiguration();

            compilerConfig.setDebug( compilerDebug );

            compilerConfig.setClasspathEntries( getClasspath() );

            compilerConfig.addSourceLocation( sourceDir );

            compilerConfig.setOutputLocation( getBasedir() + "/target/" + getRoleHint() + "/classes" );

            compilerConfig.addInclude( filename );

            compilerConfigurations.add( compilerConfig );
        }

        return compilerConfigurations;
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
}
