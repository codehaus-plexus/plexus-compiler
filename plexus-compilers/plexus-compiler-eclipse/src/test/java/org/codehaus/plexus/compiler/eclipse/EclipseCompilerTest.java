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

import org.codehaus.plexus.compiler.AbstractCompilerTest;
import org.codehaus.plexus.compiler.Compiler;
import org.codehaus.plexus.compiler.CompilerConfiguration;

import java.util.Arrays;
import java.util.Collection;

/**
 * @author <a href="mailto:jason@plexus.org">Jason van Zyl</a>
 */
public class EclipseCompilerTest
    extends AbstractCompilerTest
{

    public void setUp()
        throws Exception
    {
        super.setUp();

        setCompilerDebug( true );
        setCompilerDeprecationWarnings( true );
    }

    protected String getRoleHint()
    {
        return "eclipse";
    }

    protected int expectedErrors()
    {
        return 4;
    }

    protected int expectedWarnings()
    {
        return 2;
    }

    protected Collection<String> expectedOutputFiles()
    {
        return Arrays.asList( new String[] { "org/codehaus/foo/Deprecation.class", "org/codehaus/foo/ExternalDeps.class",
            "org/codehaus/foo/Person.class", "org/codehaus/foo/ReservedWord.class" } );
    }

    // The test is fairly meaningless as we can not validate anything
    public void testCustomArgument()
        throws Exception
    {
        org.codehaus.plexus.compiler.Compiler compiler = (Compiler) lookup( Compiler.ROLE, getRoleHint() );

        CompilerConfiguration compilerConfig = createMinimalCompilerConfig();

        compilerConfig.addCompilerCustomArgument( "-key", "value" );

        compiler.performCompile( compilerConfig );
    }

    public void testInitializeWarningsForPropertiesArgument()
        throws Exception
    {
        org.codehaus.plexus.compiler.Compiler compiler = (Compiler) lookup( Compiler.ROLE, getRoleHint() );

        CompilerConfiguration compilerConfig = createMinimalCompilerConfig();

        compilerConfig.addCompilerCustomArgument( "-properties", "file_does_not_exist" );

        try
        {
            compiler.performCompile( compilerConfig );
            fail( "looking up the properties file should have thrown an exception" );
        }
        catch ( IllegalArgumentException e )
        {
            assertTrue("Message must start with 'Properties file'", e.getMessage().startsWith("Properties file"));
        }
    }

    private CompilerConfiguration createMinimalCompilerConfig()
    {
        CompilerConfiguration compilerConfig = new CompilerConfiguration();
        compilerConfig.setOutputLocation( getBasedir() + "/target/" + getRoleHint() + "/classes-CustomArgument" );
        return compilerConfig;
    }

}
