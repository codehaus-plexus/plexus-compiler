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

/*============================================================================
                   The Apache Software License, Version 1.1
 ============================================================================

 Copyright (C) 1999-2003 The Apache Software Foundation. All rights reserved.

 Redistribution and use in source and binary forms, with or without modifica-
 tion, are permitted provided that the following conditions are met:

 1. Redistributions of  source code must  retain the above copyright  notice,
    this list of conditions and the following disclaimer.

 2. Redistributions in binary form must reproduce the above copyright notice,
    this list of conditions and the following disclaimer in the documentation
    and/or other materials provided with the distribution.

 3. The end-user documentation included with the redistribution, if any, must
    include  the following  acknowledgment:  "This product includes  software
    developed  by the  Apache Software Foundation  (http://www.apache.org/)."
    Alternately, this  acknowledgment may  appear in the software itself,  if
    and wherever such third-party acknowledgments normally appear.

 4. The names "Apache Cocoon" and  "Apache Software Foundation" must  not  be
    used to  endorse or promote  products derived from  this software without
    prior written permission. For written permission, please contact
    apache@apache.org.

 5. Products  derived from this software may not  be called "Apache", nor may
    "Apache" appear  in their name,  without prior written permission  of the
    Apache Software Foundation.

 THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 FITNESS  FOR A PARTICULAR  PURPOSE ARE  DISCLAIMED.  IN NO  EVENT SHALL  THE
 APACHE SOFTWARE  FOUNDATION  OR ITS CONTRIBUTORS  BE LIABLE FOR  ANY DIRECT,
 INDIRECT, INCIDENTAL, SPECIAL,  EXEMPLARY, OR CONSEQUENTIAL  DAMAGES (INCLU-
 DING, BUT NOT LIMITED TO, PROCUREMENT  OF SUBSTITUTE GOODS OR SERVICES; LOSS
 OF USE, DATA, OR  PROFITS; OR BUSINESS  INTERRUPTION)  HOWEVER CAUSED AND ON
 ANY  THEORY OF LIABILITY,  WHETHER  IN CONTRACT,  STRICT LIABILITY,  OR TORT
 (INCLUDING  NEGLIGENCE OR  OTHERWISE) ARISING IN  ANY WAY OUT OF THE  USE OF
 THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 This software  consists of voluntary contributions made  by many individuals
 on  behalf of the Apache Software  Foundation and was  originally created by
 Stefano Mazzocchi  <stefano@apache.org>. For more  information on the Apache
 Software Foundation, please see <http://www.apache.org/>.

*/

