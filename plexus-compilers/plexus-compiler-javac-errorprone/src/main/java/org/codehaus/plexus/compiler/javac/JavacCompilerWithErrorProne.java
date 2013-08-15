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

package org.codehaus.plexus.compiler.javac;


import org.codehaus.classworlds.ClassRealm;
import org.codehaus.classworlds.ClassWorld;
import org.codehaus.classworlds.DuplicateRealmException;
import org.codehaus.classworlds.NoSuchRealmException;
import org.codehaus.plexus.compiler.CompilerConfiguration;
import org.codehaus.plexus.compiler.CompilerException;
import org.codehaus.plexus.compiler.CompilerMessage;
import org.codehaus.plexus.compiler.CompilerResult;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * This class overrides JavacCompiler with modifications to use the error-prone
 * entry point into Javac.
 *
 * @author <a href="mailto:alexeagle@google.com">Alex Eagle</a>
 * @plexus.component role="org.codehaus.plexus.compiler.Compiler"
 * role-hint="javac-with-errorprone"
 */
public class JavacCompilerWithErrorProne
    extends JavacCompiler
{

    private ClassWorld classWorld = new ClassWorld();

    private static final String REALM_ID = "error-prone";

    @Override
    CompilerResult compileOutOfProcess( CompilerConfiguration config, String executable, String[] args )
        throws CompilerException
    {
        throw new UnsupportedOperationException( "Cannot compile out-of-process with error-prone" );
    }

    protected ClassLoader getErrorProneCompilerClassLoader()
        throws MalformedURLException, NoSuchRealmException
    {

        try
        {
            final String javaHome = System.getProperty( "java.home" );
            final File toolsJar = new File( javaHome, "../lib/tools.jar" );

            ClassLoader tccl = Thread.currentThread().getContextClassLoader();

            getLogger().debug( "javaHome:" + javaHome );

            getLogger().debug( "toolsJar:" + toolsJar.getPath() );
            URL[] originalUrls = ( (URLClassLoader) tccl ).getURLs();
            URL[] urls = new URL[originalUrls.length + 1];
            urls[0] = toolsJar.toURI().toURL();
            System.arraycopy( originalUrls, 0, urls, 1, originalUrls.length );

            ClassRealm urlClassLoader = classWorld.newRealm( REALM_ID );
            for ( URL url : urls )
            {
                urlClassLoader.addConstituent( url );
            }

            getLogger().debug( "urls:" + Arrays.asList( urls ) );
            return urlClassLoader.getClassLoader();
        }
        catch ( DuplicateRealmException e )
        {
            if ( REALM_ID.equals( e.getId() ) )
            {
                return classWorld.getRealm( REALM_ID ).getClassLoader();
            }
            // should not happen ...
            throw new NoSuchRealmException( classWorld, REALM_ID );
        }


    }

    @Override
    protected Class<?> createJavacClass()
        throws CompilerException
    {
        try
        {
            return getErrorProneCompilerClassLoader().loadClass( "com.google.errorprone.ErrorProneCompiler$Builder" );
        }
        catch ( ClassNotFoundException e )
        {
            throw new CompilerException( e.getMessage(), e );
        }
        catch ( MalformedURLException e )
        {
            throw new CompilerException( e.getMessage(), e );
        }
        catch ( NoSuchRealmException e )
        {
            throw new CompilerException( e.getMessage(), e );
        }
    }

    @Override
    CompilerResult compileInProcessWithProperClassloader( Class<?> javacClass, String[] args )
        throws CompilerException
    {

        ClassLoader original = Thread.currentThread().getContextClassLoader();

        try
        {

            // TODO(alexeagle): perhaps error-prone can conform to the 1.6 JavaCompiler API.
            // Then we could use the JavaxToolsCompiler approach instead, which would reuse more code.
            // olamy: except we have a bunch of issues with classloader hierarchy

            final List<CompilerMessage> messages = new ArrayList<CompilerMessage>();
            DiagnosticListener<? super JavaFileObject> listener = new DiagnosticListener<JavaFileObject>()
            {
                public void report( Diagnostic<? extends JavaFileObject> diagnostic )
                {
                    CompilerMessage compilerMessage =
                        new CompilerMessage( diagnostic.getSource() == null ? null : diagnostic.getSource().getName(),
                                             JavaxToolsCompiler.convertKind( diagnostic ),
                                             (int) diagnostic.getLineNumber(), (int) diagnostic.getColumnNumber(), -1,
                                             -1,// end pos line:column is hard to calculate
                                             diagnostic.getMessage( Locale.getDefault() ) );

                    messages.add( compilerMessage );
                }
            };
            /*
            int result = new com.google.errorprone.ErrorProneCompiler.Builder().listenToDiagnostics( listener ).build().compile( args );

            return new CompilerResult( result == 0, messages );
            */

            // here we need to use the new classloader containing tools.jar and use reflection with classworld class loader
            // whereas default jdk classloader will try to load com.sun.* classes from the plugin classloader!!

            Thread.currentThread().setContextClassLoader( getErrorProneCompilerClassLoader() );

            Object builderInstance = javacClass.newInstance();

            Method listenToDiagnostics = javacClass.getMethod( "listenToDiagnostics", DiagnosticListener.class );

            builderInstance = listenToDiagnostics.invoke( builderInstance, listener );

            Method build = javacClass.getMethod( "build" );
            Object compiler = build.invoke( builderInstance );
            Method compile = compiler.getClass().getMethod( "compile", new Class[]{ String[].class } );
            int result = (Integer) compile.invoke( compiler, new Object[]{ args } );
            return new CompilerResult( result == 0, messages );
        }
        catch ( InvocationTargetException e )
        {
            throw new CompilerException( e.getMessage(), e );
        }
        catch ( NoSuchMethodException e )
        {
            throw new CompilerException( e.getMessage(), e );
        }
        catch ( InstantiationException e )
        {
            throw new CompilerException( e.getMessage(), e );
        }
        catch ( IllegalAccessException e )
        {
            throw new CompilerException( e.getMessage(), e );
        }
        catch ( MalformedURLException e )
        {
            throw new CompilerException( e.getMessage(), e );
        }
        catch ( NoSuchRealmException e )
        {
            throw new CompilerException( e.getMessage(), e );
        }

        finally
        {
            Thread.currentThread().setContextClassLoader( original );
        }
    }
}
