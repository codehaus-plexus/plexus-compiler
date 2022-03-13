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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.concurrent.CopyOnWriteArrayList;

import org.codehaus.plexus.compiler.AbstractCompiler;
import org.codehaus.plexus.compiler.Compiler;
import org.codehaus.plexus.compiler.CompilerConfiguration;
import org.codehaus.plexus.compiler.CompilerException;
import org.codehaus.plexus.compiler.CompilerMessage;
import org.codehaus.plexus.compiler.CompilerOutputStyle;
import org.codehaus.plexus.compiler.CompilerResult;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.Os;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;

/**
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @author <a href="mailto:matthew.pocock@ncl.ac.uk">Matthew Pocock</a>
 * @author <a href="mailto:joerg.wassmer@web.de">J&ouml;rg Wa&szlig;mer</a>
 * @author Others
 *
 */
@Component( role = Compiler.class, hint = "javac ")
public class JavacCompiler
    extends AbstractCompiler
{

    // see compiler.warn.warning in compiler.properties of javac sources
    private static final String[] WARNING_PREFIXES = { "warning: ", "\u8b66\u544a: ", "\u8b66\u544a\uff1a " };

    // see compiler.note.note in compiler.properties of javac sources
    private static final String[] NOTE_PREFIXES = { "Note: ", "\u6ce8: ", "\u6ce8\u610f\uff1a " };

    // see compiler.misc.verbose in compiler.properties of javac sources
    private static final String[] MISC_PREFIXES = { "[" };

    private static final Object LOCK = new Object();

    private static final String JAVAC_CLASSNAME = "com.sun.tools.javac.Main";

    private static volatile Class<?> JAVAC_CLASS;

    private final List<Class<?>> javaccClasses = new CopyOnWriteArrayList<>();

    @Requirement
    private InProcessCompiler inProcessCompiler;

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public JavacCompiler()
    {
        super( CompilerOutputStyle.ONE_OUTPUT_FILE_PER_INPUT_FILE, ".java", ".class", null );
    }

    // ----------------------------------------------------------------------
    // Compiler Implementation
    // ----------------------------------------------------------------------

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
            getLogger().info( "Compiling " + sourceFiles.length + " " +
                                  "source file" + ( sourceFiles.length == 1 ? "" : "s" ) +
                                  " to " + destinationDir.getAbsolutePath() );
        }

        String[] args = buildCompilerArguments( config, sourceFiles );

        CompilerResult result;

        if ( config.isFork() )
        {
            String executable = config.getExecutable();

            if ( StringUtils.isEmpty( executable ) )
            {
                try
                {
                    executable = getJavacExecutable();
                }
                catch ( IOException e )
                {
                    if ( (getLogger() != null ) && getLogger().isWarnEnabled()) {
                        getLogger().warn( "Unable to autodetect 'javac' path, using 'javac' from the environment." );
                    }
                    executable = "javac";
                }
            }

            result = compileOutOfProcess( config, executable, args );
        }
        else
        {
            if ( isJava16() && !config.isForceJavacCompilerUse() )
            {
                // use fqcn to prevent loading of the class on 1.5 environment !
                result =
                    inProcessCompiler().compileInProcess( args, config, sourceFiles );
            }
            else
            {
                result = compileInProcess( args, config );
            }
        }

        return result;
    }

    protected InProcessCompiler inProcessCompiler()
    {
        return inProcessCompiler;
    }

    protected static boolean isJava16()
    {
        try
        {
            Thread.currentThread().getContextClassLoader().loadClass( "javax.tools.ToolProvider" );
            return true;
        }
        catch ( Exception e )
        {
            return false;
        }
    }

    public String[] createCommandLine( CompilerConfiguration config )
        throws CompilerException
    {
        return buildCompilerArguments( config, getSourceFiles( config ) );
    }

    public static String[] buildCompilerArguments( CompilerConfiguration config, String[] sourceFiles )
    {
        List<String> args = new ArrayList<>();

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

        List<String> modulepathEntries = config.getModulepathEntries();
        if ( modulepathEntries != null && !modulepathEntries.isEmpty() )
        {
            args.add( "--module-path" );

            args.add( getPathString( modulepathEntries ) );
        }

        List<String> sourceLocations = config.getSourceLocations();
        if ( sourceLocations != null && !sourceLocations.isEmpty() )
        {
            //always pass source path, even if sourceFiles are declared,
            //needed for jsr269 annotation processing, see MCOMPILER-98
            args.add( "-sourcepath" );

            args.add( getPathString( sourceLocations ) );
        }
        if ( !isJava16() || config.isForceJavacCompilerUse() || config.isFork() )
        {
            args.addAll( Arrays.asList( sourceFiles ) );
        }

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
            if ( config.getProcessorPathEntries() != null && !config.getProcessorPathEntries().isEmpty() ) 
            {
                args.add( "-processorpath" );
                args.add( getPathString( config.getProcessorPathEntries() ) );
            }
            if ( config.getProcessorModulePathEntries() != null && !config.getProcessorModulePathEntries().isEmpty() ) 
            {
                args.add( "--processor-module-path" );
                args.add( getPathString( config.getProcessorModulePathEntries() ) );
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

        if ( !isPreJava18(config) && config.isParameters() )
        {
            args.add( "-parameters" );
        }

        if ( config.isEnablePreview() )
        {
            args.add( "--enable-preview" );
        }

        if ( config.getImplicitOption() != null )
        {
            args.add( "-implicit:" + config.getImplicitOption() );
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
        else
        {
            String warnings = config.getWarnings();
            if (config.isShowLint())
            {
                if(config.isShowWarnings() && StringUtils.isNotEmpty(warnings))
                {
                    args.add( "-Xlint:" + warnings );
                }
                else
                {
                    args.add( "-Xlint" );
                }
            }
        }

        if ( config.isFailOnWarning() )
        {
            args.add( "-Werror" );
        }

        if ( !StringUtils.isEmpty( config.getReleaseVersion() ) )
        {
            args.add( "--release" );
            args.add( config.getReleaseVersion() );
        }
        else
        {
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
        }


        if ( !suppressEncoding( config ) && !StringUtils.isEmpty( config.getSourceEncoding() ) )
        {
            args.add( "-encoding" );
            args.add( config.getSourceEncoding() );
        }

        if ( !StringUtils.isEmpty( config.getModuleVersion() ) )
        {
            args.add( "--module-version" );
            args.add( config.getModuleVersion() );
        }

        for ( Map.Entry<String, String> entry : config.getCustomCompilerArgumentsEntries() )
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

        return args.toArray( new String[0] );
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
        String v = config.getReleaseVersion();

        if ( v == null )
        {
            v = config.getCompilerVersion();
        }

        if ( v == null )
        {
            v = config.getSourceVersion();
        }

        if ( v == null )
        {
            return true;
        }

        return v.startsWith( "5" ) || v.startsWith( "1.5" ) || v.startsWith( "1.4" ) || v.startsWith( "1.3" ) || v.startsWith( "1.2" )
            || v.startsWith( "1.1" ) || v.startsWith( "1.0" );
    }

    private static boolean isPreJava18( CompilerConfiguration config )
    {
        String v = config.getReleaseVersion();

        if ( v == null )
        {
            v = config.getCompilerVersion();
        }

        if ( v == null )
        {
            v = config.getSourceVersion();
        }

        if ( v == null )
        {
            return true;
        }

        return v.startsWith( "7" ) || v.startsWith( "1.7" ) || v.startsWith( "6" ) ||v.startsWith( "1.6" ) || v.startsWith( "1.5" ) || v.startsWith( "1.4" )
                || v.startsWith( "1.3" ) || v.startsWith( "1.2" ) || v.startsWith( "1.1" ) || v.startsWith( "1.0" );
    }

    private static boolean isPreJava9( CompilerConfiguration config )
    {

        String v = config.getReleaseVersion();

        if ( v == null )
        {
            v = config.getCompilerVersion();
        }

        if ( v == null )
        {
            v = config.getSourceVersion();
        }

        if ( v == null )
        {
            return true;
        }

        return v.startsWith( "8" )  || v.startsWith( "1.8" )  || v.startsWith( "7" ) || v.startsWith( "1.7" ) || v.startsWith( "1.6" ) || v.startsWith( "1.5" ) || v.startsWith( "1.4" )
                || v.startsWith( "1.3" ) || v.startsWith( "1.2" ) || v.startsWith( "1.1" ) || v.startsWith( "1.0" );
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
     * Compile the java sources in a external process, calling an external executable,
     * like javac.
     *
     * @param config     compiler configuration
     * @param executable name of the executable to launch
     * @param args       arguments for the executable launched
     * @return a CompilerResult object encapsulating the result of the compilation and any compiler messages
     * @throws CompilerException
     */
    protected CompilerResult compileOutOfProcess( CompilerConfiguration config, String executable, String[] args )
        throws CompilerException
    {
        Commandline cli = new Commandline();

        cli.setWorkingDirectory( config.getWorkingDirectory().getAbsolutePath() );

        cli.setExecutable( executable );

        try
        {
            File argumentsFile = createFileWithArguments( args, config.getBuildDirectory().getAbsolutePath() );
            cli.addArguments(
                new String[]{ "@" + argumentsFile.getCanonicalPath().replace( File.separatorChar, '/' ) } );

            if ( !StringUtils.isEmpty( config.getMaxmem() ) )
            {
                cli.addArguments( new String[]{ "-J-Xmx" + config.getMaxmem() } );
            }

            if ( !StringUtils.isEmpty( config.getMeminitial() ) )
            {
                cli.addArguments( new String[]{ "-J-Xms" + config.getMeminitial() } );
            }

            for ( String key : config.getCustomCompilerArgumentsAsMap().keySet() )
            {
                if ( StringUtils.isNotEmpty( key ) && key.startsWith( "-J" ) )
                {
                    cli.addArguments( new String[]{ key } );
                }
            }
        }
        catch ( IOException e )
        {
            throw new CompilerException( "Error creating file with javac arguments", e );
        }

        CommandLineUtils.StringStreamConsumer out = new CommandLineUtils.StringStreamConsumer();

        int returnCode;

        List<CompilerMessage> messages;

        if ( ( getLogger() != null ) && getLogger().isDebugEnabled() )
        {
            String debugFileName = StringUtils.isEmpty(config.getDebugFileName()) ? "javac" : config.getDebugFileName();

            File commandLineFile =
                new File( config.getBuildDirectory(),  StringUtils.trim(debugFileName) + "." + ( Os.isFamily( Os.FAMILY_WINDOWS ) ? "bat" : "sh" ) );
            try
            {
                FileUtils.fileWrite( commandLineFile.getAbsolutePath(), cli.toString().replaceAll( "'", "" ) );

                if ( !Os.isFamily( Os.FAMILY_WINDOWS ) )
                {
                    Runtime.getRuntime().exec( new String[]{ "chmod", "a+x", commandLineFile.getAbsolutePath() } );
                }
            }
            catch ( IOException e )
            {
                if ( ( getLogger() != null ) && getLogger().isWarnEnabled() )
                {
                    getLogger().warn( "Unable to write '" + commandLineFile.getName() + "' debug script file", e );
                }
            }
        }

        try
        {
            returnCode = CommandLineUtils.executeCommandLine( cli, out, out );

            messages = parseModernStream( returnCode, new BufferedReader( new StringReader( out.getOutput() ) ) );
        }
        catch ( CommandLineException | IOException e )
        {
            throw new CompilerException( "Error while executing the external compiler.", e );
        }

        boolean success = returnCode == 0;
        return new CompilerResult( success, messages );
    }

    /**
     * Compile the java sources in the current JVM, without calling an external executable,
     * using <code>com.sun.tools.javac.Main</code> class
     *
     * @param args   arguments for the compiler as they would be used in the command line javac
     * @param config compiler configuration
     * @return a CompilerResult object encapsulating the result of the compilation and any compiler messages
     * @throws CompilerException
     */
    CompilerResult compileInProcess( String[] args, CompilerConfiguration config )
        throws CompilerException
    {
        final Class<?> javacClass = getJavacClass( config );
        final Thread thread = Thread.currentThread();
        final ClassLoader contextClassLoader = thread.getContextClassLoader();
        thread.setContextClassLoader( javacClass.getClassLoader() );
        if ( (getLogger() != null ) && getLogger().isDebugEnabled()) {
            getLogger().debug("ttcl changed run compileInProcessWithProperClassloader");
        }
        try
        {
            return compileInProcessWithProperClassloader(javacClass, args);
        }
        finally
        {
            releaseJavaccClass( javacClass, config );
            thread.setContextClassLoader( contextClassLoader );
        }
    }

    protected CompilerResult compileInProcessWithProperClassloader( Class<?> javacClass, String[] args )
        throws CompilerException {
      return compileInProcess0(javacClass, args);
    }

    /**
     * Helper method for compileInProcess()
     */
    private static CompilerResult compileInProcess0( Class<?> javacClass, String[] args )
        throws CompilerException
    {
        StringWriter out = new StringWriter();

        Integer ok;

        List<CompilerMessage> messages;

        try
        {
            Method compile = javacClass.getMethod( "compile", new Class[]{ String[].class, PrintWriter.class } );

            ok = (Integer) compile.invoke( null, new Object[]{ args, new PrintWriter( out ) } );

            messages = parseModernStream( ok, new BufferedReader( new StringReader( out.toString() ) ) );
        }
        catch ( NoSuchMethodException | IOException | InvocationTargetException | IllegalAccessException e )
        {
            throw new CompilerException( "Error while executing the compiler.", e );
        }

        boolean success = ok == 0;
        return new CompilerResult( success, messages );
    }

    /**
     * Parse the output from the compiler into a list of CompilerMessage objects
     *
     * @param exitCode The exit code of javac.
     * @param input    The output of the compiler
     * @return List of CompilerMessage objects
     * @throws IOException
     */
    static List<CompilerMessage> parseModernStream( int exitCode, BufferedReader input )
        throws IOException
    {
        List<CompilerMessage> errors = new ArrayList<>();

        String line;

        StringBuilder buffer = new StringBuilder();

        boolean hasPointer = false;

        while ( true )
        {
            line = input.readLine();

            if ( line == null )
            {
                // javac output not detected by other parsing
                // maybe better to ignore only the summary an mark the rest as error
                String bufferAsString = buffer.toString();
                if ( buffer.length() > 0 )
                {
                    if ( bufferAsString.startsWith("javac:"))
                    {
                        errors.add( new CompilerMessage( bufferAsString, CompilerMessage.Kind.ERROR ) );
                    }
                    else if ( bufferAsString.startsWith("Error occurred during initialization of boot layer"))
                    {
                        errors.add( new CompilerMessage( bufferAsString, CompilerMessage.Kind.OTHER ) );
                    }
                    else if ( hasPointer )
                    {
                        //A compiler message remains in buffer at end of parse stream
                        errors.add( parseModernError( exitCode, bufferAsString ) );
                    }
                }
                return errors;
            }

            // A compiler error occurred, treat everything that follows as part of the error.
            if (line.startsWith( "An exception has occurred in the compiler") ) {
                buffer = new StringBuilder();

                while (line != null) {
                    buffer.append(line);
                    buffer.append(EOL);
                    line = input.readLine();
                }

                errors.add( new CompilerMessage( buffer.toString(), CompilerMessage.Kind.ERROR ) );
                return errors;
            }
            else if ( line.startsWith( "An annotation processor threw an uncaught exception." ) ) {
                CompilerMessage annotationProcessingError = parseAnnotationProcessorStream( input );
                errors.add( annotationProcessingError );
            }

            // new error block?
            if ( !line.startsWith( " " ) && hasPointer )
            {
                // add the error bean
                errors.add( parseModernError( exitCode, buffer.toString() ) );

                // reset for next error block
                buffer = new StringBuilder(); // this is quicker than clearing it

                hasPointer = false;
            }

            // TODO: there should be a better way to parse these
            if ( ( buffer.length() == 0 ) && line.startsWith( "error: " ) )
            {
                errors.add( new CompilerMessage( line, CompilerMessage.Kind.ERROR ) );
            }
            else if ( ( buffer.length() == 0 ) && line.startsWith( "warning: " ) )
            {
                errors.add( new CompilerMessage( line, CompilerMessage.Kind.WARNING ) );
            }
            else if ( ( buffer.length() == 0 ) && isNote( line ) )
            {
                // skip, JDK 1.5 telling us deprecated APIs are used but -Xlint:deprecation isn't set
            }
            else if ( ( buffer.length() == 0 ) && isMisc( line ) )
            {
                // verbose output was set
                errors.add( new CompilerMessage( line, CompilerMessage.Kind.OTHER ) );
            }
            else
            {
                buffer.append( line );

                buffer.append( EOL );
            }

            if ( line.endsWith( "^" ) )
            {
                hasPointer = true;
            }
        }
    }

    private static CompilerMessage parseAnnotationProcessorStream( final BufferedReader input )
            throws IOException
    {
        String line = input.readLine();
        final StringBuilder buffer = new StringBuilder();

        while (line != null) {
            if (!line.startsWith( "Consult the following stack trace for details." )) {
                buffer.append(line);
                buffer.append(EOL);
            }
            line = input.readLine();
        }

        return new CompilerMessage( buffer.toString(), CompilerMessage.Kind.ERROR );
    }

    private static boolean isMisc( String line )
    {
        return startsWithPrefix( line, MISC_PREFIXES );
    }

    private static boolean isNote( String line )
    {
        return startsWithPrefix( line, NOTE_PREFIXES );
    }

    private static boolean startsWithPrefix( String line, String[] prefixes )
    {
        for ( String prefix : prefixes )
        {
            if ( line.startsWith( prefix ) )
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Construct a CompilerMessage object from a line of the compiler output
     *
     * @param exitCode The exit code from javac.
     * @param error    output line from the compiler
     * @return the CompilerMessage object
     */
    static CompilerMessage parseModernError( int exitCode, String error )
    {
        final StringTokenizer tokens = new StringTokenizer( error, ":" );

        boolean isError = exitCode != 0;

        try
        {
            // With Java 6 error output lines from the compiler got longer. For backward compatibility
            // .. and the time being, we eat up all (if any) tokens up to the erroneous file and source
            // .. line indicator tokens.

            boolean tokenIsAnInteger;

            StringBuilder file = null;

            String currentToken = null;

            do
            {
                if ( currentToken != null )
                {
                    if ( file == null )
                    {
                        file = new StringBuilder(currentToken);
                    }
                    else
                    {
                        file.append(':').append(currentToken);
                    }
                }

                currentToken = tokens.nextToken();

                // Probably the only backward compatible means of checking if a string is an integer.

                tokenIsAnInteger = true;

                try
                {
                    Integer.parseInt( currentToken );
                }
                catch ( NumberFormatException e )
                {
                    tokenIsAnInteger = false;
                }
            }
            while ( !tokenIsAnInteger );

            final String lineIndicator = currentToken;

            final int startOfFileName = file.toString().lastIndexOf( ']' );

            if ( startOfFileName > -1 )
            {
                file = new StringBuilder(file.substring(startOfFileName + 1 + EOL.length()));
            }

            final int line = Integer.parseInt( lineIndicator );

            final StringBuilder msgBuffer = new StringBuilder();

            String msg = tokens.nextToken( EOL ).substring( 2 );

            // Remove the 'warning: ' prefix
            final String warnPrefix = getWarnPrefix( msg );
            if ( warnPrefix != null )
            {
                isError = false;
                msg = msg.substring( warnPrefix.length() );
            }
            else
            {
                isError = exitCode != 0;
            }

            msgBuffer.append( msg );

            msgBuffer.append( EOL );

            String context = tokens.nextToken( EOL );

            String pointer = null;

            do
            {
                final String msgLine = tokens.nextToken( EOL );

                if ( pointer != null )
                {
                    msgBuffer.append( msgLine );

                    msgBuffer.append( EOL );
                }
                else if ( msgLine.endsWith( "^" ) )
                {
                    pointer = msgLine;
                }
                else
                {
                    msgBuffer.append( context );

                    msgBuffer.append( EOL );

                    context = msgLine;
                }
            }
            while ( tokens.hasMoreTokens() );

            msgBuffer.append( EOL );

            final String message = msgBuffer.toString();

            final int startcolumn = pointer.indexOf( "^" );

            int endcolumn = (context == null) ? startcolumn : context.indexOf(" ", startcolumn);

            if ( endcolumn == -1 )
            {
                endcolumn = context.length();
            }

            return new CompilerMessage(file.toString(), isError, line, startcolumn, line, endcolumn, message.trim() );
        }
        catch ( NoSuchElementException e )
        {
            return new CompilerMessage( "no more tokens - could not parse error message: " + error, isError );
        } catch ( Exception e )
        {
            return new CompilerMessage( "could not parse error message: " + error, isError );
        }
    }

    private static String getWarnPrefix( String msg )
    {
        for ( String warningPrefix : WARNING_PREFIXES )
        {
            if ( msg.startsWith( warningPrefix ) )
            {
                return warningPrefix;
            }
        }
        return null;
    }

    /**
     * put args into a temp file to be referenced using the @ option in javac command line
     *
     * @param args
     * @return the temporary file wth the arguments
     * @throws IOException
     */
    private File createFileWithArguments( String[] args, String outputDirectory )
        throws IOException
    {
        PrintWriter writer = null;
        try
        {
            File tempFile;
            if ( ( getLogger() != null ) && getLogger().isDebugEnabled() )
            {
                tempFile =
                    File.createTempFile( JavacCompiler.class.getName(), "arguments", new File( outputDirectory ) );
            }
            else
            {
                tempFile = File.createTempFile( JavacCompiler.class.getName(), "arguments" );
                tempFile.deleteOnExit();
            }

            writer = new PrintWriter( new FileWriter( tempFile ) );

            for ( String arg : args )
            {
                String argValue = arg.replace( File.separatorChar, '/' );

                writer.write( "\"" + argValue + "\"" );

                writer.println();
            }

            writer.flush();

            return tempFile;

        }
        finally
        {
            if ( writer != null )
            {
                writer.close();
            }
        }
    }

    /**
     * Get the path of the javac tool executable: try to find it depending the OS or the <code>java.home</code>
     * system property or the <code>JAVA_HOME</code> environment variable.
     *
     * @return the path of the Javadoc tool
     * @throws IOException if not found
     */
    private static String getJavacExecutable()
        throws IOException
    {
        String javacCommand = "javac" + ( Os.isFamily( Os.FAMILY_WINDOWS ) ? ".exe" : "" );

        String javaHome = System.getProperty( "java.home" );
        File javacExe;
        if ( Os.isName( "AIX" ) )
        {
            javacExe = new File( javaHome + File.separator + ".." + File.separator + "sh", javacCommand );
        }
        else if ( Os.isName( "Mac OS X" ) )
        {
            javacExe = new File( javaHome + File.separator + "bin", javacCommand );
        }
        else
        {
            javacExe = new File( javaHome + File.separator + ".." + File.separator + "bin", javacCommand );
        }

        // ----------------------------------------------------------------------
        // Try to find javacExe from JAVA_HOME environment variable
        // ----------------------------------------------------------------------
        if ( !javacExe.isFile() )
        {
            Properties env = CommandLineUtils.getSystemEnvVars();
            javaHome = env.getProperty( "JAVA_HOME" );
            if ( StringUtils.isEmpty( javaHome ) )
            {
                throw new IOException( "The environment variable JAVA_HOME is not correctly set." );
            }
            if ( !new File( javaHome ).isDirectory() )
            {
                throw new IOException(
                    "The environment variable JAVA_HOME=" + javaHome + " doesn't exist or is not a valid directory." );
            }

            javacExe = new File( env.getProperty( "JAVA_HOME" ) + File.separator + "bin", javacCommand );
        }

        if ( !javacExe.isFile() )
        {
            throw new IOException( "The javadoc executable '" + javacExe
                                       + "' doesn't exist or is not a file. Verify the JAVA_HOME environment variable." );
        }

        return javacExe.getAbsolutePath();
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
     * @throws CompilerException if the class has not been found.
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
                c = JavacCompiler.JAVAC_CLASS;
                if ( c != null )
                {
                    return c;
                }
                synchronized ( JavacCompiler.LOCK )
                {
                    if ( c == null )
                    {
                        JavacCompiler.JAVAC_CLASS = c = createJavacClass();
                    }
                    return c;
                }


        }
    }


    /**
     * Helper method for create Javac class
     */
    protected Class<?> createJavacClass()
        throws CompilerException
    {
        try
        {
            // look whether JavaC is on Maven's classpath
            //return Class.forName( JavacCompiler.JAVAC_CLASSNAME, true, JavacCompiler.class.getClassLoader() );
            return JavacCompiler.class.getClassLoader().loadClass( JavacCompiler.JAVAC_CLASSNAME );
        }
        catch ( ClassNotFoundException ex )
        {
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
            URL[] originalUrls = ((URLClassLoader) JavacCompiler.class.getClassLoader()).getURLs();
            URL[] urls = new URL[originalUrls.length + 1];
            urls[0] = toolsJar.toURI().toURL();
            System.arraycopy(originalUrls, 0, urls, 1, originalUrls.length);
            ClassLoader javacClassLoader = new URLClassLoader(urls);

            final Thread thread = Thread.currentThread();
            final ClassLoader contextClassLoader = thread.getContextClassLoader();
            thread.setContextClassLoader( javacClassLoader );
            try
            {
                //return Class.forName( JavacCompiler.JAVAC_CLASSNAME, true, javacClassLoader );
                return javacClassLoader.loadClass( JavacCompiler.JAVAC_CLASSNAME );
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
            throw new CompilerException( "Unable to locate the Javac Compiler in:" + EOL + "  " + toolsJar + EOL
                                             + "Please ensure you are using JDK 1.4 or above and" + EOL
                                             + "not a JRE (the com.sun.tools.javac.Main class is required)." + EOL
                                             + "In most cases you can change the location of your Java" + EOL
                                             + "installation by setting the JAVA_HOME environment variable.", ex );
        }
    }

}
