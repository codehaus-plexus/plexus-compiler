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

import org.codehaus.plexus.compiler.CompilerMessage;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 */
public class CSharpCompilerTest
{
    @Test
    public void testParser()
        throws IOException
    {
        CompilerMessage error;

        // ----------------------------------------------------------------------
        // Test a few concrete lines
        // ----------------------------------------------------------------------

        error = DefaultCSharpCompilerParser.parseLine( "error CS2008: No files to compile were specified" );

        assertThat(error, notNullValue());


        assertThat( error.getMessage(), is("CS2008: No files to compile were specified"));

        error = DefaultCSharpCompilerParser.parseLine( "Compilation failed: 1 error(s), 0 warnings" );

        assertThat( error, nullValue() );

        error = DefaultCSharpCompilerParser.parseLine( "Compilation succeeded - 2 warning(s)" );

        assertThat( error, nullValue() );

        error = DefaultCSharpCompilerParser.parseLine(
            "/home/trygvis/dev/com.myrealbox/trunk/mcs/nunit20/core/./TestRunnerThread.cs(29) error CS0246: Cannot find type 'NameValueCollection'" );

        assertThat( error, notNullValue() );

        assertThat( error.getStartLine(), is(29));

        assertThat( error.getStartColumn(), is(-1) );

        assertThat( error.getEndLine(), is(29) );

        assertThat( error.getEndColumn(), is(-1) );

        assertThat( error.getMessage(), is( "CS0246: Cannot find type 'NameValueCollection'" ) );

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        String input =
            "/home/trygvis/dev/com.myrealbox/trunk/mcs/nunit20/core/./TestRunnerThread.cs(5) error CS0234: The type or namespace name `Specialized' could not be found in namespace `System.Collections'\n"
                +
                "/home/trygvis/dev/com.myrealbox/trunk/mcs/nunit20/core/./TestRunnerThread.cs(29) error CS0246: Cannot find type 'NameValueCollection'\n"
                +
                "/home/trygvis/dev/com.myrealbox/trunk/mcs/nunit20/core/./Reflect.cs(4) error CS0234: The type or namespace name `Framework' could not be found in namespace `NUnit'\n"
                +
                "/home/trygvis/dev/com.myrealbox/trunk/mcs/nunit20/core/./Reflect.cs(123) error CS0246: Cannot find type 'TestFixtureAttribute'\n"
                +
                "/home/trygvis/dev/com.myrealbox/trunk/mcs/nunit20/core/./Reflect.cs(129) error CS0246: Cannot find type 'TestAttribute'\n"
                +
                "/home/trygvis/dev/com.myrealbox/trunk/mcs/nunit20/core/./Reflect.cs(135) error CS0246: Cannot find type 'IgnoreAttribute'\n"
                +
                "/home/trygvis/dev/com.myrealbox/trunk/mcs/nunit20/core/./Reflect.cs(141) error CS0246: Cannot find type 'ExpectedExceptionAttribute'\n"
                +
                "/home/trygvis/dev/com.myrealbox/trunk/mcs/nunit20/core/./WarningSuite.cs(49) error CS0506: `NUnit.Core.WarningSuite.Add': cannot override inherited member `TestSuite.Add' because it is not virtual, abstract or override\n"
                +
                "/home/trygvis/dev/com.myrealbox/trunk/mcs/nunit20/core/./WarningSuite.cs(49) error CS0507: `NUnit.Core.WarningSuite.Add': can't change the access modifiers when overriding inherited member `TestSuite.Add'\n"
                +
                "/home/trygvis/dev/com.myrealbox/trunk/mcs/nunit20/core/./WarningSuite.cs(56) error CS0115: 'NUnit.Core.WarningSuite.CreateNewSuite(System.Type)': no suitable methods found to override\n"
                +
                "/home/trygvis/dev/com.myrealbox/trunk/mcs/nunit20/core/./TestRunnerThread.cs(5) error CS0234: The type or namespace name `Specialized' could not be found in namespace `System.Collections'\n"
                +
                "/home/trygvis/dev/com.myrealbox/trunk/mcs/nunit20/core/./TestRunnerThread.cs(5) error CS0246: The namespace `System.Collections.Specialized' can not be found (missing assembly reference?)\n"
                +
                "/home/trygvis/dev/com.myrealbox/trunk/mcs/nunit20/core/./Reflect.cs(4) error CS0234: The type or namespace name `Framework' could not be found in namespace `NUnit'\n"
                +
                "/home/trygvis/dev/com.myrealbox/trunk/mcs/nunit20/core/./Reflect.cs(4) error CS0246: The namespace `NUnit.Framework' can not be found (missing assembly reference?)\n"
                +
                "Compilation failed: 14 error(s), 0 warnings";

        List<CompilerMessage> messages =
            CSharpCompiler.parseCompilerOutput( new BufferedReader( new StringReader( input ) ) );

        assertThat( messages, notNullValue() );

        assertThat(  messages.size(), is(14) );
    }