import org.codehaus.plexus.compiler.AbstractCompiler;
import org.codehaus.plexus.compiler.Compiler;
import org.codehaus.plexus.compiler.CompilerConfiguration;
import org.codehaus.plexus.compiler.CompilerException;
import org.codehaus.plexus.compiler.CompilerMessage;
import org.codehaus.plexus.compiler.CompilerOutputStyle;
import org.codehaus.plexus.compiler.CompilerResult;
import org.codehaus.plexus.compiler.util.StreamPumper;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.util.Os;
import org.codehaus.plexus.util.StringUtils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component( role = Compiler.class, hint = "jikes" )
public class JikesCompiler
    extends AbstractCompiler
{
    private static final int OUTPUT_BUFFER_SIZE = 1024;

    public JikesCompiler()
    {
        super( CompilerOutputStyle.ONE_OUTPUT_FILE_PER_INPUT_FILE, ".java", ".class", null );
    }

    // -----------------------------------------------------------------------
    // Compiler Implementation
    // -----------------------------------------------------------------------

    public CompilerResult performCompile( CompilerConfiguration config )
        throws CompilerException
    {
        // Ensures that the directory exist.
        getDestinationDir( config );

        try
        {
            // TODO: This should use the CommandLine stuff from plexus-utils.

            // -----------------------------------------------------------------------
            // Execute the compiler
            // -----------------------------------------------------------------------

            Process p = Runtime.getRuntime().exec( createCommandLine( config ) );

            BufferedInputStream compilerErr = new BufferedInputStream( p.getErrorStream() );

            ByteArrayOutputStream tmpErr = new ByteArrayOutputStream( OUTPUT_BUFFER_SIZE );
            StreamPumper errPumper = new StreamPumper( compilerErr, tmpErr );

            errPumper.start();

            p.waitFor();

            int exitValue = p.exitValue();

            // Wait until the complete error stream has been read
            errPumper.join();

            compilerErr.close();

            p.destroy();

            tmpErr.close();

            // -----------------------------------------------------------------------
            // Parse the output
            // -----------------------------------------------------------------------

            BufferedReader input =
                new BufferedReader( new InputStreamReader( new ByteArrayInputStream( tmpErr.toByteArray() ) ) );

            List<CompilerMessage> messages = new ArrayList<>();

            parseStream( input, messages );

            if ( exitValue != 0 && exitValue != 1 )
            {
                messages.add( new CompilerMessage( "Exit code from jikes was not 0 or 1 ->" + exitValue, true ) );
            }

            return new CompilerResult().compilerMessages( messages );
        }
        catch ( IOException | InterruptedException e )
        {
            throw new CompilerException( "Error while compiling.", e );
        }
    }

    public String[] createCommandLine( CompilerConfiguration config )
        throws CompilerException
    {
        List<String> args = new ArrayList<>();

        args.add( "jikes" );

        String bootClassPath = getPathString( getBootClassPath() );
        getLogger().debug( "Bootclasspath: " + bootClassPath );
        if ( !StringUtils.isEmpty( bootClassPath ) )
        {
            args.add( "-bootclasspath" );
            args.add( bootClassPath );
        }

        String classPath = getPathString( config.getClasspathEntries() );
        getLogger().debug( "Classpath: " + classPath );
        if ( !StringUtils.isEmpty( classPath ) )
        {
            args.add( "-classpath" );
            args.add( classPath );
        }

        args.add( "+E" );

        for ( Map.Entry<String, String> arg : config.getCustomCompilerArgumentsAsMap().entrySet() )
        {
            args.add( arg.getKey() );
            args.add( arg.getValue() );
        }


        args.add( "-target" );
        if ( StringUtils.isNotEmpty( config.getTargetVersion() ) )
        {
            args.add( config.getTargetVersion() );
        }
        else
        {
            args.add( "1.1" );
        }

        args.add( "-source" );
        if ( StringUtils.isNotEmpty( config.getSourceVersion() ) )
        {
            args.add( config.getSourceVersion() );
        }
        else
        {
            args.add( "1.3" );
        }

        if ( !config.isShowWarnings() )
        {
            args.add( "-nowarn" );
        }

        if ( config.isShowDeprecation() )
        {
            args.add( "-deprecation" );
        }

        if ( config.isOptimize() )
        {
            args.add( "-O" );
        }

        if ( config.isVerbose() )
        {
            args.add( "-verbose" );
        }

        if ( config.isDebug() )
        {
            args.add( "-g:lines" );
        }

        args.add( "-d" );

        args.add( getDestinationDir( config ).getAbsolutePath() );

        String sourcePath = getPathString( config.getSourceLocations() );
        getLogger().debug( "Source path:" + sourcePath );
        if ( !StringUtils.isEmpty( sourcePath ) )
        {
            args.add( "-sourcepath" );
            args.add( sourcePath );
        }

        String[] sourceFiles = getSourceFiles( config );

        if ( Os.isFamily( Os.FAMILY_WINDOWS ) )
        {
            String tempFileName = null;
            try
            {
                File tempFile = File.createTempFile( "compList", ".cmp" );
                tempFileName = tempFile.getAbsolutePath();
                try (BufferedWriter fw = new BufferedWriter( new FileWriter( tempFile ) ))
                {

                    getLogger().debug( "create TempFile" + tempFileName );

                    tempFile.getParentFile().mkdirs();
                    for ( int i = 0; i < sourceFiles.length; i++ )
                    {
                        fw.write( sourceFiles[i] );
                        fw.newLine();
                    }
                    tempFile.deleteOnExit();
                }
            }
            catch ( IOException e )
            {
                throw new CompilerException( "Could not create temporary file " + tempFileName, e );
            }

            args.add( "@" + tempFileName );
        }
        else
        {
            for ( int i = 0; i < sourceFiles.length; i++ )
            {
                args.add( sourceFiles[i] );
            }

        }

        return args.toArray( new String[args.size()] );
    }

    // -----------------------------------------------------------------------
    // Private
    // -----------------------------------------------------------------------

    private File getDestinationDir( CompilerConfiguration config )
    {
        File destinationDir = new File( config.getOutputLocation() );

        if ( !destinationDir.exists() )
        {
            destinationDir.mkdirs();
        }

        return destinationDir;
    }

    private List<String> getBootClassPath()
    {
        List<String> bootClassPath = new ArrayList<>();
        FileFilter filter = new FileFilter()
        {

            public boolean accept( File file )
            {
                String name = file.getName();
                return name.endsWith( ".jar" ) || name.endsWith( ".zip" );
            }

        };

        File javaHomeDir = new File( System.getProperty( "java.home" ) );

        File javaLibDir = new File( javaHomeDir, "lib" );
        if ( javaLibDir.isDirectory() )
        {
            bootClassPath.addAll( asList( javaLibDir.listFiles( filter ) ) );
        }

        File javaClassesDir = new File( javaHomeDir, "../Classes" );
        if ( javaClassesDir.isDirectory() )
        {
            bootClassPath.addAll( asList( javaClassesDir.listFiles( filter ) ) );
        }

        File javaExtDir = new File( javaLibDir, "ext" );
        if ( javaExtDir.isDirectory() )
        {
            bootClassPath.addAll( asList( javaExtDir.listFiles( filter ) ) );
        }

        return bootClassPath;
    }

    private List<String> asList( File[] files )
    {
        List<String> filenames = new ArrayList<>( files.length );
        for ( File file : files )
        {
            filenames.add( file.toString() );
        }
        return filenames;
    }

    /**
     * Parse the compiler error stream to produce a list of
     * <code>CompilerMessage</code>s
     *
     * @param input The error stream
     * @return The list of compiler error messages
     * @throws IOException If an error occurs during message collection
     */
    protected List<CompilerMessage> parseStream( BufferedReader input, List<CompilerMessage> messages )
        throws IOException
    {
        String line = null;
        StringBuilder buffer;

        while ( true )
        {
            // cleanup the buffer
            buffer = new StringBuilder(); // this is faster than clearing it

            // first line is not space-starting
            if ( line == null )
            {
                line = input.readLine();
            }

            if ( line == null )
            {
                return messages;
            }

            buffer.append( line );

            // all other space-starting lines are one error
            while ( true )
            {
                line = input.readLine();
                // EOF
                if ( line == null )
                {
                    break;
                }
                // Continuation of previous error starts with ' '
                if ( line.length() > 0 && line.charAt( 0 ) != ' ' )
                {
                    break;
                }
                buffer.append( EOL );
                buffer.append( line );
            }

            if ( buffer.length() > 0 )
            {
                // add the error bean
                messages.add( parseError( buffer.toString() ) );
            }
        }
    }

    /**
     * Parse an individual compiler error message
     *
     * @param error The error text
     * @return A mssaged <code>CompilerMessage</code>
     */
    private CompilerMessage parseError( String error )
    {
        if ( error.startsWith( "Error:" ) )
        {
            return new CompilerMessage( error, true );
        }

        String[] errorBits = StringUtils.split( error, ":" );

        int i;
        String file;

        if ( Os.isFamily( Os.FAMILY_WINDOWS ) )
        {
            file = errorBits[0] + ':' + errorBits[1];
            i = 2;
        }
        else
        {
            file = errorBits[0];
            i = 1;
        }

        int startline = Integer.parseInt( errorBits[i++] );

        int startcolumn = Integer.parseInt( errorBits[i++] );

        int endline = Integer.parseInt( errorBits[i++] );

        int endcolumn = Integer.parseInt( errorBits[i++] );

        String type = errorBits[i++];

        StringBuilder message = new StringBuilder( errorBits[i++] );
        while ( i < errorBits.length )
        {
            message.append( ':' ).append( errorBits[i++] );
        }

        return new CompilerMessage( file, type.indexOf( "Error" ) > -1, startline, startcolumn, endline, endcolumn,
                                    message.toString() );
    }
}
