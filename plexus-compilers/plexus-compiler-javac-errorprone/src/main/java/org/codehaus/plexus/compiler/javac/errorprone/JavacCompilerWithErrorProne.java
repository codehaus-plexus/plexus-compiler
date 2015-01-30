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

import com.google.errorprone.ErrorProneCompiler;
import org.codehaus.plexus.compiler.AbstractCompiler;
import org.codehaus.plexus.compiler.CompilerConfiguration;
import org.codehaus.plexus.compiler.CompilerException;
import org.codehaus.plexus.compiler.CompilerMessage;
import org.codehaus.plexus.compiler.CompilerOutputStyle;
import org.codehaus.plexus.compiler.CompilerResult;
import org.codehaus.plexus.compiler.javac.JavacCompiler;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import java.io.File;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * This class overrides JavacCompiler with modifications to use the error-prone
 * entry point into Javac.
 *
 * @author <a href="mailto:alexeagle@google.com">Alex Eagle</a>
 * @plexus.component role="org.codehaus.plexus.compiler.Compiler" role-hint="javac-with-errorprone"
 */
public class JavacCompilerWithErrorProne
    extends AbstractCompiler
{
    public JavacCompilerWithErrorProne()
    {
        super( CompilerOutputStyle.ONE_OUTPUT_FILE_PER_INPUT_FILE, ".java", ".class", null );
    }

    public String[] createCommandLine( CompilerConfiguration config )
        throws CompilerException
    {
        return new String[0];
    }

    @Override
    public CompilerResult performCompile( CompilerConfiguration config )
        throws CompilerException
    {
        File destinationDir = new File( config.getOutputLocation() );

        if ( !destinationDir.exists() )
        {
            destinationDir.mkdirs();
        }

        String[] sourceFiles = getSourceFiles( config );

        if ( ( sourceFiles == null ) || ( sourceFiles.length == 0 ) )
        {
            return new CompilerResult();
        }

        if ( ( getLogger() != null ) && getLogger().isInfoEnabled() )
        {
            getLogger().info( "Compiling " + sourceFiles.length + " " //
                                  + "source file" //
                                  + ( sourceFiles.length == 1 ? "" : "s" ) //
                                  + " to " + destinationDir.getAbsolutePath() );
        }

        String[] args = JavacCompiler.buildCompilerArguments( config, sourceFiles );

        try
        {
            return (CompilerResult) getInvoker().invoke( null, new Object[]{ args } );
        }
        catch ( Exception e )
        {
            throw new CompilerException( e.getMessage(), e );
        }
    }

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
            if ( name.contentEquals( CompilerResult.class.getName() ) )
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

    private Method invokerMethod;

    private Method getInvoker()
        throws CompilerException
    {
        if ( invokerMethod == null )
        {
            ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            URL[] urls = ( (URLClassLoader) contextClassLoader ).getURLs();
            ClassLoader loader;
            try
            {
                loader = new NonDelegatingClassLoader( urls, contextClassLoader );
                Class<?> clazz = Class.forName( CompilerInvoker.class.getName(), true, loader );
                invokerMethod = clazz.getMethod( "compile", String[].class );
            }
            catch ( Exception e )
            {
                throw new CompilerException( e.getMessage(), e );
            }
        }
        return invokerMethod;
    }

    /**
     * A wrapper for all of the error-prone specific classes. Loading this class with a
     * non-delegating classloader ensures that error-prone's javac loads javax.tools.* classes from
     * javac.jar instead of from the bootclasspath.
     */
    public static class CompilerInvoker
    {
        private static class MessageListener
            implements DiagnosticListener<JavaFileObject>
        {
            private final List<CompilerMessage> messages;

            MessageListener( List<CompilerMessage> messages )
            {
                this.messages = messages;
            }

            public static CompilerMessage.Kind convertKind( Diagnostic<? extends JavaFileObject> diagnostic )
            {
                switch ( diagnostic.getKind() )
                {
                    case ERROR:
                        return CompilerMessage.Kind.ERROR;
                    case WARNING:
                        return CompilerMessage.Kind.WARNING;
                    case MANDATORY_WARNING:
                        return CompilerMessage.Kind.MANDATORY_WARNING;
                    case NOTE:
                        return CompilerMessage.Kind.NOTE;
                    default:
                        return CompilerMessage.Kind.OTHER;
                }
            }

            public void report( Diagnostic<? extends JavaFileObject> diagnostic )
            {
                CompilerMessage compilerMessage =
                    new CompilerMessage( diagnostic.getSource() == null ? null : diagnostic.getSource().getName(), //
                                         convertKind( diagnostic ), //
                                         (int) diagnostic.getLineNumber(), //
                                         (int) diagnostic.getColumnNumber(), //
                                         -1, //
                                         -1,
                                         // end pos line:column is hard to calculate
                                         diagnostic.getMessage( Locale.getDefault() ) );
                messages.add( compilerMessage );
            }
        }

        public static CompilerResult compile( String[] args )
        {
            List<CompilerMessage> messages = new ArrayList<CompilerMessage>();
            ErrorProneCompiler compiler = //
                new ErrorProneCompiler.Builder() //
                    .listenToDiagnostics( new MessageListener( messages ) ) //
                    .build();
            return new CompilerResult( compiler.compile( args ).isOK(), messages );
        }
    }
}