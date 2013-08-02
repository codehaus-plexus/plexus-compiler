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

/**
 *
 * Copyright 2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

import org.codehaus.plexus.compiler.AbstractCompiler;
import org.codehaus.plexus.compiler.CompilerConfiguration;
import org.codehaus.plexus.compiler.CompilerException;
import org.codehaus.plexus.compiler.CompilerMessage;
import org.codehaus.plexus.compiler.CompilerOutputStyle;
import org.codehaus.plexus.compiler.CompilerResult;
import org.codehaus.plexus.util.StringUtils;

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
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * This class is a modified copy of JavacCompiler with modifications to use the error-prone
 * entry point into Javac.
 *
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @author <a href="mailto:matthew.pocock@ncl.ac.uk">Matthew Pocock</a>
 * @author <a href="mailto:joerg.wassmer@web.de">J&ouml;rg Wa&szlig;mer</a>
 * @author <a href="mailto:alexeagle@google.com">Alex Eagle</a>
 * @author Others
 * @plexus.component role="org.codehaus.plexus.compiler.Compiler"
 * role-hint="javac-with-errorprone"
 */
public class JavacErrorProneCompiler
    extends AbstractCompiler
{
    private static final Object LOCK = new Object();

    private static final String JAVAC_CLASSNAME = "com.google.errorprone.ErrorProneCompiler";

    public static final String ERROR_PRONE_PARENT_CLASS = "com.sun.tools.javac.main.Main";

    private static volatile Class<?> JAVAC_CLASS;

    private List<Class<?>> javaccClasses = new CopyOnWriteArrayList<Class<?>>();

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public JavacErrorProneCompiler()
    {
        super( CompilerOutputStyle.ONE_OUTPUT_FILE_PER_INPUT_FILE, ".java", ".class", null );
    }

    // ----------------------------------------------------------------------
    // Compiler Implementation
    // ----------------------------------------------------------------------

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
            getLogger().info( "Compiling " + sourceFiles.length + " " +
                                  "source file" + ( sourceFiles.length == 1 ? "" : "s" ) +
                                  " to " + destinationDir.getAbsolutePath() );
        }

        String[] args = buildCompilerArguments( config, sourceFiles );

        CompilerResult result;

        if ( config.isFork() )
        {
            throw new UnsupportedOperationException( "Cannot compile out-of-process with error-prone enabled." );
        }
        else
        {

            result = compileInProcess( args, config );

        }

        return result;
    }

    public String[] createCommandLine( CompilerConfiguration config )
        throws CompilerException
    {
        return buildCompilerArguments( config, getSourceFiles( config ) );
    }

    public static String[] buildCompilerArguments( CompilerConfiguration config, String[] sourceFiles )
    {
        List<String> args = new ArrayList<String>();

        // ----------------------------------------------------------------------
        // Set output
        // ----------------------------------------------------------------------

        File destinationDir = new File( config.getOutputLocation() );

        args.add( "-d" );

        args.add( destinationDir.getAbsolutePath() );

        // ----------------------------------------------------------------------
        // Set the class and source paths
        // ----------------------------------------------------------------------

        List<String> classpathEntries = config.getClasspathEntries();
        if ( classpathEntries != null && !classpathEntries.isEmpty() )
        {
            args.add( "-classpath" );

            args.add( getPathString( classpathEntries ) );
        }

        List<String> sourceLocations = config.getSourceLocations();
        if ( sourceLocations != null && !sourceLocations.isEmpty() )
        {
            //always pass source path, even if sourceFiles are declared,
            //needed for jsr269 annotation processing, see MCOMPILER-98
            args.add( "-sourcepath" );

            args.add( getPathString( sourceLocations ) );
        }
        args.addAll( Arrays.asList( sourceFiles ) );

        if ( !isPreJava16( config ) )
        {
            //now add jdk 1.6 annotation processing related parameters

            if ( config.getGeneratedSourcesDirectory() != null )
            {
                config.getGeneratedSourcesDirectory().mkdirs();

                args.add( "-s" );
                args.add( config.getGeneratedSourcesDirectory().getAbsolutePath() );
            }
            if ( config.getProc() != null )
            {
                args.add( "-proc:" + config.getProc() );
            }
            if ( config.getAnnotationProcessors() != null )
            {
                args.add( "-processor" );
                String[] procs = config.getAnnotationProcessors();
                StringBuilder buffer = new StringBuilder();
                for ( int i = 0; i < procs.length; i++ )
                {
                    if ( i > 0 )
                    {
                        buffer.append( "," );
                    }

                    buffer.append( procs[i] );
                }
                args.add( buffer.toString() );
            }
        }

        if ( config.isOptimize() )
        {
            args.add( "-O" );
        }

        if ( config.isDebug() )
        {
            if ( StringUtils.isNotEmpty( config.getDebugLevel() ) )
            {
                args.add( "-g:" + config.getDebugLevel() );
            }
            else
            {
                args.add( "-g" );
            }
        }

        if ( config.isVerbose() )
        {
            args.add( "-verbose" );
        }

        if ( config.isShowDeprecation() )
        {
            args.add( "-deprecation" );

            // This is required to actually display the deprecation messages
            config.setShowWarnings( true );
        }

        if ( !config.isShowWarnings() )
        {
            args.add( "-nowarn" );
        }

        // TODO: this could be much improved
        if ( StringUtils.isEmpty( config.getTargetVersion() ) )
        {
            // Required, or it defaults to the target of your JDK (eg 1.5)
            args.add( "-target" );
            args.add( "1.1" );
        }
        else
        {
            args.add( "-target" );
            args.add( config.getTargetVersion() );
        }

        if ( !suppressSource( config ) && StringUtils.isEmpty( config.getSourceVersion() ) )
        {
            // If omitted, later JDKs complain about a 1.1 target
            args.add( "-source" );
            args.add( "1.3" );
        }
        else if ( !suppressSource( config ) )
        {
            args.add( "-source" );
            args.add( config.getSourceVersion() );
        }

        if ( !suppressEncoding( config ) && !StringUtils.isEmpty( config.getSourceEncoding() ) )
        {
            args.add( "-encoding" );
            args.add( config.getSourceEncoding() );
        }

        for ( Map.Entry<String, String> entry : config.getCustomCompilerArgumentsAsMap().entrySet() )
        {
            String key = entry.getKey();

            if ( StringUtils.isEmpty( key ) || key.startsWith( "-J" ) )
            {
                continue;
            }

            args.add( key );

            String value = entry.getValue();

            if ( StringUtils.isEmpty( value ) )
            {
                continue;
            }

            args.add( value );
        }

        return args.toArray( new String[args.size()] );
    }

    /**
     * Determine if the compiler is a version prior to 1.4.
     * This is needed as 1.3 and earlier did not support -source or -encoding parameters
     *
     * @param config The compiler configuration to test.
     * @return true if the compiler configuration represents a Java 1.4 compiler or later, false otherwise
     */
    private static boolean isPreJava14( CompilerConfiguration config )
    {
        String v = config.getCompilerVersion();

        if ( v == null )
        {
            return false;
        }

        return v.startsWith( "1.3" ) || v.startsWith( "1.2" ) || v.startsWith( "1.1" ) || v.startsWith( "1.0" );
    }

    /**
     * Determine if the compiler is a version prior to 1.6.
     * This is needed for annotation processing parameters.
     *
     * @param config The compiler configuration to test.
     * @return true if the compiler configuration represents a Java 1.6 compiler or later, false otherwise
     */
    private static boolean isPreJava16( CompilerConfiguration config )
    {
        String v = config.getCompilerVersion();

        if ( v == null )
        {
            //mkleint: i haven't completely understood the reason for the
            //compiler version parameter, checking source as well, as most projects will have this one set, not the compiler
            String s = config.getSourceVersion();
            if ( s == null )
            {
                //now return true, as the 1.6 version is not the default - 1.4 is.
                return true;
            }
            return s.startsWith( "1.5" ) || s.startsWith( "1.4" ) || s.startsWith( "1.3" ) || s.startsWith( "1.2" )
                || s.startsWith( "1.1" ) || s.startsWith( "1.0" );
        }

        return v.startsWith( "1.5" ) || v.startsWith( "1.4" ) || v.startsWith( "1.3" ) || v.startsWith( "1.2" )
            || v.startsWith( "1.1" ) || v.startsWith( "1.0" );
    }


    private static boolean suppressSource( CompilerConfiguration config )
    {
        return isPreJava14( config );
    }

    private static boolean suppressEncoding( CompilerConfiguration config )
    {
        return isPreJava14( config );
    }

    /**
     * Compile the java sources in the current JVM, without calling an external executable,
     * using <code>com.sun.tools.javac.Main</code> class
     *
     * @param args   arguments for the compiler as they would be used in the command line javac
     * @param config compiler configuration
     * @return a CompilerResult object encapsulating the result of the compilation and any compiler messages
     * @throws org.codehaus.plexus.compiler.CompilerException
     *
     */
    CompilerResult compileInProcess( String[] args, CompilerConfiguration config )
        throws CompilerException
    {
        final Class<?> javacClass = getJavacClass( config );
        final Thread thread = Thread.currentThread();
        final ClassLoader contextClassLoader = thread.getContextClassLoader();
        thread.setContextClassLoader( javacClass.getClassLoader() );
        try
        {
            return compileWithErrorProne( javacClass, args );
        }
        finally
        {
            releaseJavaccClass( javacClass, config );
            thread.setContextClassLoader( contextClassLoader );
        }
    }

    private static CompilerResult compileWithErrorProne( Class<?> errorProneCompilerClass, String[] args )
        throws CompilerException
    {
        // TODO(alexeagle): perhaps error-prone can conform to the 1.6 JavaCompiler API.
        // Then we could use the JavaxToolsCompiler approach instead, which would reuse more code.

        try
        {
            Method compile =
                errorProneCompilerClass.getMethod( "compile", new Class[]{ DiagnosticListener.class, String[].class } );
            return getCompilerResult( compile, args );
        }
        catch ( NoSuchMethodException e )
        {
            throw new CompilerException( "Couldn't find error-prone compile method", e );
        }
        catch ( IllegalAccessException e )
        {
            throw new CompilerException( "", e );
        }
        catch ( InvocationTargetException e )
        {
            throw new CompilerException( "", e );
        }

    }

    private static CompilerResult getCompilerResult( Method compile, String[] args )
        throws InvocationTargetException, IllegalAccessException
    {

        final List<CompilerMessage> messages = new ArrayList<CompilerMessage>();
        DiagnosticListener<? super JavaFileObject> listener = new DiagnosticListener<JavaFileObject>()
        {
            public void report( Diagnostic<? extends JavaFileObject> diagnostic )
            {
                messages.add(
                    new CompilerMessage( diagnostic.getSource().getName(), convertKind( diagnostic.getKind() ),
                                         (int) diagnostic.getLineNumber(), (int) diagnostic.getColumnNumber(), -1, -1,
                                         // end pos line:column is hard to calculate
                                         diagnostic.getMessage( Locale.getDefault() ) ) );
            }
        };
        int result = (Integer) compile.invoke( null, new Object[]{ listener, args } );

        return new CompilerResult( result == 0, messages );
    }

    private static CompilerMessage.Kind convertKind( Diagnostic.Kind kind )
    {
        switch ( kind )
        {
            case ERROR:
                return CompilerMessage.Kind.ERROR;
            case MANDATORY_WARNING:
                return CompilerMessage.Kind.MANDATORY_WARNING;
            case NOTE:
                return CompilerMessage.Kind.NOTE;
            case WARNING:
                return CompilerMessage.Kind.WARNING;
            case OTHER:
                return CompilerMessage.Kind.OTHER;
            default:
                return CompilerMessage.Kind.OTHER;
        }
    }

    private void releaseJavaccClass( Class<?> javaccClass, CompilerConfiguration compilerConfiguration )
    {
        if ( compilerConfiguration.getCompilerReuseStrategy()
            == CompilerConfiguration.CompilerReuseStrategy.ReuseCreated )
        {
            javaccClasses.add( javaccClass );
        }

    }

    /**
     * Find the main class of JavaC. Return the same class for subsequent calls.
     *
     * @return the non-null class.
     * @throws org.codehaus.plexus.compiler.CompilerException
     *          if the class has not been found.
     */
    private Class<?> getJavacClass( CompilerConfiguration compilerConfiguration )
        throws CompilerException
    {
        Class<?> c = null;
        switch ( compilerConfiguration.getCompilerReuseStrategy() )
        {
            case AlwaysNew:
                return createJavacClass();
            case ReuseCreated:
                synchronized ( javaccClasses )
                {
                    if ( javaccClasses.size() > 0 )
                    {
                        c = javaccClasses.get( 0 );
                        javaccClasses.remove( c );
                        return c;
                    }
                }
                c = createJavacClass();
                return c;
            case ReuseSame:
            default:
                c = JAVAC_CLASS;
                if ( c != null )
                {
                    return c;
                }
                synchronized ( LOCK )
                {
                    if ( c == null )
                    {
                        JAVAC_CLASS = c = createJavacClass();
                    }
                    return c;
                }


        }
    }


    /**
     * Helper method for create Javac class
     */
    private Class<?> createJavacClass()
        throws CompilerException
    {
        try
        {
            return JavacErrorProneCompiler.class.getClassLoader().loadClass( JAVAC_CLASSNAME );
        }
        catch ( ClassNotFoundException ex )
        {
            System.err.println( "Could not find javac in JavacCompiler classloader, falling back" );
            // ok
        }
        try
        {
            // Classloader subtlety: if we try to load the errorprone class, but its parent class
            // isn't on the classpath, we get a ClassNotFoundException from the wrong classloader
            // that doesn't match the catch block.
            // look whether error-prone's parent class is on Maven's classpath
            return JavacErrorProneCompiler.class.getClassLoader().loadClass( ERROR_PRONE_PARENT_CLASS );
        }
        catch ( ClassNotFoundException ex )
        {
            System.err.println( "Could not find javac in JavacCompiler classloader, falling back" );
            // ok
        }

        final File toolsJar = new File( System.getProperty( "java.home" ), "../lib/tools.jar" );
        if ( !toolsJar.exists() )
        {
            throw new CompilerException( "tools.jar not found: " + toolsJar );
        }

        try
        {
            // Combined classloader with no parent/child relationship, so classes in our classloader
            // can reference classes in tools.jar
            URL[] originalUrls = ( (URLClassLoader) JavacErrorProneCompiler.class.getClassLoader() ).getURLs();
            URL[] urls = new URL[originalUrls.length + 1];
            urls[0] = toolsJar.toURI().toURL();
            System.arraycopy( originalUrls, 0, urls, 1, originalUrls.length );
            ClassLoader javacClassLoader = new URLClassLoader( urls );

            final Thread thread = Thread.currentThread();
            final ClassLoader contextClassLoader = thread.getContextClassLoader();
            thread.setContextClassLoader( javacClassLoader );
            try
            {
                //return Class.forName( JavacCompiler.JAVAC_CLASSNAME, true, javacClassLoader );\
                javacClassLoader.loadClass( ERROR_PRONE_PARENT_CLASS );
                return javacClassLoader.loadClass( JAVAC_CLASSNAME );
            }
            finally
            {
                thread.setContextClassLoader( contextClassLoader );
            }
        }
        catch ( MalformedURLException ex )
        {
            throw new CompilerException(
                "Could not convert the file reference to tools.jar to a URL, path to tools.jar: '"
                    + toolsJar.getAbsolutePath() + "'.", ex );
        }
        catch ( ClassNotFoundException ex )
        {
            if ( ex.getMessage().contains( JAVAC_CLASSNAME ) )
            {
                throw new CompilerException( "Unable to locate the error-prone library on the classpath." + EOL
                                                 + "Make sure you have the error_prone_core library included as "
                                                 + "a dependency in the <plugin><dependencies> for the "
                                                 + "maven-compiler-plugin.", ex );
            }
            throw new CompilerException( "Unable to locate the Javac Compiler in:" + EOL + "  " + toolsJar + EOL
                                             + "Please ensure you are using JDK 1.4 or above and" + EOL
                                             + "not a JRE (the com.sun.tools.javac.main.Main class is required)." + EOL
                                             + "In most cases you can change the location of your Java" + EOL
                                             + "installation by setting the JAVA_HOME environment variable.", ex );
        }
    }

}
