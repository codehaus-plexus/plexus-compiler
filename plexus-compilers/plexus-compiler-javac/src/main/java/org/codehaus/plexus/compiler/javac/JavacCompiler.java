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
import org.codehaus.plexus.compiler.CompilerError;
import org.codehaus.plexus.compiler.CompilerException;
import org.codehaus.plexus.compiler.CompilerOutputStyle;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

/**
 * @plexus.component
 *   role="org.codehaus.plexus.compiler.Compiler"
 *   role-hint="javac"
 *
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @author <a href="mailto:matthew.pocock@ncl.ac.uk">Matthew Pocock</a>
 * @author Others
 * @version $Id$
 */
public class JavacCompiler
    extends AbstractCompiler
{
    private static final String WARNING_PREFIX = "warning: ";

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public JavacCompiler()
    {
        super( CompilerOutputStyle.ONE_OUTPUT_FILE_PER_INPUT_FILE,
               ".java",
               ".class",
               null );
    }

    // ----------------------------------------------------------------------
    // Compiler Implementation
    // ----------------------------------------------------------------------

    public List compile( CompilerConfiguration config )
        throws CompilerException
    {
        File destinationDir = new File( config.getOutputLocation() );

        if ( !destinationDir.exists() )
        {
            destinationDir.mkdirs();
        }

        String[] sourceFiles = getSourceFiles( config );

        if ( sourceFiles.length == 0 )
        {
            return Collections.EMPTY_LIST;
        }

        getLogger().info( "Compiling " + sourceFiles.length + " " +
                          "source file" + ( sourceFiles.length == 1 ? "" : "s" ) +
                          " to " + destinationDir.getAbsolutePath() );

        String[] args = buildCompilerArguments( config, sourceFiles );

        List messages;

        if ( config.isFork() )
        {
            String executable = config.getExecutable();

            if ( StringUtils.isEmpty( executable ) )
            {
                executable = "javac";
            }

            messages = compileOutOfProcess( config.getWorkingDirectory(), executable, args );
        }
        else
        {
            messages = compileInProcess( args );
        }

        return messages;
    }

    public String[] createCommandLine( CompilerConfiguration config )
            throws CompilerException
    {
        return buildCompilerArguments( config, getSourceFiles( config ) );
    }

    public static String[] buildCompilerArguments( CompilerConfiguration config,
                                                   String[] sourceFiles )
    {
        List args = new ArrayList();

        // ----------------------------------------------------------------------
        // Set output
        // ----------------------------------------------------------------------

        File destinationDir = new File( config.getOutputLocation() );

        args.add( "-d" );

        args.add( destinationDir.getAbsolutePath() );

        // ----------------------------------------------------------------------
        // Set the class and source paths
        // ----------------------------------------------------------------------

        List classpathEntries = config.getClasspathEntries();
        if ( classpathEntries != null && !classpathEntries.isEmpty() )
        {
            args.add( "-classpath" );

            args.add( getPathString( classpathEntries ) );
        }

        List sourceLocations = config.getSourceLocations();
        if ( sourceLocations != null && !sourceLocations.isEmpty() && ( sourceFiles.length == 0 ) )
        {
            args.add( "-sourcepath" );

            args.add( getPathString( sourceLocations ) );
        }

        for ( int i = 0; i < sourceFiles.length; i++ )
        {
            args.add( sourceFiles[i] );
        }

        if ( config.isOptimize() )
        {
            args.add( "-O" );
        }

        if ( config.isDebug() )
        {
            args.add( "-g" );
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

        if ( !StringUtils.isEmpty( config.getMaxmem() ) )
        {
            args.add( "-J-Xmx" + config.getMaxmem() );
        }

        if ( !StringUtils.isEmpty( config.getMeminitial() ) )
        {
            args.add( "-J-Xms" + config.getMeminitial() );
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

        for ( Iterator it = config.getCustomCompilerArguments().entrySet().iterator(); it.hasNext(); )
        {
            Map.Entry entry = (Map.Entry) it.next();

            String key = (String) entry.getKey();

            if ( StringUtils.isEmpty( key ) )
            {
                continue;
            }

            args.add( key );

            String value = (String) entry.getValue();

            if ( StringUtils.isEmpty( value ) )
            {
                continue;
            }

            args.add( value );
        }

        return (String[]) args.toArray( new String[ args.size() ] );
    }

    private static boolean suppressSource( CompilerConfiguration config )
    {
        return "1.3".equals( config.getCompilerVersion() );
    }

    private static boolean suppressEncoding( CompilerConfiguration config )
    {
        return "1.3".equals( config.getCompilerVersion() );
    }

    /**
     * Compile the java sources in a external process, calling an external executable,
     * like javac.
     * 
     * @param workingDirectory base directory where the process will be launched
     * @param executable name of the executable to launch
     * @param args arguments for the executable launched
     * @return List of CompilerError objects with the errors encountered.
     * @throws CompilerException
     */
    List compileOutOfProcess( File workingDirectory, String executable, String[] args )
        throws CompilerException
    {
        Commandline cli = new Commandline();

        cli.setWorkingDirectory( workingDirectory.getAbsolutePath() );

        cli.setExecutable( executable );

        cli.addArguments( args );

        CommandLineUtils.StringStreamConsumer out = new CommandLineUtils.StringStreamConsumer();

        CommandLineUtils.StringStreamConsumer err = new CommandLineUtils.StringStreamConsumer();

        int returnCode;

        List messages;

        try
        {
            returnCode = CommandLineUtils.executeCommandLine( cli, out, err );

            messages = parseModernStream( new BufferedReader( new StringReader( err.getOutput() ) ) );
        }
        catch ( CommandLineException e )
        {
            throw new CompilerException( "Error while executing the external compiler.", e );
        }
        catch ( IOException e )
        {
            throw new CompilerException( "Error while executing the external compiler.", e );
        }

        if ( returnCode != 0 && messages.isEmpty() )
        {
            // TODO: exception?
            messages.add( new CompilerError( "Failure executing javac,  but could not parse the error:" +  EOL +
                                                                          err.getOutput(), true ) );
        }

        return messages;
    }

    /**
     * Compile the java sources in the current JVM, without calling an external executable,
     * using <code>com.sun.tools.javac.Main</code> class
     * 
     * @param args arguments for the compiler as they would be used in the command line javac
     * @return List of CompilerError objects with the errors encountered.
     * @throws CompilerException
     */
    List compileInProcess( String[] args )
        throws CompilerException
    {
        IsolatedClassLoader cl = new IsolatedClassLoader();

        File toolsJar = new File( System.getProperty( "java.home" ), "../lib/tools.jar" );

        if ( toolsJar.exists() )
        {
            try
            {
                cl.addURL( toolsJar.toURL() );
            }
            catch ( MalformedURLException e )
            {
                throw new CompilerException( "Could not convert the file reference to tools.jar to a URL, path to tools.jar: '" + toolsJar.getAbsolutePath() + "'." );
            }
        }

        Class c;

        try
        {
            c = cl.loadClass( "com.sun.tools.javac.Main" );
        }
        catch ( ClassNotFoundException e )
        {
            String message = "Unable to locate the Javac Compiler in:" + EOL + "  " + toolsJar + EOL
                             + "Please ensure you are using JDK 1.4 or above and" + EOL
                             + "not a JRE (the com.sun.tools.javac.Main class is required)." + EOL
                             + "In most cases you can change the location of your Java" + EOL
                             + "installation by setting the JAVA_HOME environment variable.";
            return Collections.singletonList( new CompilerError( message, true ) );
        }

        StringWriter out = new StringWriter();

        Integer ok;

        List messages;

        try
        {
            Method compile = c.getMethod( "compile", new Class[] { String[].class, PrintWriter.class } );

            ok = (Integer) compile.invoke( null, new Object[] { args, new PrintWriter( out ) } );

            messages = parseModernStream( new BufferedReader( new StringReader( out.toString() ) ) );
        }
        catch ( NoSuchMethodException e )
        {
            throw new CompilerException( "Error while executing the compiler.", e );
        }
        catch ( IllegalAccessException e )
        {
            throw new CompilerException( "Error while executing the compiler.", e );
        }
        catch ( InvocationTargetException e )
        {
            throw new CompilerException( "Error while executing the compiler.", e );
        }
        catch ( IOException e )
        {
            throw new CompilerException( "Error while executing the compiler.", e );
        }

        if ( ok.intValue() != 0 && messages.isEmpty() )
        {
            // TODO: exception?
            messages.add( new CompilerError( "Failure executing javac, but could not parse the error:" + EOL +
                                             out.toString(), true ) );
        }

        return messages;
    }

    /**
     * Parse the output from the compiler into a list of CompilerError objects
     * 
     * @param input The output of the compiler
     * @return List of CompilerError objects
     * @throws IOException
     */
    protected static List parseModernStream( BufferedReader input )
        throws IOException
    {
        List errors = new ArrayList();

        String line;

        StringBuffer buffer;

        while ( true )
        {
            // cleanup the buffer
            buffer = new StringBuffer(); // this is quicker than clearing it

            // most errors terminate with the '^' char
            do
            {
                line = input.readLine();

                if ( line == null )
                {
                    return errors;
                }

                // TODO: there should be a better way to parse these
                if ( buffer.length() == 0 && line.startsWith( "error: " ) )
                {
                    errors.add( new CompilerError( line, true ) );
                }
                else if ( buffer.length() == 0 && line.startsWith( "Note: " ) )
                {
                    // skip this one - it is JDK 1.5 telling us that the interface is deprecated.
                }
                else
                {
                    buffer.append( line );

                    buffer.append( EOL );
                }
            }
            while ( !line.endsWith( "^" ) );

            // add the error bean
            errors.add( parseModernError( buffer.toString() ) );
        }
    }

    /**
     * Construct a CompilerError object from a line of the compiler output
     *  
     * @param error output line from the compiler 
     * @return the CompilerError object
     */
    public static CompilerError parseModernError( String error )
    {
        StringTokenizer tokens = new StringTokenizer( error, ":" );

        boolean isError = true;

        StringBuffer msgBuffer;

        try
        {
            String file = tokens.nextToken();

            // When will this happen?
            if ( file.length() == 1 )
            {
                file = new StringBuffer( file ).append( ":" ).append( tokens.nextToken() ).toString();
            }

            int line = Integer.parseInt( tokens.nextToken() );

            msgBuffer = new StringBuffer();

            String msg = tokens.nextToken( EOL ).substring( 2 );

            isError = !msg.startsWith( WARNING_PREFIX );

            // Remove the 'warning: ' prefix
            if ( !isError )
            {
                msg = msg.substring( WARNING_PREFIX.length() );
            }

            msgBuffer.append( msg );

            msgBuffer.append( EOL );

            String context = tokens.nextToken( EOL );

            String pointer = tokens.nextToken( EOL );

            if ( tokens.hasMoreTokens() )
            {
                msgBuffer.append( context );    // 'symbol' line

                msgBuffer.append( EOL );

                msgBuffer.append( pointer );    // 'location' line

                msgBuffer.append( EOL );

                context = tokens.nextToken( EOL );

                try
                {
                    pointer = tokens.nextToken( EOL );
                }
                catch ( NoSuchElementException e )
                {
                    pointer = context;

                    context = null;
                }

            }

            String message = msgBuffer.toString();

            int startcolumn = pointer.indexOf( "^" );

            int endcolumn = context == null ? startcolumn : context.indexOf( " ", startcolumn );

            if ( endcolumn == -1 )
            {
                endcolumn = context.length();
            }

            return new CompilerError( file, isError, line, startcolumn, line, endcolumn, message );
        }
        catch ( NoSuchElementException e )
        {
            return new CompilerError( "no more tokens - could not parse error message: " + error, isError );
        }
        catch ( NumberFormatException e )
        {
            return new CompilerError( "could not parse error message: " + error, isError );
        }
        catch ( Exception e )
        {
            return new CompilerError( "could not parse error message: " + error, isError );
        }
    }
}
