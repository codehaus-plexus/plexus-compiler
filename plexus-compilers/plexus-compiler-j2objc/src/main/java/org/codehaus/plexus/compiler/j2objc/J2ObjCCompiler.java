package org.codehaus.plexus.compiler.j2objc;

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

import org.codehaus.plexus.compiler.AbstractCompiler;
import org.codehaus.plexus.compiler.CompilerConfiguration;
import org.codehaus.plexus.compiler.CompilerException;
import org.codehaus.plexus.compiler.CompilerMessage;
import org.codehaus.plexus.compiler.CompilerMessage.Kind;
import org.codehaus.plexus.compiler.CompilerOutputStyle;
import org.codehaus.plexus.compiler.CompilerResult;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;
import org.codehaus.plexus.util.cli.WriterStreamConsumer;

/**
 * A plexus compiler which use J2ObjC .
 * It is derived from the CSharpCompiler to compile with J2ObjC.
 * 
 * @author <a href="mailto:ludovic.maitre@effervens.com">Ludovic Ma&icirc;tre</a>
 * @see CSharpCompiler
 * @plexus.component role="org.codehaus.plexus.compiler.Compiler"
 * role-hint="j2objc"
 */
public class J2ObjCCompiler
    extends AbstractCompiler
{
    private static final String ARGUMENTS_FILE_NAME = "j2objc-arguments";

    private static final String X_BOOTCLASSPATH = "Xbootclasspath";

    /**
     *    -J<flag>                     Pass Java <flag>, such as -Xmx1G, to the system runtime.
     */
    private static final String J_FLAG = "J";
    
    /**
     * --batch-translate-max=<n>    The maximum number of source files that are translated.
                                  together. Batching speeds up translation, but
                                  requires more memory.
     */
    private static final String BATCH_SIZE = "batch-translate-max";

    /**
    -pluginpath <path>           Specify where to find plugin class files.
    -pluginoptions <options>     Comma separated key=value pairs passed to all plugins.
    -t            Print time spent in translation steps.
    -Xbootclasspath:<path>       Boot path used by translation (not the tool itself).
    -Xno-jsni-warnings           Warn if JSNI (GWT) native code delimiters are used
                                   instead of OCNI delimiters.
    -sourcepath <path>           Specify where to find input source files.
    -classpath <path>            Specify where to find user class files.
    -d <directory>               Specify where to place generated Objective-C files.
    -encoding <encoding>         Specify character encoding used by source files.
    -g                           Generate Java source debugging support.
    -q			                  Do not print status messages.
    -v          	      Output messages about what the translator is doing.
    -Werror                      Make all warnings into errors.
    -h                   Print this message.
    -use-arc                     Generate Objective-C code to support Automatic
                                   Reference Counting (ARC).
    -use-reference-counting      Generate Objective-C code to support iOS manual
                                   reference counting (default).
    -x <language>                Specify what language to output.  Possible values
                                   are objective-c (default) and objective-c++.                              
     */    
    private static final List<String> ONE_DASH_ARGS = Arrays.asList( new String[] {
    		"-pluginpath",
    		"-pluginoptions",
    		"-t",
    		"-Xno-jsni-warnings",
    		"-sourcepath",
    		"-classpath",
    		"-d",
    		"-encoding",
    		"-g",
    		"-q",
    		"-v",
    		"-Werror",
    		"-h",
    		"-use-arc",
    		"-use-reference-counting",
    		"-x"    		
    });

    /**
   --build-closure              Translate dependent classes if out-of-date.
   --dead-code-report <file>    Specify a ProGuard usage report for dead code elimination.
   --doc-comments               Translate Javadoc comments into Xcode-compatible comments.
   --no-extract-unsequenced     Don't rewrite expressions that would produce unsequenced
                                  modification errors.
   --generate-deprecated        Generate deprecated attributes for deprecated methods,
                                  classes and interfaces.
   --mapping <file>             Add a method mapping file.
   --no-class-methods           Don't emit class methods for static Java methods.
                                  (static methods are always converted to functions)
   --no-final-methods-functions Disable generating functions for final methods.
   --no-hide-private-members    Includes private fields and methods in header file.
   --no-package-directories     Generate output files to specified directory, without
                                  creating package sub-directories.
   --prefix <package=prefix>    Substitute a specified prefix for a package name.
   --prefixes <file>            Specify a properties file with prefix definitions.
   --preserve-full-paths        Generates output files with the same relative paths as 
                                  the input files.
   --strip-gwt-incompatible     Removes methods that are marked with a GwtIncompatible
                                  annotation, unless its value is known to be compatible.
   --strip-reflection           Do not generate metadata needed for Java reflection.
   --segmented-headers          Generates headers with guards around each declared type.
                                  Useful for breaking import cycles.
   --timing-info            Print time spent in translation steps.
   --quiet                  Do not print status messages.
   --verbose                Output messages about what the translator is doing.
   --help                   Print this message.
   */    
    private static final List<String> TWO_DASH_ARGS = Arrays.asList( new String[] {
    	"--build-closure",
    	"--dead-code-report",
    	"--doc-comments",
    	"--no-extract-unsequenced",
    	"--generate-deprecated",
    	"--mapping",
    	"--no-class-methods",
    	"--no-final-methods-functions",
    	"--no-hide-private-members",
    	"--no-package-directories",
    	"--prefix",
    	"--prefixes",
    	"--preserve-full-paths",
    	"--strip-gwt-incompatible",
    	"--strip-reflection",
    	"--segmented-headers",
    	"--timing-info",
    	"--quiet",
    	"--verbose",
    	"--help"    	
    } );
    
    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public J2ObjCCompiler()
    {
        super( CompilerOutputStyle.ONE_OUTPUT_FILE_PER_INPUT_FILE, ".java", null, null );
    }

    // ----------------------------------------------------------------------
    // Compiler Implementation
    // ----------------------------------------------------------------------

    public boolean canUpdateTarget( CompilerConfiguration configuration )
        throws CompilerException
    {
        return false;
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

        String[] sourceFiles = J2ObjCCompiler.getSourceFiles( config );

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
        return buildCompilerArguments( config, J2ObjCCompiler.getSourceFiles( config ) );
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

        return "j2objc";
    }

    private String[] buildCompilerArguments( CompilerConfiguration config, String[] sourceFiles )
        throws CompilerException
    {
    	/*
    	j2objc --help
    	Usage: j2objc <options> <source files>
    	*/    	
        List<String> args = new ArrayList<String>();
        Map<String, String> compilerArguments = config.getCustomCompilerArgumentsAsMap();

        // Verbose
        if ( config.isVerbose() )
        {
            args.add( "-v" );
        }

        // Destination/output directory
        args.add( "-d");
        args.add(config.getOutputLocation());
        
        // config.isShowWarnings()
        // config.getSourceVersion()
        // config.getTargetVersion()
        // config.getSourceEncoding()

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        if ( !config.getClasspathEntries().isEmpty() ) {
            List<String> classpath = new ArrayList<String>();
            for ( String element : config.getClasspathEntries() )
            {
                File f = new File( element );
                classpath.add(f.getAbsolutePath());
                
                classpath.add( element );
            }
            args.add("-classpath");
            args.add(StringUtils.join(classpath.toArray(), File.pathSeparator));
        }

        for ( String k : compilerArguments.keySet() ) {
        	//System.out.println( k + "=" + compilerArguments.get( k ) );
        	String v = compilerArguments.get(k);
        	if ( J_FLAG.equals(k)) {
        		args.add(J_FLAG+v);
        	} else if ( X_BOOTCLASSPATH.equals(k)) {
        		args.add(X_BOOTCLASSPATH+":" + v);
        	} else if (BATCH_SIZE.equals(k)) {
        		args.add("-" + BATCH_SIZE +"=" + v);
        	} else {
        		if ( TWO_DASH_ARGS.contains( k ) ) {
                	args.add("-" + k);        			
        		} else if (ONE_DASH_ARGS.contains(k)) {
        			args.add(k);
        		} else {
        			throw new IllegalArgumentException("The argument " + k + " isnt't a flag recognized by J2ObjC.");
        		}
            	if ( v != null ) {
            		args.add(v);
            	}        		
        	}
        }

        // ----------------------------------------------------------------------
        // add source files
        // ----------------------------------------------------------------------
        //List<String> sources = new ArrayList<String>();
        for ( String sourceFile : sourceFiles )
        {
        	//sources.add(sourceFile)
            args.add( sourceFile );
        }
               
        return args.toArray( new String[args.size()] );
    }

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

        cli.addArguments(args);

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
                Kind.ERROR ) );
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
            CompilerMessage compilerError = DefaultJ2ObjCCompilerParser.parseLine( line );

            if ( compilerError != null )
            {
                messages.add( compilerError );
            }

            line = bufferedReader.readLine();
        }

        return messages;
    }

    // added for debug purposes.... 
    protected static String[] getSourceFiles( CompilerConfiguration config )
    {
        Set<String> sources = new HashSet<String>();

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
                // annotations directory does not always exist and the below scanner fails on non existing directories
                File potentialSourceDirectory = new File( sourceLocation );
                if ( potentialSourceDirectory.exists() ) {
                	sources.addAll( getSourceFilesForSourceRoot( config, sourceLocation ) );
                }
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
            scanner.setIncludes( new String[]{ "**/*.java" } );
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
