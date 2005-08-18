package org.codehaus.plexus.compiler.csharp;

/*
 * Copyright 2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.codehaus.plexus.compiler.AbstractCompiler;
import org.codehaus.plexus.compiler.CompilerConfiguration;
import org.codehaus.plexus.compiler.CompilerException;
import org.codehaus.plexus.compiler.CompilerOutputStyle;
import org.codehaus.plexus.compiler.CompilerError;
import org.codehaus.plexus.util.Os;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;
import org.codehaus.plexus.util.cli.WriterStreamConsumer;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.CommandLineException;

import java.io.File;
import java.io.Writer;
import java.io.StringWriter;
import java.io.BufferedReader;
import java.io.StringReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * @author <a href="mailto:gdodinet@karmicsoft.com">Gilles Dodinet</a>
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @version $Id$
 */
public class CSharpCompiler
    extends AbstractCompiler
{
    private final String ARGUMENTS_FILE_NAME = "csharp-arguments";
    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public CSharpCompiler()
    {
        super( CompilerOutputStyle.ONE_OUTPUT_FILE_FOR_ALL_INPUT_FILES,
               ".cs",
               null,
               null );
    }

    // ----------------------------------------------------------------------
    // Compiler Implementation
    // ----------------------------------------------------------------------

    public boolean canUpdateTarget( CompilerConfiguration configuration )
        throws CompilerException
    {
        return false;
    }

    public String getOutputFile( CompilerConfiguration configuration )
        throws CompilerException
    {
        return configuration.getOutputFileName() + "." + getTypeExtension( configuration );
    }

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

        System.out.println( "Compiling " + sourceFiles.length + " " +
                            "source file" + ( sourceFiles.length == 1 ? "" : "s" ) +
                            " to " + destinationDir.getAbsolutePath() );

        String[] args = buildCompilerArguments( config, sourceFiles );

        List messages;

        if ( config.isFork() )
        {
            messages = compileOutOfProcess( config.getWorkingDirectory(),
                                            config.getBuildDirectory(),
                                            findExecutable( config ),
                                            args );
        }
        else
        {
            throw new CompilerException( "This compiler doesn't support in-process compilation." );
        }

        return messages;
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------
    
    private String findExecutable( CompilerConfiguration config )
    {
        String executable = config.getExecutable();

        if ( !StringUtils.isEmpty( executable ) )
        {
            return executable;
        }

        if ( Os.isFamily("windows") )
        {
            return "csc";
        }

        return "mcs";
    }

    /*
$ mcs --help
Mono C# compiler, (C) 2001 - 2003 Ximian, Inc.
mcs [options] source-files
   --about            About the Mono C# compiler
   -addmodule:MODULE  Adds the module to the generated assembly
   -checked[+|-]      Set default context to checked
   -codepage:ID       Sets code page to the one in ID (number, utf8, reset)
   -clscheck[+|-]     Disables CLS Compliance verifications
   -define:S1[;S2]    Defines one or more symbols (short: /d:)
   -debug[+|-], -g    Generate debugging information
   -delaysign[+|-]    Only insert the public key into the assembly (no signing)
   -doc:FILE          XML Documentation file to generate
   -keycontainer:NAME The key pair container used to strongname the assembly
   -keyfile:FILE      The strongname key file used to strongname the assembly
   -langversion:TEXT  Specifies language version modes: ISO-1 or Default
   -lib:PATH1,PATH2   Adds the paths to the assembly link path
   -main:class        Specified the class that contains the entry point
   -noconfig[+|-]     Disables implicit references to assemblies
   -nostdlib[+|-]     Does not load core libraries
   -nowarn:W1[,W2]    Disables one or more warnings
   -optimize[+|-]     Enables code optimalizations
   -out:FNAME         Specifies output file
   -pkg:P1[,Pn]       References packages P1..Pn
   -recurse:SPEC      Recursively compiles the files in SPEC ([dir]/file)
   -reference:ASS     References the specified assembly (-r:ASS)
   -target:KIND       Specifies the target (KIND is one of: exe, winexe,
                      library, module), (short: /t:)
   -unsafe[+|-]       Allows unsafe code
   -warnaserror[+|-]  Treat warnings as errors
   -warn:LEVEL        Sets warning level (the highest is 4, the default is 2)
   -help2             Show other help flags

Resources:
   -linkresource:FILE[,ID] Links FILE as a resource
   -resource:FILE[,ID]     Embed FILE as a resource
   -win32res:FILE          Specifies Win32 resource file (.res)
   -win32icon:FILE         Use this icon for the output
   @file                   Read response file for more options

Options can be of the form -option or /option
    */

    private String[] buildCompilerArguments( CompilerConfiguration config,
                                             String[] sourceFiles )
        throws CompilerException
    {
        List args = new ArrayList();

        if ( config.isDebug() )
        {
            args.add( "/debug+" );
        }
        else
        {
            args.add( "/debug-" );
        }

        // config.isShowWarnings()
        // config.getSourceVersion()
        // config.getTargetVersion()
        // config.getSourceEncoding()

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        for ( Iterator it = config.getClasspathEntries().iterator(); it.hasNext(); )
        {
            String element = (String) it.next();

            File f = new File( element );

            if ( !f.isFile() )
            {
                continue;
            }

            args.add( "/reference:" + element );
        }

        // TODO: include resources

        // ----------------------------------------------------------------------
        // Main class
        // ----------------------------------------------------------------------

        Map compilerArguments = config.getCustomCompilerArguments();

        String mainClass = (String) compilerArguments.get( "mainClass" );

        if ( !StringUtils.isEmpty( mainClass ) )
        {
            args.add( "/mainClass" );

            args.add( mainClass );
        }

        // ----------------------------------------------------------------------
        // Out
        // ----------------------------------------------------------------------

        args.add( "/out:" + new File( config.getBuildDirectory(), getOutputFile( config ) ).getAbsolutePath() );

        // ----------------------------------------------------------------------
        // Target
        // ----------------------------------------------------------------------

        String target = (String) compilerArguments.get( "target" );

        if ( StringUtils.isEmpty( target ) )
        {
            args.add( "/target:library" );
        }
        else
        {
            args.add( "/target:" + target );
        }

        for ( int i = 0; i < sourceFiles.length; i++ )
        {
            String sourceFile = sourceFiles[ i ];

            args.add( sourceFile );
        }

        return (String[]) args.toArray( new String[ args.size() ] );
    }

    private List compileOutOfProcess( File workingDirectory,
                                      File target,
                                      String executable,
                                      String[] args )
        throws CompilerException
    {
        // ----------------------------------------------------------------------
        // Build the @arguments file
        // ----------------------------------------------------------------------

        File file;

        PrintWriter output = null;

        try
        {
            file = new File( target, ARGUMENTS_FILE_NAME );

            output = new PrintWriter( new FileWriter( file ) );

            for ( int i = 0; i < args.length; i++ )
            {
                String arg = args[ i ];

                System.out.println( "arg: " + arg );
                output.println( arg );
            }
        }
        catch ( IOException e )
        {
            throw new CompilerException( "Error writing arguments file.", e );
        }
        finally
        {
            IOUtil.close( output );
        }

        // ----------------------------------------------------------------------
        // Execute!
        // ----------------------------------------------------------------------

        Commandline cli = new Commandline();

        cli.setWorkingDirectory( workingDirectory.getAbsolutePath() );

        cli.setExecutable( executable );

        cli.createArgument().setValue( "@" + file.getAbsolutePath() );

        Writer stringWriter = new StringWriter();

        StreamConsumer out = new WriterStreamConsumer( stringWriter );

        StreamConsumer err = new WriterStreamConsumer( stringWriter );

        int returnCode;

        List messages;

        try
        {
            returnCode = CommandLineUtils.executeCommandLine( cli, out, err );

            messages = parseCompilerOutput( new BufferedReader( new StringReader( stringWriter.toString() ) ) );
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
            messages.add( new CompilerError( "Failure executing the compiler, but could not parse the error:" +  EOL +
                                             stringWriter.toString(), true ) );
        }

        return messages;
    }

    public static List parseCompilerOutput( BufferedReader bufferedReader )
        throws IOException
    {
        List messages = new ArrayList();

        String line = bufferedReader.readLine();

        while( line != null )
        {
            CompilerError compilerError = parseLine( line );

            if ( compilerError != null )
            {
                messages.add( compilerError );
            }

            line = bufferedReader.readLine();
        }

        return messages;
    }

    public static CompilerError parseLine( String line )
    {
        String file = null;
        boolean error = true;
        int startline = -1;
        int startcolumn = -1;
        int endline = -1;
        int endcolumn = -1;
        String message;

        String ERROR_PREFIX = "error ";

        String COMPILATION_PREFIX = "Compilation ";

        String MAGIC_LINE_MARKER = ".cs(";

        if ( line.startsWith( ERROR_PREFIX ) )
        {
            message = line.substring( ERROR_PREFIX.length() );
        }
        else if ( line.startsWith( COMPILATION_PREFIX ) )
        {
            // ignore

            return null;
        }
        else if ( line.indexOf( MAGIC_LINE_MARKER ) != -1 )
        {
            int i = line.indexOf( MAGIC_LINE_MARKER );

            int j = line.indexOf( ' ', i );

            file = line.substring( 0, i + 3 );

            String num = line.substring( i + MAGIC_LINE_MARKER.length(), j - 1 );

            startline = Integer.parseInt( num );

            endline = startline;

            message = line.substring( j + 1 + ERROR_PREFIX.length() );

            error = line.indexOf( ") error" ) != -1;
        }
        else
        {
            System.err.println( "Unknown output: " + line );

            return null;
        }

        return new CompilerError( file,
                                  error,
                                  startline,
                                  startcolumn,
                                  endline,
                                  endcolumn,
                                  message );
    }

    private String getType( Map compilerArguments )
    {
        String type = (String) compilerArguments.get( "type" );

        if ( StringUtils.isEmpty( type ) )
        {
            return "library";
        }

        return type;
    }

    private String getTypeExtension( CompilerConfiguration configuration )
        throws CompilerException
    {
        String type = getType( configuration.getCustomCompilerArguments() );

        if ( type.equals( "exe" ) || type.equals( "winexe" ) )
        {
            return "exe";
        }

        if ( type.equals( "library" ) || type.equals( "module" ) )
        {
            return "dll";
        }

        throw new CompilerException( "Unrecognized type '" + type + "'." );
    }
}
