package org.codehaus.plexus.compiler.csharp;

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

import org.codehaus.plexus.compiler.CompilerError;

import java.util.List;
import java.io.StringReader;
import java.io.BufferedReader;
import java.io.IOException;

import junit.framework.TestCase;

/**
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @version $Id$
 */
public class CSharpCompilerTest
    extends TestCase
{
    public void testParser()
        throws IOException
    {
        CompilerError error;

        // ----------------------------------------------------------------------
        // Test a few concrete lines
        // ----------------------------------------------------------------------

        error = CSharpCompiler.parseLine( "error CS2008: No files to compile were specified" );

        assertNotNull( error );

        assertEquals( "CS2008: No files to compile were specified", error.getMessage() );

        error = CSharpCompiler.parseLine( "Compilation failed: 1 error(s), 0 warnings" );

        assertNull( error );

        error = CSharpCompiler.parseLine( "Compilation succeeded - 2 warning(s)" );

        assertNull( error );

        error = CSharpCompiler.parseLine( "/home/trygvis/dev/com.myrealbox/trunk/mcs/nunit20/core/./TestRunnerThread.cs(29) error CS0246: Cannot find type 'NameValueCollection'" );

        assertNotNull( error );

        assertEquals( 29, error.getStartLine() );

        assertEquals( -1, error.getStartColumn() );

        assertEquals( 29, error.getEndLine() );

        assertEquals( -1, error.getEndColumn() );

        assertEquals( "CS0246: Cannot find type 'NameValueCollection'", error.getMessage() );

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        String input = "/home/trygvis/dev/com.myrealbox/trunk/mcs/nunit20/core/./TestRunnerThread.cs(5) error CS0234: The type or namespace name `Specialized' could not be found in namespace `System.Collections'\n" +
                       "/home/trygvis/dev/com.myrealbox/trunk/mcs/nunit20/core/./TestRunnerThread.cs(29) error CS0246: Cannot find type 'NameValueCollection'\n" +
                       "/home/trygvis/dev/com.myrealbox/trunk/mcs/nunit20/core/./Reflect.cs(4) error CS0234: The type or namespace name `Framework' could not be found in namespace `NUnit'\n" +
                       "/home/trygvis/dev/com.myrealbox/trunk/mcs/nunit20/core/./Reflect.cs(123) error CS0246: Cannot find type 'TestFixtureAttribute'\n" +
                       "/home/trygvis/dev/com.myrealbox/trunk/mcs/nunit20/core/./Reflect.cs(129) error CS0246: Cannot find type 'TestAttribute'\n" +
                       "/home/trygvis/dev/com.myrealbox/trunk/mcs/nunit20/core/./Reflect.cs(135) error CS0246: Cannot find type 'IgnoreAttribute'\n" +
                       "/home/trygvis/dev/com.myrealbox/trunk/mcs/nunit20/core/./Reflect.cs(141) error CS0246: Cannot find type 'ExpectedExceptionAttribute'\n" +
                       "/home/trygvis/dev/com.myrealbox/trunk/mcs/nunit20/core/./WarningSuite.cs(49) error CS0506: `NUnit.Core.WarningSuite.Add': cannot override inherited member `TestSuite.Add' because it is not virtual, abstract or override\n" +
                       "/home/trygvis/dev/com.myrealbox/trunk/mcs/nunit20/core/./WarningSuite.cs(49) error CS0507: `NUnit.Core.WarningSuite.Add': can't change the access modifiers when overriding inherited member `TestSuite.Add'\n" +
                       "/home/trygvis/dev/com.myrealbox/trunk/mcs/nunit20/core/./WarningSuite.cs(56) error CS0115: 'NUnit.Core.WarningSuite.CreateNewSuite(System.Type)': no suitable methods found to override\n" +
                       "/home/trygvis/dev/com.myrealbox/trunk/mcs/nunit20/core/./TestRunnerThread.cs(5) error CS0234: The type or namespace name `Specialized' could not be found in namespace `System.Collections'\n" +
                       "/home/trygvis/dev/com.myrealbox/trunk/mcs/nunit20/core/./TestRunnerThread.cs(5) error CS0246: The namespace `System.Collections.Specialized' can not be found (missing assembly reference?)\n" +
                       "/home/trygvis/dev/com.myrealbox/trunk/mcs/nunit20/core/./Reflect.cs(4) error CS0234: The type or namespace name `Framework' could not be found in namespace `NUnit'\n" +
                       "/home/trygvis/dev/com.myrealbox/trunk/mcs/nunit20/core/./Reflect.cs(4) error CS0246: The namespace `NUnit.Framework' can not be found (missing assembly reference?)\n" +
                       "Compilation failed: 14 error(s), 0 warnings";

        List messages = CSharpCompiler.parseCompilerOutput( new BufferedReader( new StringReader( input ) ) );

        assertNotNull( messages );

        assertEquals( 14, messages.size() );
    }
}
