package org.codehaus.plexus.compiler.ajc;

import org.aspectj.ajdt.internal.core.builder.AjBuildConfig;
import org.aspectj.ajdt.internal.core.builder.AjBuildManager;
import org.aspectj.bridge.CountingMessageHandler;
import org.aspectj.bridge.IMessage;
import org.aspectj.bridge.IMessageHolder;
import org.aspectj.bridge.MessageHandler;
import org.aspectj.tools.ajc.Main;
import org.codehaus.plexus.compiler.AbstractCompiler;
import org.codehaus.plexus.compiler.CompilerConfiguration;
import org.codehaus.plexus.compiler.CompilerError;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Options
 *
 * -injars JarList
 *
 *     Accept as source bytecode any .class files inside the specified .jar files. The output will include these
 *     classes, possibly as woven with any applicable aspects. JarList, like classpath, is a single argument
 *     containing a list of paths to jar files, delimited by the platform- specific classpath delimiter.
 *
 * -aspectpath JarList
 *
 *     Weave binary aspects from JarList zip files into all sources. The aspects should have been output by
 *     the same version of the compiler. To run the output classes requires putting all the aspectpath entries on
 *     the run classpath. JarList, like classpath, is a single argument containing a list of paths to jar files,
 *     delimited by the platform- specific classpath delimiter.
 *
 * -argfile File
 *
 *     The file is a line-delimited list of arguments. These arguments are inserted into the argument list.
 *
 * -outjar output.jar
 *
 *     Put output classes in zip file output.jar.
 *
 * -incremental
 *
 *     Run the compiler continuously. After the initial compilation, the compiler will wait to recompile until it
 *     reads a newline from the standard input, and will quit when it reads a 'q'. It will only recompile necessary
 *     components, so a recompile should be much faster than doing a second compile. This requires -sourceroots.
 *
 * -sourceroots DirPaths
 *
 *     Find and build all .java or .aj source files under any directory listed in DirPaths. DirPaths, like
 *     classpath, is a single argument containing a list of paths to directories, delimited by the platform-
 *     specific classpath delimiter. Required by -incremental.
 *
 * -emacssym
 *
 *     Generate .ajesym symbol files for emacs support
 *
 * -Xlint
 *
 *     Same as -Xlint:warning (enabled by default)
 *
 * -Xlint:{level}
 *
 *     Set default level for messages about potential programming mistakes in crosscutting code. {level} may be
 *     ignore, warning, or error. This overrides entries in org/aspectj/weaver/XlintDefault.properties from
 *     aspectjtools.jar, but does not override levels set using the -Xlintfile option.
 *
 * -Xlintfile PropertyFile
 *
 *     Specify properties file to set levels for specific crosscutting messages. PropertyFile is a path to a
 *     Java .properties file that takes the same property names and values as
 *     org/aspectj/weaver/XlintDefault.properties from aspectjtools.jar, which it also overrides.
 * -help
 *
 *     Emit information on compiler options and usage
 *
 * -version
 *
 *     Emit the version of the AspectJ compiler
 *
 * -classpath Path
 *
 *     Specify where to find user class files. Path is a single argument containing a list of paths to zip files
 *     or directories, delimited by the platform-specific path delimiter.
 *
 * -bootclasspath Path
 *
 *     Override location of VM's bootclasspath for purposes of evaluating types when compiling. Path is a single
 *     argument containing a list of paths to zip files or directories, delimited by the platform-specific path
 *     delimiter.
 *
 * -extdirs Path
 *
 *     Override location of VM's extension directories for purposes of evaluating types when compiling. Path is
 *     a single argument containing a list of paths to directories, delimited by the platform-specific path
 *     delimiter.
 *
 * -d Directory
 *
 *     Specify where to place generated .class files. If not specified, Directory defaults to the current
 *     working dir.
 *
 * -target [1.1|1.2]
 *
 *     Specify classfile target setting (1.1 or 1.2, default is 1.1)
 *
 * -1.3
 *
 *     Set compliance level to 1.3 (default)
 * -1.4
 *
 *     Set compliance level to 1.4
 * -source [1.3|1.4]
 *
 *     Toggle assertions (1.3 or 1.4, default is 1.3 in -1.3 mode and 1.4 in -1.4 mode). When using -source 1.3,
 *     an assert() statement valid under Java 1.4 will result in a compiler error. When using -source 1.4, treat
 *     assert as a keyword and implement assertions according to the 1.4 language spec.
 *
 * -nowarn
 *
 *     Emit no warnings (equivalent to '-warn:none') This does not suppress messages generated by declare warning
 *     or Xlint.
 *
 * -warn: items
 *
 *     Emit warnings for any instances of the comma-delimited list of questionable code
 *     (eg '-warn:unusedLocals,deprecation'):
 *
 *     constructorName        method with constructor name
 *     packageDefaultMethod   attempt to override package-default method
 *     deprecation            usage of deprecated type or member
 *     maskedCatchBlocks      hidden catch block
 *     unusedLocals           local variable never read
 *     unusedArguments        method argument never read
 *     unusedImports          import statement not used by code in file
 *     none                   suppress all compiler warnings
 *
 *
 *     -warn:none does not suppress messages generated by declare warning or Xlint.
 *
 * -deprecation
 *
 *     Same as -warn:deprecation
 *
 * -noImportError
 *
 *     Emit no errors for unresolved imports
 *
 * -proceedOnError
 *
 *     Keep compiling after error, dumping class files with problem methods
 *
 * -g:[lines,vars,source]
 *
 *     debug attributes level, that may take three forms:
 *
 *     -g         all debug info ('-g:lines,vars,source')
 *     -g:none    no debug info
 *     -g:{items} debug info for any/all of [lines, vars, source], e.g.,
 *                -g:lines,source
 *
 *
 * -preserveAllLocals
 *
 *     Preserve all local variables during code generation (to facilitate debugging).
 *
 * -referenceInfo
 *
 *     Compute reference information.
 *
 * -encoding format
 *
 *     Specify default source encoding format. Specify custom encoding on a per file basis by suffixing each
 *     input source file/folder name with '[encoding]'.
 *
 * -verbose
 *
 *     Emit messages about accessed/processed compilation units
 *
 * -log file
 *
 *     Specify a log file for compiler messages.
 * -progress
 *
 *     Show progress (requires -log mode).
 * -time
 *
 *     Display speed information.
 * -noExit
 *
 *     Do not call System.exit(n) at end of compilation (n=0 if no error)
 * -repeat N
 *
 *     Repeat compilation process N times (typically to do performance analysis).
 * -Xnoweave
 *
 *     (Experimental) produce unwoven class files for input using -injars.
 * -Xnoinline
 *
 *     (Experimental) do not inline around advice
 * -XincrementalFile file
 *
 *     (Experimental) This works like incremental mode, but using a file rather than standard input to control
 *     the compiler. It will recompile each time file is changed and and halt when file is deleted.
 *
 * -XserializableAspects
 *
 *     (Experimental) Normally it is an error to declare aspects Serializable. This option removes that restriction.
 *
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 *
 * @version $Id$
 */
