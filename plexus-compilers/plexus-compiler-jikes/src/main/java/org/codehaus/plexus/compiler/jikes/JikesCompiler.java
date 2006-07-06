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
import org.codehaus.plexus.compiler.CompilerConfiguration;
import org.codehaus.plexus.compiler.CompilerError;
import org.codehaus.plexus.compiler.CompilerException;
import org.codehaus.plexus.compiler.CompilerOutputStyle;
import org.codehaus.plexus.compiler.util.StreamPumper;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * @plexus.component role="org.codehaus.plexus.compiler.Compiler" role-hint="jikes"
 */
public class JikesCompiler
    extends AbstractCompiler
{
    private static final int OUTPUT_BUFFER_SIZE = 1024;

    private static final String pathSeperator = System.getProperty( "path.separator" );

    private static final String lineSeperator = System.getProperty( "line.separator" );

    private static final String fileSeperator = System.getProperty( "file.separator" );

    public JikesCompiler()
    {
        super( CompilerOutputStyle.ONE_OUTPUT_FILE_PER_INPUT_FILE,
               ".java",
               ".class",
               null );
    }

    // -----------------------------------------------------------------------
    // Compiler Implementation
    // -----------------------------------------------------------------------

    public List compile( CompilerConfiguration config )
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

            BufferedReader input = new BufferedReader( new InputStreamReader( new ByteArrayInputStream( tmpErr.toByteArray() ) ) );

            List messages = new ArrayList();

            parseStream( input, messages );

            if ( exitValue != 0 && exitValue != 1 )
            {
                messages.add( new CompilerError( "Exit code from jikes was not 0 or 1 ->" + exitValue, true ) );
            }

            return messages;
        }
        catch ( IOException e )
        {
            throw new CompilerException( "Error while compiling.", e );
        }
        catch ( InterruptedException e )
        {
            throw new CompilerException( "Error while compiling.", e );
        }
    }

    public String[] createCommandLine( CompilerConfiguration config )
        throws CompilerException
    {
        String javaHome = System.getProperty( "java.home" );

        List args = new ArrayList();

        args.add( "jikes" );

        args.add( "-bootclasspath" );

        String bootclasspath = convertListToPath( new File( javaHome + "/lib" ).listFiles() );
        bootclasspath = bootclasspath + convertListToPath( new File( javaHome + "/lib/ext" ).listFiles() );

        args.add( bootclasspath );
        getLogger().debug( "Bootclasspath: " + bootclasspath );

        args.add( "-classpath" );

        String classpathEntries = getPathString( config.getClasspathEntries() );

        args.add( classpathEntries );
        getLogger().debug( "Classpath: " + classpathEntries );

        args.add( "+E" );

        if ( config.getCustomCompilerArguments().size() > 0 )
        {
            LinkedHashMap customArgs = config.getCustomCompilerArguments();
            Object[] entries = customArgs.entrySet().toArray();
            for ( int i = 0; i < customArgs.size(); i++ )
            {
                args.add( entries[i] );
            }
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

        Object[] sources = config.getSourceLocations().toArray();

        String sourceDir = "";
        for ( int i = 0; i < sources.length; i++ )
        {
            sourceDir = sourceDir + sources[i] + pathSeperator;
        }
        if ( sources.length > 0 )
        {
            args.add( "-sourcepath" );
            args.add( sourceDir.substring( 0, sourceDir.length() - 1 ) + fileSeperator );
            getLogger().debug( "Source path:" + sourceDir );
        }

        String[] _sources = getSourceFiles( config );

        if ( System.getProperty( "os.name" ).indexOf( "Windows" ) > -1 )
        {
            String filelist = "";
            for ( int i = 0; i < _sources.length; i++ )
            {
                filelist = filelist + _sources[i] + lineSeperator;
            }

            String tempFileName = null;
            FileWriter fw = null;

            try
            {
                File tempFile = File.createTempFile( "compList", ".cmp" );
                tempFileName = tempFile.getPath();
                getLogger().debug( "create TempFile" + tempFileName );

                new File( tempFile.getParent() ).mkdirs();
                fw = new FileWriter( tempFile );
                fw.write( filelist );
                fw.flush();
                tempFile.deleteOnExit();
            }
            catch ( IOException e )
            {
                throw new CompilerException( "Could not create temporary file " + tempFileName );
            }
            finally
            {
                IOUtil.close( fw );
            }

            args.add( "@" + tempFileName );
        }
        else
        {
            for ( int i = 0; i < _sources.length; i++ )
            {
                args.add( _sources[i] );
            }

        }

        return (String[]) args.toArray( new String[ args.size() ] );
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

    private String convertListToPath( File[] files )
    {
        String filePath = "";
        for ( int i = 0; i < files.length; i++ )
        {
            if ( files[i].getPath().endsWith( ".jar" ) )
            {
                filePath = filePath + files[i].getPath() + pathSeperator;
            }
        }
        return filePath;
    }

    /**
     * Parse the compiler error stream to produce a list of
     * <code>CompilerError</code>s
     *
     * @param input The error stream
     * @return The list of compiler error messages
     * @throws IOException If an error occurs during message collection
     */
    protected List parseStream( BufferedReader input, List messages )
        throws IOException
    {
        String line = null;
        StringBuffer buffer;

        while ( true )
        {
            // cleanup the buffer
            buffer = new StringBuffer(); // this is faster than clearing it

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
     * @return A mssaged <code>CompilerError</code>
     */
    private CompilerError parseError( String error )
    {
        String[] errorBits = StringUtils.split( error, ":" );

        int i;
        String file;

        if ( System.getProperty( "os.name" ).startsWith( "Windows" ) )
        {
            file = errorBits[0] + ":" + errorBits[1];
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

        String message = errorBits[i];

        return new CompilerError( file, type.indexOf( "Error" ) > -1, startline, startcolumn, endline, endcolumn,
                                  message );
    }
}
