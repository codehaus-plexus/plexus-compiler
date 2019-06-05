package org.codehaus.plexus.compiler.jikes;

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

import java.util.Arrays;
import java.util.Collection;

import org.codehaus.plexus.compiler.AbstractCompilerTest;

/**
 * @author <a href="mailto:jason@plexus.org">Jason van Zyl</a>
 */
public class JikesCompilerTest
    extends AbstractCompilerTest
{
    protected String getRoleHint()
    {
        return "jikes";
    }

    public void setUp()
        throws Exception
    {
        super.setUp();

        setCompilerDebug( true );
        setCompilerDeprecationWarnings( true );
    }

    protected int expectedErrors()
    {
        return 2;
    }

    protected int expectedWarnings()
    {
        return 3;
    }

    protected Collection<String> expectedOutputFiles()
    {
        return Arrays.asList( "org/codehaus/foo/Deprecation.class", "org/codehaus/foo/ExternalDeps.class",
                "org/codehaus/foo/Person.class", "org/codehaus/foo/ReservedWord.class",
                "org/codehaus/foo/RightClassname.class" );
    }

}