    @Test
    public void testParserCscWin()
        throws Exception
    {

        String cscWin =
            "src\\test\\csharp\\Hierarchy\\Logger.cs(77,4): warning CS0618: 'NUnit.Framework.Assertion' is obsolete: 'Use Assert class instead'\n"
                +
                "src\\test\\csharp\\Hierarchy\\Logger.cs(98,4): warning CS0618: 'NUnit.Framework.Assertion' is obsolete: 'Use Assert class instead'\n"
                +
                "src\\test\\csharp\\Hierarchy\\Logger.cs(126,4): warning CS0618: 'NUnit.Framework.Assertion' is obsolete: 'Use Assert class instead'\n"
                +
                "src\\test\\csharp\\Hierarchy\\Logger.cs(151,4): warning CS0618: 'NUnit.Framework.Assertion' is obsolete: 'Use Assert class instead'\n"
                +
                "src\\test\\csharp\\Hierarchy\\Logger.cs(187,4): warning CS0618: 'NUnit.Framework.Assertion' is obsolete: 'Use Assert class instead'\n"
                +
                "src\\test\\csharp\\Hierarchy\\Logger.cs(222,4): warning CS0618: 'NUnit.Framework.Assertion' is obsolete: 'Use Assert class instead'\n"
                +
                "src\\test\\csharp\\Hierarchy\\Logger.cs(255,33): warning CS0618: 'NUnit.Framework.Assertion' is obsolete: 'Use Assert class instead'\n"
                +
                "src\\test\\csharp\\Hierarchy\\Logger.cs(270,4): warning CS0618: 'NUnit.Framework.Assertion' is obsolete: 'Use Assert class instead'\n"
                +
                "src\\test\\csharp\\Util\\PropertiesDictionaryTest.cs(56,4): warning CS0618: 'NUnit.Framework.Assertion' is obsolete: 'Use Assert class instead'\n"
                +
                "src\\main\\csharp\\Flow\\Loader.cs(62,27): error CS0246: The type or namespace name 'log4net' could not be found (are you missing a using directive or an assembly reference?)\n"
                +
                "src\\main\\csharp\\Ctl\\Aspx\\AspxController.cs(18,27): error CS0246: The type or namespace name 'log4net' could not be found (are you missing a using directive or an assembly\n"
                +
                "src\\main\\csharp\\Transform\\XsltTransform.cs(78,11): error CS0246: The type or namespace name 'log4net' could not be found (are you missing a using directive or an assembly\n"
                +
                "src\\main\\csharp\\View\\DispatchedViewFactory.cs(20,27): error CS0246: The type or namespace name 'log4net' could not be found (are you missing a using directive or an assemb\n"
                +
                "src\\main\\csharp\\Flow\\ViewRegistry.cs(31,27): error CS0246: The type or namespace name 'log4net' could not be found (are you missing a using directive or an assembly refere\n"
                +
                "src\\main\\csharp\\Flow\\MasterFactory.cs(50,27): error CS0246: The type or namespace name 'log4net' could not be found (are you missing a using directive or an assembly refer\n"
                +
                "src\\main\\csharp\\Dispatcher.cs(152,27): error CS0246: The type or namespace name 'log4net' could not be found (are you missing a using directive or an assembly reference?)\n"
                +
                "src\\main\\csharp\\Flow\\MaverickContext.cs(43,27): error CS0246: The type or namespace name 'log4net' could not be found (are you missing a using directive or an assembly ref\n"
                +
                "src\\main\\csharp\\Transform\\DocumentTransform.cs(38,12): error CS0246: The type or namespace name 'log4net' could not be found (are you missing a using directive or an assem\n"
                +
                "src\\main\\csharp\\Flow\\CommandBase.cs(11,27): error CS0246: The type or namespace name 'log4net' could not be found (are you missing a using directive or an assembly referen\n"
                +
                "src\\main\\csharp\\Shunt\\LanguageShuntFactory.cs(47,27): error CS0246: The type or namespace name 'log4net' could not be found (are you missing a using directive or an assemb\n"
                +
                "src\\main\\csharp\\Shunt\\LanguageShuntFactory.cs(67,27): error CS0246: The type or namespace name 'log4net' could not be found (are you missing a using directive or an assemb\n"
                +
                "src\\main\\csharp\\Util\\PropertyPopulator.cs(19,27): error CS0246: The type or namespace name 'log4net' could not be found (are you missing a using directive or an assembly r\n"
                +
                "src\\main\\csharp\\Ctl\\ThrowawayForm.cs(30,27): error CS0246: The type or namespace name 'log4net' could not be found (are you missing a using directive or an assembly refere\n"
                +
                "src\\main\\csharp\\AssemblyInfo.cs(68,12): error CS0246: The type or namespace name 'log4net' could not be found (are you missing a using directive or an assembly reference?)\n";

        List<CompilerMessage> messagesWinCsc =
            CSharpCompiler.parseCompilerOutput( new BufferedReader( new StringReader( cscWin ) ) );

        assertThat( messagesWinCsc, notNullValue() );

        assertThat( messagesWinCsc.size(), is(24) );

        assertThat( "Check that the line number is not -1",
                    messagesWinCsc.get( 0 ).getStartLine(), not(-1) );
        assertThat( "Check that the column number is not -1",
                    messagesWinCsc.get( 0 ).getStartColumn(), not(-1) );

    }

