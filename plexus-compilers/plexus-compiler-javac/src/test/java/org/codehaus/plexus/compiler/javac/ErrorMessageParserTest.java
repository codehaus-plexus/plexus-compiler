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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;

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

    public void testUnknownSymbolError()
    {
        String error = "./org/codehaus/foo/UnknownSymbol.java:7: cannot find symbol" + EOL +
                       "symbol  : method foo()" + EOL +
                       "location: class org.codehaus.foo.UnknownSymbol" + EOL +
                       "        foo();" + EOL +
                       "        ^" + EOL;

        CompilerError compilerError = JavacCompiler.parseModernError( error );

        assertNotNull( compilerError );

        assertTrue( compilerError.isError() );

        assertEquals( "cannot find symbol" + EOL +
            "symbol  : method foo()" + EOL +
            "location: class org.codehaus.foo.UnknownSymbol" + EOL,
            compilerError.getMessage()
        );

        assertEquals( 8, compilerError.getStartColumn() );

        assertEquals( 14, compilerError.getEndColumn() );

        assertEquals( 7, compilerError.getStartLine() );

        assertEquals( 7, compilerError.getEndLine() );
    }



    public void testTwoErrors()
        throws IOException
    {
        String errors = "./org/codehaus/foo/ExternalDeps.java:4: package org.apache.commons.lang does not exist" + EOL +
                        "import org.apache.commons.lang.StringUtils;" + EOL +
                        "                               ^" + EOL +
                        "./org/codehaus/foo/ExternalDeps.java:12: cannot find symbol" + EOL +
                        "symbol  : variable StringUtils" + EOL +
                        "location: class org.codehaus.foo.ExternalDeps" + EOL +
                        "          System.out.println( StringUtils.upperCase( str)  );" + EOL +
                        "                              ^" + EOL +
                        "2 errors" + EOL;

        class MyTestJavacCompiler extends JavacCompiler
        {
            public List parseErrors( BufferedReader reader )
                throws IOException
            {
                return parseModernStream( reader );
            }
        }

        MyTestJavacCompiler myTestCompiler = new MyTestJavacCompiler();

        List messages = myTestCompiler.parseErrors( new BufferedReader( new StringReader( errors ) ) );

        assertEquals( 2, messages.size() );
    }


    public void testAnotherTwoErrors()
        throws IOException
    {
        String errors = "./org/codehaus/foo/ExternalDeps.java:4: package org.apache.commons.lang does not exist" + EOL +
                        "import org.apache.commons.lang.StringUtils;" + EOL +
                        "                               ^" + EOL +
                        "./org/codehaus/foo/ExternalDeps.java:12: cannot find symbol" + EOL +
                        "symbol  : variable StringUtils" + EOL +
                        "location: class org.codehaus.foo.ExternalDeps" + EOL +
                        "          System.out.println( StringUtils.upperCase( str)  );" + EOL +
                        "                              ^" + EOL +
                        "2 errors" + EOL;

        class MyTestJavacCompiler extends JavacCompiler
        {
            public List parseErrors( BufferedReader reader )
                throws IOException
            {
                return parseModernStream( reader );
            }
        }

        MyTestJavacCompiler myTestCompiler = new MyTestJavacCompiler();

        List messages = myTestCompiler.parseErrors( new BufferedReader( new StringReader( errors ) ) );

        assertEquals( 2, messages.size() );
    }

    public void testAssertError()
        throws IOException
    {
        String errors = "./org/codehaus/foo/ReservedWord.java:5: as of release 1.4, 'assert' is a keyword, and may not be used as an identifier" + EOL +
                        "(try -source 1.3 or lower to use 'assert' as an identifier)" + EOL +
                        "        String assert;" + EOL +
                        "               ^" + EOL +
                        "1 error" + EOL;

        class MyTestJavacCompiler extends JavacCompiler
        {
            public List parseErrors( BufferedReader reader )
                throws IOException
            {
                return parseModernStream( reader );
            }
        }

        MyTestJavacCompiler myTestCompiler = new MyTestJavacCompiler();

        List messages = myTestCompiler.parseErrors( new BufferedReader( new StringReader( errors ) ) );

        assertEquals( 1, messages.size() );
    }

}
