package org.codehaus.plexus.compiler;

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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.codehaus.plexus.testing.PlexusTest;
import org.codehaus.plexus.util.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 */
@PlexusTest
public abstract class AbstractCompilerTckTest
{
    private static final String EOL = System.lineSeparator();

    private final String roleHint;
    
    private TestInfo testInfo;
    
    @Inject
    private Map<String, Compiler> compilers;

    protected AbstractCompilerTckTest( String roleHint )
    {
        this.roleHint = roleHint;
    }
    
    @BeforeEach
    final void setup( TestInfo testInfo )
    {
        this.testInfo = testInfo;
    }

    @Test
    public void testDeprecation()
        throws Exception
    {
        File foo = new File( getSrc(), "Foo.java" );

        writeFileWithDeprecatedApi( foo, "Foo" );

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        CompilerConfiguration configuration = new CompilerConfiguration();

        configuration.setShowDeprecation( true );

        configuration.addSourceLocation( getSrc().getAbsolutePath() );

        List<CompilerMessage> result = compile( configuration );

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        assertThat( result.size() ).isEqualTo( 1 );

        CompilerMessage error = result.get( 0 );

        assertThat( error.isError() ).isFalse();

        assertThat( error.getMessage() ).contains( "Date" );

        assertThat( error.getMessage() ).contains( "deprecated" );
    }

    @Test
    public void testWarning()
        throws Exception
    {
        File foo = new File( getSrc(), "Foo.java" );

        writeFileWithWarning( foo, "Foo" );

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        CompilerConfiguration configuration = new CompilerConfiguration();

        configuration.setShowWarnings( true );

        configuration.addSourceLocation( getSrc().getAbsolutePath() );

        List<CompilerMessage> result = compile( configuration );

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        assertThat( result.size() ).isEqualTo( 1 );

        CompilerMessage error = result.get( 0 );

        assertThat( error.isError() ).isFalse();

        assertThat( error.getMessage() ).contains( "finally block does not complete normally" );
    }
    
    protected List<CompilerMessage> compile( CompilerConfiguration configuration )
        throws Exception
    {
        // ----------------------------------------------------------------------
        // Set up configuration
        // ----------------------------------------------------------------------

        File compilerOutput = getCompilerOutput();

        if ( compilerOutput.exists() )
        {
            FileUtils.deleteDirectory( compilerOutput );
        }

        configuration.setOutputLocation( compilerOutput.getAbsolutePath() );

        // ----------------------------------------------------------------------
        // Compile!
        // ----------------------------------------------------------------------

        List<CompilerMessage> result = getCompiler().performCompile( configuration ).getCompilerMessages();

        assertThat( result ).isNotNull();

        return result;
    }

    private Compiler getCompiler() {
        return compilers.get( roleHint );
    }
    
    private File getCompilerOutput()
    {
        return new File( "target/compiler-output/" + testInfo.getTestMethod().map( Method::getName ).orElseThrow( null ) );
    }

    private File getSrc()
    {
        return new File( "target/compiler-src/" + testInfo.getTestMethod().map( Method::getName ).orElseThrow( null ) );
    }

    protected void writeFileWithDeprecatedApi( File path, String className )
        throws IOException
    {
        File parent = path.getParentFile();

        if ( !parent.exists() )
        {
            assertThat( parent.mkdirs() ).isTrue();
        }

        String source = "import java.util.Date;" + EOL +
            "" + EOL +
            "public class " + className + "" + EOL +
            "{" + EOL +
            "    private static Date date = new Date( \"foo\" );" + EOL +
            "    static " + EOL +
            "    { " + EOL +
            "        Date date = " + className + ".date; " + EOL +
            "        Date date2 = date; " + EOL +
            "        date = date2; " + EOL +
            "    }" + EOL +
            "}";

        FileUtils.fileWrite( path.getAbsolutePath(), source );
    }

    protected void writeFileWithWarning( File path, String className )
        throws IOException
    {
        File parent = path.getParentFile();

        if ( !parent.exists() )
        {
            assertThat( parent.mkdirs() ).isTrue();
        }

        String source = "public class " + className + "" + EOL +
            "{" + EOL +
            "    public void foo()" + EOL +
            "    {" + EOL +
            "        try{ throw new java.io.IOException(); }" + EOL +
            "        finally { return; }" + EOL +
            "    }" + EOL +
            "}";

        FileUtils.fileWrite( path.getAbsolutePath(), source );
    }
}
