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
import org.codehaus.plexus.util.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

public class JavacCompiler
    extends AbstractCompiler
{
    private static final int OUTPUT_BUFFER_SIZE = 1024;

    private static final String EOL = System.getProperty( "line.separator" );

    public List compile( CompilerConfiguration config )
        throws Exception
    {
        File destinationDir = new File( config.getOutputLocation() );

        if ( !destinationDir.exists() )
        {
            destinationDir.mkdirs();
        }

        String[] sources = getSourceFiles( config );

        if ( sources.length == 0 )
        {
            return Collections.EMPTY_LIST;
        }

        getLogger().info( "Compiling " + sources.length + " source file" + ( sources.length == 1 ? "" : "s" ) + " " +
                          "to " + destinationDir.getAbsolutePath() );

        List args = new ArrayList( 100 );

        // ----------------------------------------------------------------------
        // Build command line arguments list
        // ----------------------------------------------------------------------

        args.add( "-d" );

        args.add( destinationDir.getAbsolutePath() );

        List classpathEntries = config.getClasspathEntries();
        if ( classpathEntries != null && !classpathEntries.isEmpty() )
        {
            args.add( "-classpath" );

            args.add( getPathString( classpathEntries ) );
        }

        List sourceLocations = config.getSourceLocations();
        if ( sourceLocations != null && !sourceLocations.isEmpty() )
        {
            args.add( "-sourcepath" );

            args.add( getPathString( sourceLocations ) );
        }

        // ----------------------------------------------------------------------
        // Build settings from configuration
        // ----------------------------------------------------------------------

        if ( config.isDebug() )
        {
            args.add( "-g" );
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

        if ( StringUtils.isEmpty( config.getSourceVersion() ) )
        {
            // If omitted, later JDKs complain about a 1.1 target
            args.add( "-source" );
            args.add( "1.3" );
        }
        else
        {
            args.add( "-source" );
            args.add( config.getSourceVersion() );
        }

        if ( !StringUtils.isEmpty( config.getSourceEncoding() ) )
        {
            args.add( "-encoding" );
            args.add( config.getSourceEncoding() );
        }

        // ----------------------------------------------------------------------
        // Add all other compiler options verbatim
        // ----------------------------------------------------------------------

        Map compilerOptions = config.getCompilerOptions();

        Iterator it = compilerOptions.entrySet().iterator();

        while ( it.hasNext() )
        {
            Map.Entry entry = (Map.Entry) it.next();
            args.add( entry.getKey() );
            if ( entry.getValue() != null )
            {
                args.add( entry.getValue() );
            }
        }

        for ( int i = 0; i < sources.length; i++ )
        {
            args.add( sources[i] );
        }

        IsolatedClassLoader cl = new IsolatedClassLoader();

        File toolsJar = new File( System.getProperty( "java.home" ), "../lib/tools.jar" );

        if ( toolsJar.exists() )
        {
            cl.addURL( toolsJar.toURL() );
        }

        Class c;

        try
        {
            c = cl.loadClass( "com.sun.tools.javac.Main" );
        }
        catch ( ClassNotFoundException e )
        {
            String message = "Unable to locate the Javac Compiler in:" + EOL + "  " + toolsJar + EOL +
                "Please ensure you are using JDK 1.4 or above and" + EOL +
                "not a JRE (the com.sun.tools.javac.Main class is required)." + EOL +
                "In most cases you can change the location of your Java" + EOL +
                "installation by setting the JAVA_HOME environment variable.";
            return Collections.singletonList( new CompilerError( message, true ) );
        }

        StringWriter out = new StringWriter();

        Method compile = c.getMethod( "compile", new Class[]{String[].class, PrintWriter.class} );

        Integer ok = (Integer) compile.invoke( null, new Object[]{args.toArray( new String[0] ), new PrintWriter( out )} );

        List messages = parseModernStream(  new BufferedReader( new StringReader( out.toString() ) ) );

        if ( ok.intValue() != 0 && messages.isEmpty() )
        {
            // TODO: exception?
            messages.add( new CompilerError( "Failure executing javac, " +
                                             "but could not parse the error:" + EOL + EOL + out.toString(), true ) );
        }

        return messages;
    }

    protected List parseModernStream( BufferedReader input )
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

    public static CompilerError parseModernError( String error )
    {
        StringTokenizer tokens = new StringTokenizer( error, ":" );

        boolean isError;

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

            String WARNING_PREFIX = "warning: ";

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
/*
            if ( tokens.hasMoreTokens() )
            {
                msgBuffer.append( context );	// 'symbol' line

                msgBuffer.append( EOL );

                msgBuffer.append( pointer );	// 'location' line

                msgBuffer.append( EOL );

                context = tokens.nextToken( EOL );

                pointer = tokens.nextToken( EOL );
            }
*/
            String message = msgBuffer.toString();

            int startcolumn = pointer.indexOf( "^" );

            int endcolumn = context.indexOf( " ", startcolumn );

            if ( endcolumn == -1 )
            {
                endcolumn = context.length();
            }

            return new CompilerError( file, isError, line, startcolumn, line, endcolumn, message );
        }
        catch ( NoSuchElementException e )
        {
            e.printStackTrace();
            return new CompilerError( "no more tokens - could not parse error message: " + error, true );
        }
        catch ( NumberFormatException e )
        {
            return new CompilerError( "could not parse error message: " + error, true );
        }
        catch ( Exception e )
        {
            return new CompilerError( "could not parse error message: " + error, true );
        }
    }
}
