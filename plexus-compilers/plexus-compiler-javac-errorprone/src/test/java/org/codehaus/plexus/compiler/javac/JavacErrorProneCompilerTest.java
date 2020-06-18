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

import org.codehaus.classworlds.DefaultClassRealm;
import org.codehaus.plexus.compiler.AbstractCompilerTest;

import java.lang.reflect.Field;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * @author <a href="mailto:jason@plexus.org">Jason van Zyl</a>
 */
public class JavacErrorProneCompilerTest
    extends AbstractCompilerTest
{

    protected boolean java8() {
        return System.getProperty( "java.version" ).startsWith( "1.8" );
    }
    
    private ClassLoader originalTCCL;
    
    public void setUp()
        throws Exception
    {
        super.setUp();
        if ( java8() ) {
            ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            this.originalTCCL = contextClassLoader;
            Field realmField = contextClassLoader.getClass().getDeclaredField("realm");
            realmField.setAccessible(true);
            DefaultClassRealm realm = (DefaultClassRealm) realmField.get(contextClassLoader);
            Field foreignClassLoaderField = realm.getClass().getDeclaredField("foreignClassLoader");
            foreignClassLoaderField.setAccessible(true);
            URLClassLoader foreignClassLoader = (URLClassLoader) foreignClassLoaderField.get(realm);
            Thread.currentThread().setContextClassLoader(foreignClassLoader);
        }
    }

    public void tearDown()
        throws Exception
    {
        if ( java8() ) {
            Thread.currentThread().setContextClassLoader(originalTCCL);
        }
        super.tearDown();
    }

    protected String getRoleHint()
    {
        return "javac-with-errorprone";
    }

    protected int expectedWarnings()
    {
        if ( java8() ) {
            return 1;
        }
        else
        {
            return 2;
        }
    }

    @Override
    protected int expectedErrors()
    {
        return 1;
    }

    @Override
    public String getSourceVersion()
    {
        return "1.8";
    }

    @Override
    public String getTargetVersion()
    {
        return "1.8";
    }
}
