package org.codehaus.plexus.compiler.javac;

/**
 * The MIT License
 *
 * Copyright (c) 2005, The Codehaus
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

import org.codehaus.plexus.compiler.AbstractCompilerTest;
import org.codehaus.plexus.compiler.CompilerConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * @author <a href="mailto:jason@plexus.org">Jason van Zyl</a>
 * @version $Id$
 */
public class JavacCompilerTest
    extends AbstractCompilerTest
{
    private static final String PS = System.getProperty( "path.separator" );

    public void setUp()
        throws Exception
    {
        super.setUp();
        setCompilerDebug( true );
        setCompilerDeprecationWarnings( true );
    }

    protected String getRoleHint()
    {
        return "javac";
    }

    protected int expectedErrors()
    {
        return 3;
    }

    protected int expectedWarnings()
    {
        return 2;
    }

    public void internalTest( CompilerConfiguration compilerConfiguration, List expectedArguments )
    {
        String[] actualArguments = JavacCompiler.buildCompilerArguments( compilerConfiguration, new String[0] );

        assertEquals( "The expected and actual argument list sizes differ.",
                      expectedArguments.size(),
                      actualArguments.length );

        for ( int i=0; i<actualArguments.length; i++ )
        {
            assertEquals( "Unexpected argument", expectedArguments.get(i), actualArguments[i] );
        }
    }

    public void testBuildCompilerArgs13()
    {
        List expectedArguments = new ArrayList();

        CompilerConfiguration compilerConfiguration = new CompilerConfiguration();

        compilerConfiguration.setCompilerVersion( "1.3" );

        populateArguments( compilerConfiguration, expectedArguments, true, true );

        internalTest( compilerConfiguration, expectedArguments );
    }

    public void testBuildCompilerArgs14()
    {
        List expectedArguments = new ArrayList();

        CompilerConfiguration compilerConfiguration = new CompilerConfiguration();

        compilerConfiguration.setCompilerVersion( "1.4" );

        populateArguments( compilerConfiguration, expectedArguments, false, false );

        internalTest( compilerConfiguration, expectedArguments );
    }

    public void testBuildCompilerArgs15()
    {
        List expectedArguments = new ArrayList();

        CompilerConfiguration compilerConfiguration = new CompilerConfiguration();

        compilerConfiguration.setCompilerVersion( "1.5" );

        populateArguments( compilerConfiguration, expectedArguments, false, false );

        internalTest( compilerConfiguration, expectedArguments );
    }

    public void testBuildCompilerArgsUnspecifiedVersion()
    {
        List expectedArguments = new ArrayList();

        CompilerConfiguration compilerConfiguration = new CompilerConfiguration();

        populateArguments( compilerConfiguration, expectedArguments, false, false );

        internalTest( compilerConfiguration, expectedArguments );
    }

    private void populateArguments( CompilerConfiguration compilerConfiguration, List expectedArguments,
                                    boolean suppressSourceVersion, boolean suppressEncoding )
    {
        // outputLocation

        compilerConfiguration.setOutputLocation( "/output" );

        expectedArguments.add( "-d" );

        expectedArguments.add( new File( "/output" ).getAbsolutePath() );

        // classpathEntires

        List classpathEntries = new ArrayList();

        classpathEntries.add( "/myjar1.jar" );

        classpathEntries.add( "/myjar2.jar" );

        compilerConfiguration.setClasspathEntries( classpathEntries );

        expectedArguments.add( "-classpath" );

        expectedArguments.add( "/myjar1.jar" + PS + "/myjar2.jar" + PS );

        // sourceRoots

        List compileSourceRoots = new ArrayList();

        compileSourceRoots.add( "/src/main/one" );

        compileSourceRoots.add( "/src/main/two" );

        compilerConfiguration.setSourceLocations( compileSourceRoots );

        expectedArguments.add( "-sourcepath" );

        expectedArguments.add( "/src/main/one" + PS + "/src/main/two" + PS );

        // debug

        compilerConfiguration.setDebug( true );

        expectedArguments.add( "-g" );

        // showDeprecation

        compilerConfiguration.setShowDeprecation( true );

        expectedArguments.add( "-deprecation" );

        // targetVersion

        compilerConfiguration.setTargetVersion( "1.3" );

        expectedArguments.add( "-target" );

        expectedArguments.add( "1.3" );

        // sourceVersion

        compilerConfiguration.setSourceVersion( "1.3" );

        if ( !suppressSourceVersion )
        {
            expectedArguments.add( "-source" );

            expectedArguments.add( "1.3" );
        }

        // sourceEncoding

        compilerConfiguration.setSourceEncoding( "iso-8859-1" );

        if ( !suppressEncoding )
        {
            expectedArguments.add( "-encoding" );

            expectedArguments.add( "iso-8859-1" );
        }

        // customerCompilerArguments

        LinkedHashMap customerCompilerArguments = new LinkedHashMap();

        customerCompilerArguments.put( "arg1", null );

        customerCompilerArguments.put( "foo", "bar" );

        compilerConfiguration.setCustomCompilerArguments( customerCompilerArguments );

        expectedArguments.add( "arg1" );

        expectedArguments.add( "foo" );

        expectedArguments.add( "bar" );
    }
}
