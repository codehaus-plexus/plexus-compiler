package org.codehaus.plexus.compiler.javac;

import org.codehaus.plexus.compiler.CompilerConfiguration;

import junit.framework.Assert;
import junit.framework.TestCase;

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

public class JdkVersionTest
    extends TestCase
{
    public void testJdk14()
    {
        int cutoff = 4;

        for ( int i = 10; i >= 0; i-- )
        {
            String test = "1." + i;
            CompilerConfiguration config = new CompilerConfiguration();
            config.setCompilerVersion( test );
            boolean result = JavacCompiler.isPreJava14(config);
            if (i < cutoff)
            {
                Assert.assertTrue( result );
            }
            else
            {
                Assert.assertFalse( result );
            }
        }
    }

    public void testJdk16()
    {
        int cutoff = 6;

        for ( int i = 10; i >= 0; i-- )
        {
            String test = "1." + i;
            CompilerConfiguration config = new CompilerConfiguration();
            config.setCompilerVersion( test );
            boolean result = JavacCompiler.isPreJava16(config);
            if (i < cutoff)
            {
                Assert.assertTrue( result );
            }
            else
            {
                Assert.assertFalse( result );
            }
        }
    }

    public void testJdk16Source()
    {
        int cutoff = 6;

        for ( int i = 10; i >= 0; i-- )
        {
            String test = "1." + i;
            CompilerConfiguration config = new CompilerConfiguration();
            config.setSourceVersion( test );
            boolean result = JavacCompiler.isPreJava16(config);
            if (i < cutoff)
            {
                Assert.assertTrue( result );
            }
            else
            {
                Assert.assertFalse( result );
            }
        }
    }

    public void testJdk17()
    {
        int cutoff = 6;

        for ( int i = 10; i >= 0; i-- )
        {
            String test = "1." + i;
            CompilerConfiguration config = new CompilerConfiguration();
            config.setCompilerVersion( test );
            boolean result = JavacCompiler.isPreJava16(config);
            if (i < cutoff)
            {
                Assert.assertTrue( result );
            }
            else
            {
                Assert.assertFalse( result );
            }
        }
    }

    public void testJdk17Source()
    {
        int cutoff = 6;

        for ( int i = 10; i >= 0; i-- )
        {
            String test = "1." + i;
            CompilerConfiguration config = new CompilerConfiguration();
            config.setSourceVersion( test );
            boolean result = JavacCompiler.isPreJava16(config);
            if (i < cutoff)
            {
                Assert.assertTrue( result );
            }
            else
            {
                Assert.assertFalse( result );
            }
        }
    }
}
