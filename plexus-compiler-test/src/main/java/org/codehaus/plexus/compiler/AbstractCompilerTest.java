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

    private CompilerConfiguration compilerConfig;

    protected abstract String getRoleHint();

    public void setUp()
        throws Exception
    {
        super.setUp();

        String basedir = getBasedir();

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

    protected CompilerConfiguration getCompilerConfiguration()
    {
        return compilerConfig;
    }

    public void testCompilingSources()
        throws Exception
    {
        List messages = compiler.compile( compilerConfig );

        System.err.println( "Compiler messages: " );

        for ( Iterator iter = messages.iterator(); iter.hasNext(); )
        {
            System.out.println( iter.next() );
        }

        assertEquals( "Wrong number of compilation errors.",
                      expectedErrors(),
                      messages.size() );
    }

    protected int expectedErrors()
    {
        return 1;
    }
}