    @Test
    public void testParserMonoWin()
        throws Exception
    {

        String monoWin =
            "C:\\Work\\SCM\\SVN\\javaforge\\maven-csharp\\trunk\\maverick-net\\src\\main\\csharp\\Ctl\\ThrowawayForm.cs(30,27): error CS0246: The type or namespace name `log4net' could not be found. Are you missing a using directive or an assembly reference? error CS0234: No such name or typespace log4net\n"
                +
                "C:\\Work\\SCM\\SVN\\javaforge\\maven-csharp\\trunk\\maverick-net\\src\\main\\csharp\\Util\\PropertyPopulator.cs(19,27): error CS0246: The type or namespace name `log4net' could not be found. Are you missing a using directive or an assembly reference? error CS0234: No such name or typespace log4net\n"
                +
                "C:\\Work\\SCM\\SVN\\javaforge\\maven-csharp\\trunk\\maverick-net\\src\\main\\csharp\\Flow\\ViewRegistry.cs(31,27): error CS0246: The type or namespace name `log4net' could not be found. Are you missing a using directive or an assembly reference? error CS0234: No such name or typespace log4net\n"
                +
                "C:\\Work\\SCM\\SVN\\javaforge\\maven-csharp\\trunk\\maverick-net\\src\\main\\csharp\\Shunt\\LanguageShuntFactory.cs(47,27): error CS0246: The type or namespace name `log4net' could not be found. Are you missing a using directive or an assembly reference? error CS0234: No such name or typespace log4net\n"
                +
                "C:\\Work\\SCM\\SVN\\javaforge\\maven-csharp\\trunk\\maverick-net\\src\\main\\csharp\\Shunt\\LanguageShuntFactory.cs(67,27): error CS0246: The type or namespace name `log4net' could not be found. Are you missing a using directive or an assembly reference? error CS0234: No such name or typespace log4net\n"
                +
                "Compilation failed: 28 error(s), 0 warnings";

        List<CompilerMessage> messagesMonoWin =
            CSharpCompiler.parseCompilerOutput( new BufferedReader( new StringReader( monoWin ) ) );

        assertThat( messagesMonoWin, notNullValue() );

        assertThat( messagesMonoWin.size(), is(5) );

        assertThat( "Check that the line number is not -1",
                    messagesMonoWin.get( 0 ).getStartLine(), not(-1) );
        assertThat( "Check that the column number is not -1",
                    messagesMonoWin.get( 0 ).getStartColumn(), not(-1) );

    }

}