public class AspectJCompiler
    extends AbstractCompiler
    implements Initializable
{

    /** Aspjectj compiler */
    private Main compiler;

    /** @see org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable#initialize */
    public void initialize() throws Exception
    {
        compiler = new Main();
    }

    public List compile( CompilerConfiguration config ) throws Exception
    {
        List messages = new ArrayList();

        AjBuildConfig buildConfig = new AjBuildConfig();
        buildConfig.setIncrementalMode( false );

        String[] files = getSourceFiles(config);
        if ( files != null )
        {
            buildConfig.setFiles( buildFileList( Arrays.asList(files) ) );
        }

        Map javaOpts = config.getCompilerOptions();
        if ( javaOpts != null && !javaOpts.isEmpty() )
        {
            buildConfig.setJavaOptions( javaOpts );
        }

        List cp = new LinkedList( config.getClasspathEntries() );
        cp.add( 0, System.getProperty("java.home") + "/lib/rt.jar" );
        
        checkForAspectJRT( cp );
        if ( cp != null && !cp.isEmpty() )
        {
            buildConfig.setClasspath( cp );
        }

        String outputLocation = config.getOutputLocation();
        if ( outputLocation != null )
        {
            File outDir = new File( outputLocation );
            if( !outDir.exists() ) {
                outDir.mkdirs();
            }
            
            buildConfig.setOutputDir( outDir );
        }

        if ( config instanceof AspectJCompilerConfiguration )
        {
            AspectJCompilerConfiguration ajCfg = (AspectJCompilerConfiguration) config;

            Map sourcePathResources = ajCfg.getSourcePathResources();
            if ( sourcePathResources != null && !sourcePathResources.isEmpty() )
            {
                buildConfig.setSourcePathResources( sourcePathResources );
            }

            Map ajOptions = ajCfg.getAJOptions();
            if ( ajOptions != null && !ajOptions.isEmpty() )
            {
                buildConfig.setAjOptions( ajCfg.getAJOptions() );
            }

            List aspectPath = buildFileList( ajCfg.getAspectPath() );
            if ( aspectPath != null && !aspectPath.isEmpty() )
            {
                buildConfig.setAspectpath( buildFileList( ajCfg.getAspectPath() ) );
            }

            List inJars = buildFileList( ajCfg.getInJars() );
            if ( inJars != null && !inJars.isEmpty() )
            {
                buildConfig.setInJars( buildFileList( ajCfg.getInJars() ) );
            }

            List inPaths = buildFileList( ajCfg.getInPath() );
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

        MessageHandler messageHandler = new MessageHandler();

        AjBuildManager manager = new AjBuildManager( messageHandler );

        boolean success = manager.batchBuild( buildConfig, messageHandler );
        
        // We need the location of the maven so we have a couple of options
        // here.
        //
        // The aspectjrt jar is something this component needs to function so we
        // can either
        // bake it into the plugin and retrieve it somehow or use a system
        // property or we
        // could pass in a set of parameters in a Map.
        
        boolean errors = messageHandler.hasAnyMessage( IMessage.ERROR, true );

        if ( errors )
        {
            IMessage[] errorMessages = messageHandler.getMessages( IMessage.ERROR, true );

            for ( int i = 0; i < errorMessages.length; i++ )
            {
                IMessage m = errorMessages[i];

                messages.add( new CompilerError( m.getSourceLocation().getSourceFile().getPath(),
                                                 true, m.getSourceLocation().getLine(),
                                                 m.getSourceLocation().getColumn(),
                                                 m.getSourceLocation().getEndLine(),
                                                 m.getSourceLocation().getColumn(), m.getMessage() ) );
            }
        }

        return messages;
    }

    private void checkForAspectJRT( List cp )
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
                    urls[i] = new File((String)cp.get( i )).toURL();
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

    private List buildFileList( List locations )
    {
        List fileList = new LinkedList();
        for ( Iterator it = locations.iterator(); it.hasNext(); )
        {
            String location = (String) it.next();
            fileList.add( new File( location ) );
        }
        
        return fileList;
    }
    
}