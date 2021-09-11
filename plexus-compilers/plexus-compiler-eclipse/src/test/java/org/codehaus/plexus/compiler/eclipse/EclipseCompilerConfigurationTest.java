package org.codehaus.plexus.compiler.eclipse;

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

import junit.framework.TestCase;
import org.codehaus.plexus.compiler.CompilerConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EclipseCompilerConfigurationTest
        extends TestCase
{
    private static final String PROPERTIES_FILE_NAME =
            "src/test/resources".replace( "/", File.separator ) + File.separator
                    + EclipseCompilerConfigurationTest.class.getName().replace( ".", File.separator )
                    + "-test.properties";

    private CompilerConfiguration configuration;

    @Override
    protected void setUp()
    {
        configuration = new CompilerConfiguration();
    }

    public void testProcessCustomArgumentsWithMultipleAddExports()
    {
        configuration.addCompilerCustomArgument( "--add-exports", "FROM-MOD/package1=OTHER-MOD" );
        configuration.addCompilerCustomArgument( "--add-exports", "FROM-MOD/package2=OTHER-MOD" );
        List<String> args = new ArrayList<>();
        EclipseJavaCompiler.processCustomArguments( configuration, args );
        assertEquals( 4, args.size() );
        assertEquals( "--add-exports", args.get( 0 ) );
        assertEquals( "FROM-MOD/package1=OTHER-MOD", args.get( 1 ) );
        assertEquals( "--add-exports", args.get( 2 ) );
        assertEquals( "FROM-MOD/package2=OTHER-MOD", args.get( 3 ) );
    }

    public void testProcessCustomArgumentsWithProceedOnError()
    {
        configuration.addCompilerCustomArgument( "-proceedOnError", null );
        List<String> args = new ArrayList<>();
        EclipseJavaCompiler.processCustomArguments( configuration, args );
        assertEquals( 1, args.size() );
        assertEquals( "-proceedOnError:Fatal", args.get( 0 ) );
    }

    public void testProcessCustomArgumentsWithErrorsAsWarnings()
    {
        configuration.addCompilerCustomArgument( "errorsAsWarnings", null );
        List<String> args = new ArrayList<>();
        final boolean errorsAsWarnings = EclipseJavaCompiler.processCustomArguments( configuration, args );
        assertTrue( errorsAsWarnings );
    }

    public void testProcessCustomArgumentsWithErrorsAsWarningsAndMinus()
    {
        configuration.addCompilerCustomArgument( "-errorsAsWarnings", null );
        List<String> args = new ArrayList<>();
        final boolean errorsAsWarnings = EclipseJavaCompiler.processCustomArguments( configuration, args );
        assertTrue( errorsAsWarnings );
    }

    public void testProcessCustomArgumentsWithPropertiesAndNonExistingFile()
    {
        configuration.addCompilerCustomArgument( "-properties", "fooBar.txt" );
        try
        {
            EclipseJavaCompiler.processCustomArguments( configuration, Collections.<String>emptyList() );
            fail( "IllegalArgumentException expected" );
        } catch ( IllegalArgumentException expected )
        {
            assertEquals( "Properties file specified by -properties fooBar.txt does not exist", expected.getMessage() );
        }
    }

    public void testProcessCustomArgumentsWithPropertiesAndValidFile()
    {
        configuration.addCompilerCustomArgument( "-properties", PROPERTIES_FILE_NAME );
        List<String> args = new ArrayList<>();
        EclipseJavaCompiler.processCustomArguments( configuration, args );
        assertEquals( 2, args.size() );
        assertEquals( "-properties", args.get( 0 ) );
        assertEquals( PROPERTIES_FILE_NAME, args.get( 1 ) );
    }
}
