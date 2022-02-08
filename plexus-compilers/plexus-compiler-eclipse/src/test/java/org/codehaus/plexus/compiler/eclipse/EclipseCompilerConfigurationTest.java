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

import org.codehaus.plexus.compiler.CompilerConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

public class EclipseCompilerConfigurationTest
{
    private static final String PROPERTIES_FILE_NAME =
            "src/test/resources".replace( "/", File.separator ) + File.separator
                    + EclipseCompilerConfigurationTest.class.getName().replace( ".", File.separator )
                    + "-test.properties";

    private CompilerConfiguration configuration;

    @BeforeEach
    protected void setUp()
    {
        configuration = new CompilerConfiguration();
    }

    @Test
    public void testProcessCustomArgumentsWithMultipleAddExports()
    {
        configuration.addCompilerCustomArgument( "--add-exports", "FROM-MOD/package1=OTHER-MOD" );
        configuration.addCompilerCustomArgument( "--add-exports", "FROM-MOD/package2=OTHER-MOD" );
        List<String> args = new ArrayList<>();
        EclipseJavaCompiler.processCustomArguments( configuration, args );
        assertThat( args.size(), is(4) );
        assertThat( args, contains("--add-exports", "FROM-MOD/package1=OTHER-MOD", "--add-exports", "FROM-MOD/package2=OTHER-MOD"));
//        assertThat( args.get( 0 ), is("--add-exports") );
//        assertThat( args.get( 1 ), is("FROM-MOD/package1=OTHER-MOD") );
//        assertThat( args.get( 2 ), is("--add-exports") );
//        assertThat( args.get( 3 ), is("FROM-MOD/package2=OTHER-MOD") );
    }

    @Test
    public void testProcessCustomArgumentsWithProceedOnError()
    {
        configuration.addCompilerCustomArgument( "-proceedOnError", null );
        List<String> args = new ArrayList<>();
        EclipseJavaCompiler.processCustomArguments( configuration, args );
        assertThat( args.size(), is(1) );
        assertThat( args, contains("-proceedOnError:Fatal") );
    }

    @Test
    public void testProcessCustomArgumentsWithErrorsAsWarnings()
    {
        configuration.addCompilerCustomArgument( "errorsAsWarnings", null );
        List<String> args = new ArrayList<>();
        final boolean errorsAsWarnings = EclipseJavaCompiler.processCustomArguments( configuration, args );
        assertThat( errorsAsWarnings, is(true) );
    }

    @Test
    public void testProcessCustomArgumentsWithErrorsAsWarningsAndMinus()
    {
        configuration.addCompilerCustomArgument( "-errorsAsWarnings", null );
        List<String> args = new ArrayList<>();
        final boolean errorsAsWarnings = EclipseJavaCompiler.processCustomArguments( configuration, args );
        assertThat( errorsAsWarnings, is(true) );
    }

    @Test
    public void testProcessCustomArgumentsWithPropertiesAndNonExistingFile()
    {
        configuration.addCompilerCustomArgument( "-properties", "fooBar.txt" );
        IllegalArgumentException expected =
                Assertions.assertThrows(IllegalArgumentException.class,
                () -> EclipseJavaCompiler.processCustomArguments( configuration, Collections.emptyList() ));
        assertThat( expected.getMessage(),
                is( "Properties file specified by -properties fooBar.txt does not exist"));
    }

    @Test
    public void testProcessCustomArgumentsWithPropertiesAndValidFile()
    {
        configuration.addCompilerCustomArgument( "-properties", PROPERTIES_FILE_NAME );
        List<String> args = new ArrayList<>();
        EclipseJavaCompiler.processCustomArguments( configuration, args );
        assertThat( args.size(), is(2));
        assertThat(args, contains("-properties", PROPERTIES_FILE_NAME));
    }
}
