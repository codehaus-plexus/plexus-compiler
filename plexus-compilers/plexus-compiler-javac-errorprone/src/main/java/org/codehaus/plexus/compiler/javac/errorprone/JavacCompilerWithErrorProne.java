/*
 * Copyright 2011 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.codehaus.plexus.compiler.javac.errorprone;

import com.google.errorprone.ErrorProneJavaCompiler;

import org.codehaus.plexus.compiler.Compiler;
import org.codehaus.plexus.compiler.CompilerConfiguration;
import org.codehaus.plexus.compiler.CompilerException;
import org.codehaus.plexus.compiler.CompilerMessage;
import org.codehaus.plexus.compiler.CompilerResult;
import org.codehaus.plexus.compiler.javac.InProcessCompiler;
import org.codehaus.plexus.compiler.javac.JavacCompiler;
import org.codehaus.plexus.compiler.javac.JavaxToolsCompiler;
import org.codehaus.plexus.component.annotations.Component;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * This class overrides JavacCompiler with modifications to use the error-prone
 * entry point into Javac.
 *
 * @author <a href="mailto:alexeagle@google.com">Alex Eagle</a>
 */
@Component( role = Compiler.class, hint = "javac-with-errorprone")
public class JavacCompilerWithErrorProne
    extends JavacCompiler
{
    private static class NonDelegatingClassLoader
        extends URLClassLoader
    {
        ClassLoader original;

        public NonDelegatingClassLoader( URL[] urls, ClassLoader original )
            throws MalformedURLException
        {
            super( urls, null );
            this.original = original;
        }

        @Override
        public Class<?> loadClass( String name, boolean complete )
            throws ClassNotFoundException
        {
            // Classes loaded inside CompilerInvoker that need to reach back to the caller
            if ( name.contentEquals( CompilerResult.class.getName() )
                || name.contentEquals( InProcessCompiler.class.getName() )
                || name.contentEquals( CompilerConfiguration.class.getName() )
                || name.contentEquals( CompilerConfiguration.CompilerReuseStrategy.class.getName() )
                || name.contentEquals( CompilerException.class.getName() )
                || name.contentEquals( CompilerMessage.class.getName() )
                || name.contentEquals( CompilerMessage.Kind.class.getName() ) )
            {
                return original.loadClass( name );
            }

            try
            {
                synchronized ( getClassLoadingLock( name ) )
                {
                    Class c = findLoadedClass( name );
                    if ( c != null )
                    {
                        return c;
                    }
                    return findClass( name );
                }
            }
            catch ( ClassNotFoundException e )
            {
                return super.loadClass( name, complete );
            }
        }
    }

    protected InProcessCompiler inProcessCompiler()
    {
        if ( Thread.currentThread().getContextClassLoader().getResource("java/lang/module/ModuleReference.class") == null )
        {
            ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            URL[] urls = ( (URLClassLoader) contextClassLoader ).getURLs();
            ClassLoader loader;
            try
            {
                loader = new NonDelegatingClassLoader( urls, contextClassLoader );
                Class<?> clazz = Class.forName( CompilerInvoker.class.getName(), true, loader );
                return ( InProcessCompiler ) clazz.newInstance();
            }
            catch ( Exception e )
            {
                throw new IllegalStateException( e );
            }
        }
        return new CompilerInvoker();
    }

    /**
     * A wrapper for all of the error-prone specific classes. Loading this class with a
     * non-delegating classloader ensures that error-prone's javac loads javax.tools.* classes from
     * javac.jar instead of from the bootclasspath.
     */
    public static class CompilerInvoker
    	extends JavaxToolsCompiler
    {
    	@Override
    	protected JavaCompiler newJavaCompiler()
    	{
    		return new ErrorProneJavaCompiler();
    	}
    }
}
