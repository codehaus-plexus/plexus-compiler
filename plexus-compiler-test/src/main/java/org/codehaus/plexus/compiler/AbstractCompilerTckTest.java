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

import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @version $Id$
 */
public abstract class AbstractCompilerTckTest
    extends PlexusTestCase
{
    private static final String EOL = System.getProperty( "line.separator" );

    private String roleHint;

    protected AbstractCompilerTckTest( String roleHint )
    {
        this.roleHint = roleHint;
    }

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

        List result = compile( configuration );

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        assertEquals( 1, result.size() );

        CompilerError error = (CompilerError) result.get( 0 );

        System.out.println( error.getMessage() );

        assertFalse( error.isError() );

        assertTrue( error.getMessage().indexOf( "Date" ) != -1 );

        assertTrue( error.getMessage().indexOf( "deprecated" ) != -1 );
    }

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

        List result = compile( configuration );

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        assertEquals( 1, result.size() );

        CompilerError error = (CompilerError) result.get( 0 );

        assertFalse( error.isError() );

        assertTrue( error.getMessage().indexOf( "finally block does not complete normally" ) != -1 );
    }

    protected List compile( CompilerConfiguration configuration )
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

        Compiler compiler = (Compiler) lookup( Compiler.ROLE, roleHint );

        List result = compiler.compile( configuration );

        assertNotNull( result );

        return result;
    }

    private File getCompilerOutput()
    {
        return getTestFile( "target/compiler-output/" + getName() );
    }

    private File getSrc()
    {
        return getTestFile( "target/compiler-src/" + getName() );
    }

    protected void writeFileWithDeprecatedApi( File path, String className )
        throws IOException
    {
        File parent = path.getParentFile();

        if ( !parent.exists() )
        {
            assertTrue( parent.mkdirs() );
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
            assertTrue( parent.mkdirs() );
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
