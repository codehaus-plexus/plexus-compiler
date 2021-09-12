package org.codehaus.plexus.compiler.eclipse;

/**
 * The MIT License
 * <p>
 * Copyright (c) 2005, The Codehaus
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import org.codehaus.plexus.compiler.AbstractCompiler;
import org.codehaus.plexus.compiler.Compiler;
import org.codehaus.plexus.compiler.CompilerConfiguration;
import org.codehaus.plexus.compiler.CompilerException;
import org.codehaus.plexus.compiler.CompilerMessage;
import org.codehaus.plexus.compiler.CompilerOutputStyle;
import org.codehaus.plexus.compiler.CompilerResult;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.StringUtils;
import org.eclipse.jdt.core.compiler.CompilationProgress;
import org.eclipse.jdt.core.compiler.batch.BatchCompiler;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.ServiceLoader;
import java.util.Set;

/**
 *
 */
@Component( role = Compiler.class, hint = "eclipse" )
public class EclipseJavaCompiler
    extends AbstractCompiler
{
    public EclipseJavaCompiler()
    {
        super( CompilerOutputStyle.ONE_OUTPUT_FILE_PER_INPUT_FILE, ".java", ".class", null );
    }

    // ----------------------------------------------------------------------
    // Compiler Implementation
    // ----------------------------------------------------------------------
    boolean errorsAsWarnings = false;

    @Override
    public CompilerResult performCompile( CompilerConfiguration config )
        throws CompilerException
    {
        List<String> args = new ArrayList<>();
        args.add( "-noExit" );                            // Make sure ecj does not System.exit on us 8-/

        // Build settings from configuration
        if ( config.isDebug() )
        {
            args.add( "-preserveAllLocals" );
            args.add( "-g:lines,vars,source" );
        }
        else
        {
            args.add( "-g:lines,source" );
        }

        String releaseVersion = decodeVersion( config.getReleaseVersion() );
        // EcjFailureException: Failed to run the ecj compiler: option -source is not supported when --release is used
        if ( releaseVersion != null )
        {
            args.add( "--release" );
            args.add( releaseVersion );
        }
        else
        {
            String sourceVersion = decodeVersion( config.getSourceVersion() );

            if ( sourceVersion != null )
            {
                args.add( "-source" );
                args.add( sourceVersion );
            }

            String targetVersion = decodeVersion( config.getTargetVersion() );

            if ( targetVersion != null )
            {
                args.add( "-target" );
                args.add( targetVersion );
            }
        }

        if ( StringUtils.isNotEmpty( config.getSourceEncoding() ) )
        {
            args.add( "-encoding" );
            args.add( config.getSourceEncoding() );
        }

        if ( !config.isShowWarnings() )
        {
            args.add( "-warn:none" );
        }
        else
        {
            String warnings = config.getWarnings();
            StringBuilder warns = StringUtils.isEmpty(warnings)
                    ? new StringBuilder()
                    : new StringBuilder(warnings).append(',');

            if ( config.isShowDeprecation() )
            {
                append( warns, "+deprecation" );
            }
            else
            {
                append( warns, "-deprecation" );
            }

            //-- Make room for more warnings to be enabled/disabled
            args.add( "-warn:" + warns );
        }

        if ( config.isParameters() )
        {
            args.add( "-parameters" );
        }
        
        if(config.isFailOnWarning())
        {
        	args.add("-failOnWarning");
        }

        // Set Eclipse-specific options
        // compiler-specific extra options override anything else in the config object...
        this.errorsAsWarnings = processCustomArguments(config, args);

        // Output path
        args.add( "-d" );
        args.add( config.getOutputLocation() );

        // Annotation processors defined?
        List<String> extraSourceDirs = new ArrayList<>();
        if ( !isPreJava1_6( config ) )
        {
            File generatedSourcesDir = config.getGeneratedSourcesDirectory();
            if ( generatedSourcesDir != null )
            {
                generatedSourcesDir.mkdirs();
                extraSourceDirs.add( generatedSourcesDir.getAbsolutePath() );

                //-- option to specify where annotation processor is to generate its output
                args.add( "-s" );
                args.add( generatedSourcesDir.getAbsolutePath() );
            }

            //now add jdk 1.6 annotation processing related parameters
            String[] annotationProcessors = config.getAnnotationProcessors();
            List<String> processorPathEntries = config.getProcessorPathEntries();
            List<String> processorModulePathEntries = config.getProcessorModulePathEntries();
            
            if ( ( annotationProcessors != null && annotationProcessors.length > 0 ) 
                            || ( processorPathEntries != null && processorPathEntries.size() > 0 ) 
                            || ( processorModulePathEntries != null && processorModulePathEntries.size() > 0 ) )
            {
                if ( annotationProcessors != null && annotationProcessors.length > 0 )
                {
                    args.add( "-processor" );
                    StringBuilder sb = new StringBuilder();
                    for ( String ap : annotationProcessors )
                    {
                        if ( sb.length() > 0 )
                        {
                            sb.append( ',' );
                        }
                        sb.append( ap );
                    }
                    args.add( sb.toString() );
                }

                if ( processorPathEntries != null && processorPathEntries.size() > 0 )
                {
                    args.add( "-processorpath" );
                    args.add( getPathString( processorPathEntries ) );
                }
                
                if ( processorModulePathEntries != null && processorModulePathEntries.size() > 0 )
                {
                    args.add( "-processorpath" );
                    args.add( getPathString( processorModulePathEntries ) );
                }

                if ( config.getProc() != null )
                {
                    args.add( "-proc:" + config.getProc() );
                }
            }
        }

        //-- classpath
        List<String> classpathEntries = new ArrayList<>( config.getClasspathEntries() );
        classpathEntries.add( config.getOutputLocation() );
        args.add( "-classpath" );
        args.add( getPathString( classpathEntries ) );

        // Collect sources
        List<String> allSources = new ArrayList<>();
        for ( String source : config.getSourceLocations() )
        {
            File srcFile = new File( source );
            if ( srcFile.exists() )
            {
                Set<String> ss = getSourceFilesForSourceRoot( config, source );
                allSources.addAll( ss );
            }
        }
        for ( String extraSrcDir : extraSourceDirs )
        {
            File extraDir = new File( extraSrcDir );
            if ( extraDir.isDirectory() )
            {
                addExtraSources( extraDir, allSources );
            }
        }
        List<CompilerMessage> messageList = new ArrayList<>();
        if ( allSources.isEmpty() )
        {
            // -- Nothing to do -> bail out
            return new CompilerResult( true, messageList );
        }

        // Compile
        try
        {
            StringWriter sw = new StringWriter();
            PrintWriter devNull = new PrintWriter( sw );
            JavaCompiler compiler = getEcj();
            boolean success = false;
            if ( compiler != null )
            {
                getLogger().debug( "Using JSR-199 EclipseCompiler" );
                // ECJ JSR-199 compiles against the latest Java version it supports if no source
                // version is given explicitly. BatchCompiler uses 1.3 as default. So check
                // whether a source version is specified, and if not supply 1.3 explicitly.
                if ( !haveSourceOrReleaseArgument( args ) )
                {
                    getLogger().debug( "ecj: no source level nor release specified, defaulting to Java 1.3" );
                    args.add( "-source" );
                    args.add( "1.3" );
                }

                // Also check for the encoding. Could have been set via the CompilerConfig
                // above, or also via the arguments explicitly. We need the charset for the
                // StandardJavaFileManager below.
                String encoding = null;
                Iterator<String> allArgs = args.iterator();
                while ( encoding == null && allArgs.hasNext() )
                {
                    String option = allArgs.next();
                    if ( "-encoding".equals( option ) && allArgs.hasNext() )
                    {
                        encoding = allArgs.next();
                    }
                }
                final Locale defaultLocale = Locale.getDefault();
                final List<CompilerMessage> messages = messageList;
                DiagnosticListener<? super JavaFileObject> messageCollector = new DiagnosticListener<JavaFileObject>()
                {

                    @Override
                    public void report( Diagnostic<? extends JavaFileObject> diagnostic )
                    {
                        // Convert to Plexus' CompilerMessage and append to messageList
                        String fileName = "Unknown source";
                        try
                        {
                            JavaFileObject file = diagnostic.getSource();
                            if ( file != null )
                            {
                                fileName = file.getName();
                            }
                        }
                        catch ( NullPointerException e )
                        {
                            // ECJ bug: diagnostic.getSource() may throw an NPE if there is no source
                        }
                        long startColumn = diagnostic.getColumnNumber();
                        // endColumn may be wrong if the endPosition is not on the same line.
                        long endColumn = startColumn + ( diagnostic.getEndPosition() - diagnostic.getStartPosition() );
                        CompilerMessage message = new CompilerMessage( fileName, convert( diagnostic.getKind() ),
                                                                       (int) diagnostic.getLineNumber(),
                                                                       (int) startColumn,
                                                                       (int) diagnostic.getLineNumber(),
                                                                       (int) endColumn,
                                                                       diagnostic.getMessage( defaultLocale ) );
                        messages.add( message );
                    }
                };
                Charset charset = null;
                if ( encoding != null )
                {
                    encoding = encoding.trim();
                    try
                    {
                        charset = Charset.forName( encoding );
                    }
                    catch ( IllegalCharsetNameException | UnsupportedCharsetException e )
                    {
                        getLogger().warn(
                            "ecj: invalid or unsupported character set '" + encoding + "', using default" );
                        // charset remains null
                    }
                }
                if ( charset == null )
                {
                    charset = Charset.defaultCharset();
                }
                if ( getLogger().isDebugEnabled() )
                {
                    getLogger().debug( "ecj: using character set " + charset.displayName() );
                    getLogger().debug( "ecj command line: " + args );
                    getLogger().debug( "ecj input source files: " + allSources );
                }

                try ( StandardJavaFileManager manager =
                    compiler.getStandardFileManager( messageCollector, defaultLocale, charset ) ) {
                    Iterable<? extends JavaFileObject> units = manager.getJavaFileObjectsFromStrings( allSources );
                    success = Boolean.TRUE.equals(
                        compiler.getTask( devNull, manager, messageCollector, args, null, units ).call() );
                }
                catch ( RuntimeException e )
                {
                    throw new EcjFailureException( e.getLocalizedMessage() );
                }
                getLogger().debug( sw.toString() );
            }
            else
            {
                // Use the BatchCompiler and send all errors to xml temp file.
                File errorF = null;
                try
                {
                    errorF = File.createTempFile( "ecjerr-", ".xml" );
                    getLogger().debug( "Using legacy BatchCompiler; error file " + errorF );

                    args.add( "-log" );
                    args.add( errorF.toString() );
                    args.addAll( allSources );

                    getLogger().debug( "ecj command line: " + args );

                    success = BatchCompiler.compile( args.toArray( new String[args.size()] ), devNull, devNull,
                                                     new CompilationProgress()
                                                     {
                                                         @Override
                                                         public void begin( int i )
                                                         {
                                                         }

                                                         @Override
                                                         public void done()
                                                         {
                                                         }

                                                         @Override
                                                         public boolean isCanceled()
                                                         {
                                                             return false;
                                                         }

                                                         @Override
                                                         public void setTaskName( String s )
                                                         {
                                                         }

                                                         @Override
                                                         public void worked( int i, int i1 )
                                                         {
                                                         }
                                                     } );
                    getLogger().debug( sw.toString() );

                    if ( errorF.length() < 80 )
                    {
                        throw new EcjFailureException( sw.toString() );
                    }
                    messageList = new EcjResponseParser().parse( errorF, errorsAsWarnings );
                }
                finally
                {
                    if ( null != errorF )
                    {
                        try
                        {
                            errorF.delete();
                        }
                        catch ( Exception x )
                        {
                        }
                    }
                }
            }
            boolean hasError = false;
            for ( CompilerMessage compilerMessage : messageList )
            {
                if ( compilerMessage.isError() )
                {
                    hasError = true;
                    break;
                }
            }
            if ( !hasError && !success && !errorsAsWarnings )
            {
                CompilerMessage.Kind kind =
                    errorsAsWarnings ? CompilerMessage.Kind.WARNING : CompilerMessage.Kind.ERROR;

                // -- Compiler reported failure but we do not seem to have one -> probable
                // exception
                CompilerMessage cm =
                    new CompilerMessage( "[ecj] The compiler reported an error but has not written it to its logging",
                                         kind );
                messageList.add( cm );
                hasError = true;

                // -- Try to find the actual message by reporting the last 5 lines as a message
                String stdout = getLastLines( sw.toString(), 5 );
                if ( stdout.length() > 0 )
                {
                    cm =
                        new CompilerMessage( "[ecj] The following line(s) might indicate the issue:\n" + stdout, kind );
                    messageList.add( cm );
                }
            }
            return new CompilerResult( !hasError || errorsAsWarnings, messageList );
        }
        catch ( EcjFailureException x )
        {
            throw x;
        }
        catch ( Exception x )
        {
            throw new RuntimeException( x ); // sigh
        }
    }

    static boolean processCustomArguments( CompilerConfiguration config, List<String> args )
    {
        boolean result = false;

        for ( Entry<String, String> entry : config.getCustomCompilerArgumentsEntries() )
        {
            String opt = entry.getKey();
            String optionValue = entry.getValue();

            // handle errorsAsWarnings options
            if ( opt.equals("errorsAsWarnings") || opt.equals("-errorsAsWarnings") )
            {
                result = true;
                continue;
            }

            if ( opt.equals( "-properties" ) )
            {
                if ( null != optionValue )
                {
                    File propFile = new File( optionValue );
                    if ( !propFile.exists() || !propFile.isFile() )
                    {
                        throw new IllegalArgumentException(
                                        "Properties file specified by -properties " + propFile + " does not exist" );
                    }
                }
            }

            //-- Write .class files even when error occur, but make sure methods with compile errors do abort when called
            if ( opt.equals( "-proceedOnError" ) )
            {
                // Generate a class file even with errors, but make methods with errors fail when called
                args.add( "-proceedOnError:Fatal" );
                continue;
            }

            /*
            * The compiler mojo makes quite a mess of passing arguments, depending on exactly WHICH
            * way is used to pass them. The method method using <compilerArguments> uses the tag names
            * of its contents to denote option names, and so the compiler mojo happily adds a '-' to
            * all of the names there and adds them to the "custom compiler arguments" map as a
            * name, value pair where the name always contains a single '-'. The Eclipse compiler (and
            * javac too, btw) has options with two dashes (like --add-modules for java 9). These cannot
            * be passed using a <compilerArguments> tag.
            *
            * The other method is to use <compilerArgs>, where each SINGLE argument needs to be passed
            * using an <arg>xxxx</arg> tag. In there the xxx is not manipulated by the compiler mojo, so
            * if it starts with a dash or more dashes these are perfectly preserved. But of course these
            * single <arg> entries are not a pair. So the compiler mojo adds them as pairs of (xxxx, null).
            *
            * We use that knowledge here: if a pair has a null value then do not mess up the key but
            * render it as a single value. This should ensure that something like:
            * <compilerArgs>
            *     <arg>--add-modules</arg>
            *     <arg>java.se.ee</arg>
            * </compilerArgs>
            *
            * is actually added to the command like as such.
            *
            * (btw: the above example will still give an error when using ecj <= 4.8M6:
            *      invalid module name: java.se.ee
            * but that seems to be a bug in ecj).
            */
            if ( null == optionValue )
            {
                //-- We have an option from compilerArgs: use the key as-is as a single option value
                args.add( opt );
            }
            else
            {
                if ( !opt.startsWith( "-" ) )
                {
                    opt = "-" + opt;
                }
                args.add( opt );
                args.add( optionValue );
            }
        }
        return result;
    }

    private static boolean haveSourceOrReleaseArgument( List<String> args )
    {
        Iterator<String> allArgs = args.iterator();
        while ( allArgs.hasNext() )
        {
            String option = allArgs.next();
            if ( ( "-source".equals( option ) || "--release".equals( option ) ) && allArgs.hasNext() )
            {
                return true;
            }
        }
        return false;
    }

    private JavaCompiler getEcj()
    {
        ServiceLoader<JavaCompiler> javaCompilerLoader =
            ServiceLoader.load( JavaCompiler.class, BatchCompiler.class.getClassLoader() );
        Class<?> c = null;
        try
        {
            c = Class.forName( "org.eclipse.jdt.internal.compiler.tool.EclipseCompiler", false,
                               BatchCompiler.class.getClassLoader() );
        }
        catch ( ClassNotFoundException e )
        {
            // Ignore
        }
        if ( c != null )
        {
            for ( JavaCompiler javaCompiler : javaCompilerLoader )
            {
                if ( c.isInstance( javaCompiler ) )
                {
                    return javaCompiler;
                }
            }
        }
        getLogger().debug( "Cannot find org.eclipse.jdt.internal.compiler.tool.EclipseCompiler" );
        return null;
    }

    private void addExtraSources( File dir, List<String> allSources )
    {
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir( dir.getAbsolutePath() );
        scanner.setIncludes( new String[]{ "**/*.java" } );
        scanner.scan();
        for ( String file : scanner.getIncludedFiles() )
        {
            allSources.add( new File( dir, file ).getAbsolutePath() );
        }
    }

    private CompilerMessage.Kind convert( Diagnostic.Kind kind )
    {
        if ( kind == null )
        {
            return CompilerMessage.Kind.OTHER;
        }
        switch ( kind )
        {
            case ERROR:
                return errorsAsWarnings ? CompilerMessage.Kind.WARNING : CompilerMessage.Kind.ERROR;
            case WARNING:
                return CompilerMessage.Kind.WARNING;
            case MANDATORY_WARNING:
                return CompilerMessage.Kind.MANDATORY_WARNING;
            case NOTE:
                return CompilerMessage.Kind.NOTE;
            case OTHER:
            default:
                return CompilerMessage.Kind.OTHER;
        }
    }

    private String getLastLines( String text, int lines )
    {
        List<String> lineList = new ArrayList<>();
        text = text.replace( "\r\n", "\n" );
        text = text.replace( "\r", "\n" );            // make sure eoln is \n

        int index = text.length();
        while ( index > 0 )
        {
            int before = text.lastIndexOf( '\n', index - 1 );

            if ( before + 1 < index )
            {                        // Non empty line?
                lineList.add( text.substring( before + 1, index ) );
                lines--;
                if ( lines <= 0 )
                {
                    break;
                }
            }

            index = before;
        }

        StringBuilder sb = new StringBuilder();
        for ( int i = lineList.size() - 1; i >= 0; i-- )
        {
            String s = lineList.get( i );
            sb.append( s );
            sb.append( System.getProperty( "line.separator" ) );        // 8-/
        }
        return sb.toString();
    }

    static private void append( StringBuilder warns, String s )
    {
        if ( warns.length() > 0 )
        {
            warns.append( ',' );
        }
        warns.append( s );
    }

    private boolean isPreJava1_6( CompilerConfiguration config )
    {
        String s = config.getSourceVersion();
        if ( s == null )
        {
            //now return true, as the 1.6 version is not the default - 1.4 is.
            return true;
        }
        return s.startsWith( "1.5" ) || s.startsWith( "1.4" ) || s.startsWith( "1.3" ) || s.startsWith( "1.2" )
            || s.startsWith( "1.1" ) || s.startsWith( "1.0" );
    }

    @Override
    public String[] createCommandLine( CompilerConfiguration config )
        throws CompilerException
    {
        return null;
    }

    @Override
    public boolean supportsIncrementalCompilation()
    {
        return true;
    }

    /**
     * Change any Maven Java version number to ECJ's version number. Do not check the validity
     * of the version: the compiler does that nicely, and this allows compiler updates without
     * changing the compiler plugin. This is important with the half year release cycle for Java.
     */
    private String decodeVersion( String versionSpec )
    {
        if ( StringUtils.isEmpty( versionSpec ) )
        {
            return null;
        }

        if ( versionSpec.equals( "1.9" ) )
        {
            getLogger().warn( "Version 9 should be specified as 9, not 1.9" );
            return "9";
        }
        return versionSpec;
    }
}
