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
import org.codehaus.plexus.compiler.CompilerMessage;
import org.codehaus.plexus.compiler.CompilerOutputStyle;
import org.codehaus.plexus.compiler.CompilerResult;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.Os;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;
import org.codehaus.plexus.util.cli.WriterStreamConsumer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:gdodinet@karmicsoft.com">Gilles Dodinet</a>
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @author <a href="mailto:matthew.pocock@ncl.ac.uk">Matthew Pocock</a>
 * @author <a href="mailto:chris.stevenson@gmail.com">Chris Stevenson</a>
 * @plexus.component role="org.codehaus.plexus.compiler.Compiler"
 * role-hint="csharp"
 */
public class CSharpCompiler
    extends AbstractCompiler
{
    private static final String ARGUMENTS_FILE_NAME = "csharp-arguments";

    private static final String[] DEFAULT_INCLUDES = { "**/**" };

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public CSharpCompiler()
    {
        super( CompilerOutputStyle.ONE_OUTPUT_FILE_FOR_ALL_INPUT_FILES, ".cs", null, null );
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

    public CompilerResult performCompile( CompilerConfiguration config )
        throws CompilerException
    {
        File destinationDir = new File( config.getOutputLocation() );

        if ( !destinationDir.exists() )
        {
            destinationDir.mkdirs();
        }

        config.setSourceFiles( null );

        String[] sourceFiles = CSharpCompiler.getSourceFiles( config );

        if ( sourceFiles.length == 0 )
        {
            return new CompilerResult().success( true );
        }

        System.out.println( "Compiling " + sourceFiles.length + " " + "source file" +
                                ( sourceFiles.length == 1 ? "" : "s" ) + " to " + destinationDir.getAbsolutePath() );

        String[] args = buildCompilerArguments( config, sourceFiles );

        List<CompilerMessage> messages;

        if ( config.isFork() )
        {
            messages =
                compileOutOfProcess( config.getWorkingDirectory(), config.getBuildDirectory(), findExecutable( config ),
                                     args );
        }
        else
        {
            throw new CompilerException( "This compiler doesn't support in-process compilation." );
        }

        return new CompilerResult().compilerMessages( messages );
    }

    public String[] createCommandLine( CompilerConfiguration config )
        throws CompilerException
    {
        return buildCompilerArguments( config, CSharpCompiler.getSourceFiles( config ) );
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

        if ( Os.isFamily( "windows" ) )
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

    private String[] buildCompilerArguments( CompilerConfiguration config, String[] sourceFiles )
        throws CompilerException
    {
        List<String> args = new ArrayList<String>();

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

        for ( String element : config.getClasspathEntries() )
        {
            File f = new File( element );

            if ( !f.isFile() )
            {
                continue;
            }

            args.add( "/reference:\"" + element + "\"" );
        }

        // ----------------------------------------------------------------------
        // Main class
        // ----------------------------------------------------------------------

        Map<String, String> compilerArguments = config.getCustomCompilerArguments();

        String mainClass = compilerArguments.get( "-main" );

        if ( !StringUtils.isEmpty( mainClass ) )
        {
            args.add( "/main:" + mainClass );
        }

        // ----------------------------------------------------------------------
        // Xml Doc output
        // ----------------------------------------------------------------------

        String doc = compilerArguments.get( "-doc" );

        if ( !StringUtils.isEmpty( doc ) )
        {
            args.add( "/doc:" + new File( config.getOutputLocation(),
                                          config.getOutputFileName() + ".xml" ).getAbsolutePath() );
        }

        // ----------------------------------------------------------------------
        // Xml Doc output
        // ----------------------------------------------------------------------

        String nowarn = compilerArguments.get( "-nowarn" );

        if ( !StringUtils.isEmpty( nowarn ) )
        {
            args.add( "/nowarn:" + nowarn );
        }

        // ----------------------------------------------------------------------
        // Out - Override output name, this is required for generating the unit test dll
        // ----------------------------------------------------------------------

        String out = compilerArguments.get( "-out" );

        if ( !StringUtils.isEmpty( out ) )
        {
            args.add( "/out:" + new File( config.getOutputLocation(), out ).getAbsolutePath() );
        }
        else
        {
            args.add( "/out:" + new File( config.getOutputLocation(), getOutputFile( config ) ).getAbsolutePath() );
        }

        // ----------------------------------------------------------------------
        // Resource File - compile in a resource file into the assembly being created
        // ----------------------------------------------------------------------
        String resourcefile = compilerArguments.get( "-resourcefile" );

        if ( !StringUtils.isEmpty( resourcefile ) )
        {
            String resourceTarget = (String) compilerArguments.get( "-resourcetarget" );
            args.add( "/res:" + new File( resourcefile ).getAbsolutePath() + "," + resourceTarget );
        }

        // ----------------------------------------------------------------------
        // Target - type of assembly to produce, lib,exe,winexe etc... 
        // ----------------------------------------------------------------------

        String target = compilerArguments.get( "-target" );

        if ( StringUtils.isEmpty( target ) )
        {
            args.add( "/target:library" );
        }
        else
        {
            args.add( "/target:" + target );
        }

        // ----------------------------------------------------------------------
        // remove MS logo from output (not applicable for mono)
        // ----------------------------------------------------------------------
        String nologo = compilerArguments.get( "-nologo" );

        if ( !StringUtils.isEmpty( nologo ) )
        {
            args.add( "/nologo" );
        }

        // ----------------------------------------------------------------------
        // add any resource files
        // ----------------------------------------------------------------------
        this.addResourceArgs( config, args );

        // ----------------------------------------------------------------------
        // add source files
        // ----------------------------------------------------------------------
        for ( String sourceFile : sourceFiles )
        {
            args.add( sourceFile );
        }

        return args.toArray( new String[args.size()] );
    }

    private void addResourceArgs( CompilerConfiguration config, List<String> args )
    {
        File filteredResourceDir = this.findResourceDir( config );
        if ( ( filteredResourceDir != null ) && filteredResourceDir.exists() )
        {
            DirectoryScanner scanner = new DirectoryScanner();
            scanner.setBasedir( filteredResourceDir );
            scanner.setIncludes( DEFAULT_INCLUDES );
            scanner.addDefaultExcludes();
            scanner.scan();

            List<String> includedFiles = Arrays.asList( scanner.getIncludedFiles() );
            for ( String name : includedFiles )
            {
                File filteredResource = new File( filteredResourceDir, name );
                String assemblyResourceName = this.convertNameToAssemblyResourceName( name );
                String argLine = "/resource:\"" + filteredResource + "\",\"" + assemblyResourceName + "\"";
                if ( config.isDebug() )
                {
                    System.out.println( "adding resource arg line:" + argLine );
                }
                args.add( argLine );

            }
        }
    }

    private File findResourceDir( CompilerConfiguration config )
    {
        if ( config.isDebug() )
        {
            System.out.println( "Looking for resourcesDir" );
        }
        Map<String, String> compilerArguments = config.getCustomCompilerArguments();
        String tempResourcesDirAsString = (String) compilerArguments.get( "-resourceDir" );
        File filteredResourceDir = null;
        if ( tempResourcesDirAsString != null )
        {
            filteredResourceDir = new File( tempResourcesDirAsString );
            if ( config.isDebug() )
            {
                System.out.println( "Found resourceDir at: " + filteredResourceDir.toString() );
            }
        }
        else
        {
            if ( config.isDebug() )
            {
                System.out.println( "No resourceDir was available." );
            }
        }
        return filteredResourceDir;
    }

    private String convertNameToAssemblyResourceName( String name )
    {
        return name.replace( File.separatorChar, '.' );
    }

    @SuppressWarnings( "deprecation" )
    private List<CompilerMessage> compileOutOfProcess( File workingDirectory, File target, String executable,
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

            for ( String arg : args )
            {
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

        List<CompilerMessage> messages;

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
            messages.add( new CompilerMessage(
                "Failure executing the compiler, but could not parse the error:" + EOL + stringWriter.toString(),
                true ) );
        }

        return messages;
    }

    public static List<CompilerMessage> parseCompilerOutput( BufferedReader bufferedReader )
        throws IOException
    {
        List<CompilerMessage> messages = new ArrayList<CompilerMessage>();

        String line = bufferedReader.readLine();

        while ( line != null )
        {
            CompilerMessage compilerError = DefaultCSharpCompilerParser.parseLine( line );

            if ( compilerError != null )
            {
                messages.add( compilerError );
            }

            line = bufferedReader.readLine();
        }

        return messages;
    }

    private String getType( Map<String, String> compilerArguments )
    {
        String type = compilerArguments.get( "-target" );

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

        if ( "exe".equals( type ) || "winexe".equals( type ) )
        {
            return "exe";
        }

        if ( "library".equals( type ) || "module".equals( type ) )
        {
            return "dll";
        }

        throw new CompilerException( "Unrecognized type '" + type + "'." );
    }

    // added for debug purposes.... 
    protected static String[] getSourceFiles( CompilerConfiguration config )
    {
        Set<String> sources = new HashSet<String>();

        //Set sourceFiles = null;
        //was:
        Set<File> sourceFiles = config.getSourceFiles();

        if ( sourceFiles != null && !sourceFiles.isEmpty() )
        {
            for ( File sourceFile : sourceFiles )
            {
                sources.add( sourceFile.getAbsolutePath() );
            }
        }
        else
        {
            for ( String sourceLocation : config.getSourceLocations() )
            {
                sources.addAll( getSourceFilesForSourceRoot( config, sourceLocation ) );
            }
        }

        String[] result;

        if ( sources.isEmpty() )
        {
            result = new String[0];
        }
        else
        {
            result = (String[]) sources.toArray( new String[sources.size()] );
        }

        return result;
    }

    /**
     * This method is just here to maintain the public api. This is now handled in the parse
     * compiler output function.
     *
     * @author Chris Stevenson
     * @deprecated
     */
    public static CompilerMessage parseLine( String line )
    {
        return DefaultCSharpCompilerParser.parseLine( line );
    }

    protected static Set<String> getSourceFilesForSourceRoot( CompilerConfiguration config, String sourceLocation )
    {
        DirectoryScanner scanner = new DirectoryScanner();

        scanner.setBasedir( sourceLocation );

        Set<String> includes = config.getIncludes();

        if ( includes != null && !includes.isEmpty() )
        {
            String[] inclStrs = includes.toArray( new String[includes.size()] );
            scanner.setIncludes( inclStrs );
        }
        else
        {
            scanner.setIncludes( new String[]{ "**/*.cs" } );
        }

        Set<String> excludes = config.getExcludes();

        if ( excludes != null && !excludes.isEmpty() )
        {
            String[] exclStrs = excludes.toArray( new String[excludes.size()] );
            scanner.setIncludes( exclStrs );
        }

        scanner.scan();

        String[] sourceDirectorySources = scanner.getIncludedFiles();

        Set<String> sources = new HashSet<String>();

        for ( String source : sourceDirectorySources )
        {
            File f = new File( sourceLocation, source );

            sources.add( f.getPath() );
        }

        return sources;
    }
}
