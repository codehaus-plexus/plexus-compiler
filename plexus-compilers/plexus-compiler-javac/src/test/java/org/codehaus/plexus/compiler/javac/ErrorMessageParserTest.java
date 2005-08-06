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

import junit.framework.TestCase;
import org.codehaus.plexus.compiler.CompilerError;

/**
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @version $Id$
 */
public class ErrorMessageParserTest
    extends TestCase
{
    private static final String EOL = System.getProperty( "line.separator" );

    public void testDeprecationMessage()
        throws Exception
    {
        String error = "target/compiler-src/testDeprecation/Foo.java:1: warning: Date(java.lang.String) in java.util.Date has been deprecated" + EOL +
                       "import java.util.Date;public class Foo{    private Date date = new Date( \"foo\");}" + EOL +
                       "                                                               ^" + EOL;

        CompilerError compilerError = JavacCompiler.parseModernError( error );

        assertNotNull( compilerError );

        assertFalse( compilerError.isError() );

        assertEquals( "Date(java.lang.String) in java.util.Date has been deprecated" + EOL, compilerError.getMessage() );

        assertEquals( 63, compilerError.getStartColumn() );

        assertEquals( 66, compilerError.getEndColumn() );

        assertEquals( 1, compilerError.getStartLine() );

        assertEquals( 1, compilerError.getEndLine() );
    }

    public void testWarningMessage()
    {
        String error = "target/compiler-src/testWarning/Foo.java:8: warning: finally clause cannot complete normally" + EOL +
                       "        finally { return; }" + EOL +
                       "                          ^" + EOL;

        CompilerError compilerError = JavacCompiler.parseModernError( error );

        assertNotNull( compilerError );

        assertFalse( compilerError.isError() );

        assertEquals( "finally clause cannot complete normally" + EOL, compilerError.getMessage() );

        assertEquals( 26, compilerError.getStartColumn() );

        assertEquals( 27, compilerError.getEndColumn() );

        assertEquals( 8, compilerError.getStartLine() );

        assertEquals( 8, compilerError.getEndLine() );
    }

    public void testErrorMessage()
    {
        String error = "Foo.java:7: not a statement" + EOL +
                       "         i;" + EOL +
                       "         ^" + EOL;

        CompilerError compilerError = JavacCompiler.parseModernError( error );

        assertNotNull( compilerError );

        assertTrue( compilerError.isError() );

        assertEquals( "not a statement" + EOL, compilerError.getMessage() );

        assertEquals( 9, compilerError.getStartColumn() );

        assertEquals( 11, compilerError.getEndColumn() );

        assertEquals( 7, compilerError.getStartLine() );

        assertEquals( 7, compilerError.getEndLine() );
    }
}
