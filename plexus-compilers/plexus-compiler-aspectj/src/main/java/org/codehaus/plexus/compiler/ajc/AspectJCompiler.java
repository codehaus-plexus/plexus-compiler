package org.codehaus.plexus.compiler.ajc;

import org.aspectj.ajdt.ajc.BuildArgParser;
import org.aspectj.ajdt.internal.core.builder.AjBuildConfig;
import org.aspectj.ajdt.internal.core.builder.AjBuildManager;
import org.aspectj.bridge.AbortException;
import org.aspectj.bridge.IMessage;
import org.aspectj.bridge.ISourceLocation;
import org.aspectj.bridge.MessageHandler;
import org.aspectj.org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.aspectj.tools.ajc.Main;
import org.codehaus.plexus.compiler.AbstractCompiler;
import org.codehaus.plexus.compiler.Compiler;
import org.codehaus.plexus.compiler.CompilerConfiguration;
import org.codehaus.plexus.compiler.CompilerException;
import org.codehaus.plexus.compiler.CompilerMessage;
import org.codehaus.plexus.compiler.CompilerOutputStyle;
import org.codehaus.plexus.compiler.CompilerResult;
import org.codehaus.plexus.component.annotations.Component;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * Options
 * </p>
 * <p>
 * -injars JarList
 * </p>
 * <p>
 * Accept as source bytecode any .class files inside the specified .jar files. The output will include these
 * classes, possibly as woven with any applicable aspects. JarList, like classpath, is a single argument
 * containing a list of paths to jar files, delimited by the platform- specific classpath delimiter.
 * </p>
 * <p>
 * -aspectpath JarList
 * </p>
 * <p>
 * Weave binary aspects from JarList zip files into all sources. The aspects should have been output by
 * the same version of the compiler. To run the output classes requires putting all the aspectpath entries on
 * the run classpath. JarList, like classpath, is a single argument containing a list of paths to jar files,
 * delimited by the platform- specific classpath delimiter.
 * </p>
 * <p>
 * -argfile File
 * </p>
 * <p>
 * The file is a line-delimited list of arguments. These arguments are inserted into the argument list.
 * </p>
 * <p>
 * -outjar output.jar
 * </p>
 * <p>
 * Put output classes in zip file output.jar.
 * </p>
 * <p>
 * -incremental
 * </p>
 * <p>
 * Run the compiler continuously. After the initial compilation, the compiler will wait to recompile until it
 * reads a newline from the standard input, and will quit when it reads a 'q'. It will only recompile necessary
 * components, so a recompile should be much faster than doing a second compile. This requires -sourceroots.
 * </p>
 * <p>
 * -sourceroots DirPaths
 * </p>
 * <p>
 * Find and build all .java or .aj source files under any directory listed in DirPaths. DirPaths, like
 * classpath, is a single argument containing a list of paths to directories, delimited by the platform-
 * specific classpath delimiter. Required by -incremental.
 * </p>
 * <p>
 * -emacssym
 * </p>
 * <p>
 * Generate .ajesym symbol files for emacs support
 * </p>
 * <p>
 * -Xlint
 * </p>
 * <p>
 * Same as -Xlint:warning (enabled by default)
 * </p>
 * <p>
 * -Xlint:{level}
 * </p>
 * <p>
 * Set default level for messages about potential programming mistakes in crosscutting code. {level} may be
 * ignore, warning, or error. This overrides entries in org/aspectj/weaver/XlintDefault.properties from
 * aspectjtools.jar, but does not override levels set using the -Xlintfile option.
 * </p>
 * <p>
 * -Xlintfile PropertyFile
 * </p>
 * <p>
 * Specify properties file to set levels for specific crosscutting messages. PropertyFile is a path to a
 * Java .properties file that takes the same property names and values as
 * org/aspectj/weaver/XlintDefault.properties from aspectjtools.jar, which it also overrides.
 * -help
 * </p>
 * <p>
 * Emit information on compiler options and usage
 * </p>
 * <p>
 * -version
 * </p>
 * <p>
 * Emit the version of the AspectJ compiler
 * </p>
 * <p>
 * -classpath Path
 * </p>
 * <p>
 * Specify where to find user class files. Path is a single argument containing a list of paths to zip files
 * or directories, delimited by the platform-specific path delimiter.
 * </p>
 * <p>
 * -bootclasspath Path
 * </p>
 * <p>
 * Override location of VM's bootclasspath for purposes of evaluating types when compiling. Path is a single
 * argument containing a list of paths to zip files or directories, delimited by the platform-specific path
 * delimiter.
 * </p>
 * <p>
 * -extdirs Path
 * </p>
 * <p>
 * Override location of VM's extension directories for purposes of evaluating types when compiling. Path is
 * a single argument containing a list of paths to directories, delimited by the platform-specific path
 * delimiter.
 * </p>
 * <p>
 * -d Directory
 * </p>
 * <p>
 * Specify where to place generated .class files. If not specified, Directory defaults to the current
 * working dir.
 * </p>
 * <p>
 * -target [1.1|1.2]
 * </p>
 * <p>
 * Specify classfile target setting (1.1 or 1.2, default is 1.1)
 * </p>
 * <p>
 * -1.3
 * </p>
 * <p>
 * Set compliance level to 1.3 (default)
 * -1.4
 * </p>
 * <p>
 * Set compliance level to 1.4
 * -source [1.3|1.4]
 * </p>
 * <p>
 * Toggle assertions (1.3 or 1.4, default is 1.3 in -1.3 mode and 1.4 in -1.4 mode). When using -source 1.3,
 * an assert() statement valid under Java 1.4 will result in a compiler error. When using -source 1.4, treat
 * assert as a keyword and implement assertions according to the 1.4 language spec.
 * </p>
 * <p>
 * -nowarn
 * </p>
 * <p>
 * Emit no warnings (equivalent to '-warn:none') This does not suppress messages generated by declare warning
 * or Xlint.
 * </p>
 * <p>
 * -warn: items
 * </p>
 * <p>
 * Emit warnings for any instances of the comma-delimited list of questionable code
 * (eg '-warn:unusedLocals,deprecation'):
 * </p>
 * <p>
 * constructorName        method with constructor name
 * packageDefaultMethod   attempt to override package-default method
 * deprecation            usage of deprecated type or member
 * maskedCatchBlocks      hidden catch block
 * unusedLocals           local variable never read
 * unusedArguments        method argument never read
 * unusedImports          import statement not used by code in file
 * none                   suppress all compiler warnings
 * </p>
 * <p>
 * -warn:none does not suppress messages generated by declare warning or Xlint.
 * </p>
 * <p>
 * -deprecation
 * </p>
 * <p>
 * Same as -warn:deprecation
 * </p>
 * <p>
 * -noImportError
 * </p>
 * <p>
 * Emit no errors for unresolved imports
 * </p>
 * <p>
 * -proceedOnError
 * </p>
 * <p>
 * Keep compiling after error, dumping class files with problem methods
 * </p>
 * <p>
 * -g:[lines,vars,source]
 * </p>
 * <p>
 * debug attributes level, that may take three forms:
 * </p>
 * <p>
 * -g         all debug info ('-g:lines,vars,source')
 * -g:none    no debug info
 * -g:{items} debug info for any/all of [lines, vars, source], e.g.,
 * -g:lines,source
 * </p>
 * <p>
 * -preserveAllLocals
 * </p>
 * <p>
 * Preserve all local variables during code generation (to facilitate debugging).
 * </p>
 * <p>
 * -referenceInfo
 * </p>
 * <p>
 * Compute reference information.
 * </p>
 * <p>
 * -encoding format
 * </p>
 * <p>
 * Specify default source encoding format. Specify custom encoding on a per file basis by suffixing each
 * input source file/folder name with '[encoding]'.
 * </p>
 * <p>
 * -verbose
 * </p>
 * <p>
 * Emit messages about accessed/processed compilation units
 * </p>
 * <p>
 * -log file Specify a log file for compiler messages.
 * </p>
 * <p>
 * -progress Show progress (requires -log mode).
 * </p>
 * <p>
 * -time Display speed information.
 * </p>
 * <p>
 * -noExit Do not call System.exit(n) at end of compilation (n=0 if no error)
 * </p>
 * <p>
 * -repeat N Repeat compilation process N times (typically to do performance analysis).
 * </p>
 * <p>
 * -Xnoweave (Experimental) produce unwoven class files for input using -injars.
 * </p>
 * <p>
 * -Xnoinline (Experimental) do not inline around advice
 * </p>
 * <p>
 * -XincrementalFile file
 * </p>
 * <p>
 * (Experimental) This works like incremental mode, but using a file rather than standard input to control
 * the compiler. It will recompile each time file is changed and and halt when file is deleted.
 * </p>
 * <p>
 * -XserializableAspects (Experimental) Normally it is an error to declare aspects Serializable. This option removes that restriction.
 * </p>
 *
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 */
@Component( role = Compiler.class, hint = "aspectj")
public class AspectJCompiler
    extends AbstractCompiler
{

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public AspectJCompiler()
    {
        super( CompilerOutputStyle.ONE_OUTPUT_FILE_PER_INPUT_FILE, ".java", ".class", null );
    }

    public CompilerResult performCompile( CompilerConfiguration config )
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
            return new CompilerResult();
        }

        System.out.println(
            "Compiling " + sourceFiles.length + " " + "source file" + ( sourceFiles.length == 1 ? "" : "s" ) + " to "
                + destinationDir.getAbsolutePath() );

        //        String[] args = buildCompilerArguments( config, sourceFiles );
        AjBuildConfig buildConfig = buildCompilerConfig( config );
        return new CompilerResult().compilerMessages( compileInProcess( buildConfig ) );
    }

    private static class AspectJMessagePrinter extends Main.MessagePrinter
    {
        public AspectJMessagePrinter( boolean verbose )
        {
            super( verbose );
        }
    }

    private AjBuildConfig buildCompilerConfig( CompilerConfiguration config )
        throws CompilerException
    {
        AjBuildConfig buildConfig = new AjBuildConfig(new BuildArgParser(new AspectJMessagePrinter(config.isVerbose())));
        buildConfig.setIncrementalMode( false );

        String[] files = getSourceFiles( config );
        if ( files != null )
        {
            buildConfig.setFiles( buildFileList( Arrays.asList( files ) ) );
        }

        setSourceVersion( buildConfig, config.getSourceVersion() );

        if ( config.isDebug() )
        {
            buildConfig.getOptions().produceDebugAttributes =
                ClassFileConstants.ATTR_SOURCE + ClassFileConstants.ATTR_LINES + ClassFileConstants.ATTR_VARS;
        }

        Map<String, String> javaOpts = config.getCustomCompilerArguments();
        if ( javaOpts != null && !javaOpts.isEmpty() )
        {
            // TODO support customCompilerArguments
            // buildConfig.setJavaOptions( javaOpts );
        }

        List<String> cp = new LinkedList<String>( config.getClasspathEntries() );

        File javaHomeDir = new File( System.getProperty( "java.home" ) );
        File[] jars = new File( javaHomeDir, "lib" ).listFiles();
        if ( jars != null )
        {
            for ( File jar : jars )
            {
                if ( jar.getName().endsWith( ".jar" ) || jar.getName().endsWith( ".zip" ) )
                {
                    cp.add( 0, jar.getAbsolutePath() );
                }
            }
        }
        jars = new File( javaHomeDir, "../Classes" ).listFiles();
        if ( jars != null )
        {
            for ( File jar : jars )
            {
                if ( jar.getName().endsWith( ".jar" ) || jar.getName().endsWith( ".zip" ) )
                {
                    cp.add( 0, jar.getAbsolutePath() );
                }
            }
        }

        checkForAspectJRT( cp );
        if ( cp != null && !cp.isEmpty() )
        {
            List<String> elements = new ArrayList<String>( cp.size() );
            for ( String path : cp )
            {
                elements.add( ( new File( path ) ).getAbsolutePath() );
            }

            buildConfig.setClasspath( elements );
        }

        String outputLocation = config.getOutputLocation();
        if ( outputLocation != null )
        {
            File outDir = new File( outputLocation );
            if ( !outDir.exists() )
            {
                outDir.mkdirs();
            }

            buildConfig.setOutputDir( outDir );
        }

        if ( config instanceof AspectJCompilerConfiguration )
        {
            AspectJCompilerConfiguration ajCfg = (AspectJCompilerConfiguration) config;

            Map<String, File> sourcePathResources = ajCfg.getSourcePathResources();
            if ( sourcePathResources != null && !sourcePathResources.isEmpty() )
            {
                buildConfig.setSourcePathResources( sourcePathResources );
            }

            Map<String, String> ajOptions = ajCfg.getAJOptions();
            if ( ajOptions != null && !ajOptions.isEmpty() )
            {
                // TODO not supported
                //buildConfig.setAjOptions( ajCfg.getAJOptions() );
            }

            List<File> aspectPath = buildFileList( ajCfg.getAspectPath() );
            if ( aspectPath != null && !aspectPath.isEmpty() )
            {
                buildConfig.setAspectpath( buildFileList( ajCfg.getAspectPath() ) );
            }

            List<File> inJars = buildFileList( ajCfg.getInJars() );
            if ( inJars != null && !inJars.isEmpty() )
            {
                buildConfig.setInJars( buildFileList( ajCfg.getInJars() ) );
            }

            List<File> inPaths = buildFileList( ajCfg.getInPath() );
            if ( inPaths != null && !inPaths.isEmpty() )
            {
                buildConfig.setInPath( buildFileList( ajCfg.getInPath() ) );
            }

            String outJar = ajCfg.getOutputJar();
            if ( outJar != null )
            {
                buildConfig.setOutputJar( new File( ajCfg.getOutputJar() ) );
            }
        }

        return buildConfig;
    }

    private List<CompilerMessage> compileInProcess( AjBuildConfig buildConfig )
        throws CompilerException
    {

        MessageHandler messageHandler = new MessageHandler();

        AjBuildManager manager = new AjBuildManager( messageHandler );

        try
        {
            manager.batchBuild( buildConfig, messageHandler );
        }
        catch ( AbortException e )
        {
            throw new CompilerException( "Unknown error while compiling", e );
        }
        catch ( IOException e )
        {
            throw new CompilerException( "Unknown error while compiling", e );
        }

        // We need the location of the maven so we have a couple of options
        // here.
        //
        // The aspectjrt jar is something this component needs to function so we
        // can either
        // bake it into the plugin and retrieve it somehow or use a system
        // property or we
        // could pass in a set of parameters in a Map.

        boolean errors = messageHandler.hasAnyMessage( IMessage.ERROR, true );

        List<CompilerMessage> messages = new ArrayList<CompilerMessage>();
        if ( errors )
        {
            IMessage[] errorMessages = messageHandler.getMessages( IMessage.ERROR, true );

            for ( IMessage m : errorMessages )
            {
                ISourceLocation sourceLocation = m.getSourceLocation();
                CompilerMessage error;

                if ( sourceLocation == null )
                {
                    error = new CompilerMessage( m.getMessage(), true );
                }
                else
                {
                    error =
                        new CompilerMessage( sourceLocation.getSourceFile().getPath(), true, sourceLocation.getLine(),
                                             sourceLocation.getColumn(), sourceLocation.getEndLine(),
                                             sourceLocation.getColumn(), m.getMessage() );
                }
                messages.add( error );
            }
        }

        return messages;
    }

    private void checkForAspectJRT( List<String> cp )
    {
        if ( cp == null || cp.isEmpty() )
        {
            throw new IllegalStateException( "AspectJ Runtime not found in supplied classpath" );
        }
        else
        {
            try
            {
                URL[] urls = new URL[cp.size()];
                for ( int i = 0; i < urls.length; i++ )
                {
                    urls[i] = ( new File( cp.get( i ) ) ).toURL();
                }

                URLClassLoader cloader = new URLClassLoader( urls );

                cloader.loadClass( "org.aspectj.lang.JoinPoint" );
            }
            catch ( MalformedURLException e )
            {
                throw new IllegalArgumentException( "Invalid classpath entry" );
            }
            catch ( ClassNotFoundException e )
            {
                throw new IllegalStateException( "AspectJ Runtime not found in supplied classpath" );
            }
        }
    }

    private List<File> buildFileList( List<String> locations )
    {
        List<File> fileList = new LinkedList<File>();
        for ( String location : locations )
        {
            fileList.add( new File( location ) );
        }

        return fileList;
    }

    /**
     * Set the source version in ajc compiler
     *
     * @param buildConfig
     * @param sourceVersion
     */
    private void setSourceVersion( AjBuildConfig buildConfig, String sourceVersion )
        throws CompilerException
    {
        if ( "11".equals( sourceVersion ) )
        {
            buildConfig.getOptions().sourceLevel = ClassFileConstants.JDK11;
        }
        else if ( "10".equals( sourceVersion ) )
        {
            buildConfig.getOptions().sourceLevel = ClassFileConstants.JDK10;
        }
        else if ( "9".equals( sourceVersion ) )
        {
            buildConfig.getOptions().sourceLevel = ClassFileConstants.JDK9;
        }
        else if ( "1.9".equals( sourceVersion ) )
        {
            buildConfig.getOptions().sourceLevel = ClassFileConstants.JDK9;
        }
        else if ( "1.8".equals( sourceVersion ) )
        {
            buildConfig.getOptions().sourceLevel = ClassFileConstants.JDK1_8;
        }
        else if ( "1.7".equals( sourceVersion ) )
        {
            buildConfig.getOptions().sourceLevel = ClassFileConstants.JDK1_7;
        }
        else if ( "1.6".equals( sourceVersion ) )
        {
            buildConfig.getOptions().sourceLevel = ClassFileConstants.JDK1_6;
        }
        else if ( "1.5".equals( sourceVersion ) )
        {
            buildConfig.getOptions().sourceLevel = ClassFileConstants.JDK1_5;
        }
        else if ( "5.0".equals( sourceVersion ) )
        {
            buildConfig.getOptions().sourceLevel = ClassFileConstants.JDK1_5;
        }
        else if ( "1.4".equals( sourceVersion ) )
        {
            buildConfig.getOptions().sourceLevel = ClassFileConstants.JDK1_4;
        }
        else if ( "1.3".equals( sourceVersion ) )
        {
            buildConfig.getOptions().sourceLevel = ClassFileConstants.JDK1_3;
        }
        else if ( "1.2".equals( sourceVersion ) )
        {
            buildConfig.getOptions().sourceLevel = ClassFileConstants.JDK1_2;
        }
        else if ( "1.1".equals( sourceVersion ) )
        {
            buildConfig.getOptions().sourceLevel = ClassFileConstants.JDK1_1;
        }
        else if ( sourceVersion == null || sourceVersion.length() <= 0 )
        {
            buildConfig.getOptions().sourceLevel = ClassFileConstants.JDK1_3;
        }
        else
        {
            throw new CompilerException( "The source version was not recognized: " + sourceVersion );
        }
    }

    /**
     * @return null
     */
    public String[] createCommandLine( CompilerConfiguration config )
        throws CompilerException
    {
        return null;
    }

}
