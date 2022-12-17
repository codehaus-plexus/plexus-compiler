package org.codehaus.plexus.compiler;

/**
 * The MIT License
 *
 * Copyright (c) 2004, The Codehaus
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

import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author jdcasey
 */
public class CompilerConfiguration
{
    private String outputLocation;

    private List<String> classpathEntries = new LinkedList<>();

    private List<String> modulepathEntries = new LinkedList<>();

    // ----------------------------------------------------------------------
    // Source Files
    // ----------------------------------------------------------------------

    private Set<File> sourceFiles = new HashSet<>();

    private List<String> sourceLocations = new LinkedList<>();

    private Set<String> includes = new HashSet<>();

    private Set<String> excludes = new HashSet<>();

    // ----------------------------------------------------------------------
    // Compiler Settings
    // ----------------------------------------------------------------------

    private boolean debug;

    private String debugLevel;

    private boolean showWarnings = true;

    private String warnings;
    
    private boolean showLint;

    /**
     * -Werror argument as supported since Java 1.7
     */
    private boolean failOnWarning;

    private boolean showDeprecation;

    private String sourceVersion;

    private String targetVersion;

    /**
     * value of -release parameter in java 9+
     */
    private String releaseVersion;

    private String sourceEncoding;

    /**
     * value of --module-version parameter in java 9+
     */
    private String moduleVersion;

    private Collection<Map.Entry<String,String>> customCompilerArguments = new ArrayList<>();

    private boolean fork;

    private boolean optimize;

    private String meminitial;

    private String maxmem;

    private String executable;

    private File workingDirectory;

    private String compilerVersion;

    private boolean verbose = false;

    /**
     * @since 2.8.2
     */
    private boolean parameters;

    /**
     * A build temporary directory, eg target/.
     * <p/>
     * Used by the compiler implementation to put temporary files.
     */
    private File buildDirectory;

    /**
     * Used to control the name of the output file when compiling a set of
     * sources to a single file.
     */
    private String outputFileName;

    /**
     * in jdk 1.6+, used to hold value of the -s path parameter.
     */
    private File generatedSourcesDirectory;

    /**
     * value of the -proc parameter in jdk 1.6+
     */
    private String proc;

    /**
     * -processor parameters in jdk 1.6+
     */
    private String[] annotationProcessors;

    /**
     * -processorpath parameter in jdk 1.6+. If specified, annotation processors are only searched in the processor
     * path. Otherwise they are searched in the classpath.
     */
    private List<String> processorPathEntries;

    /**
     * --processor-module-path parameter in jdk 9+. If specified, annotation processors are only searched in the processor
     * path. Otherwise they are searched in the modulepath.
     */
    private List<String> processorModulePathEntries;

    /**
     * default value {@link CompilerReuseStrategy#ReuseCreated}
     *
     * @since 1.9
     */
    private CompilerReuseStrategy compilerReuseStrategy = CompilerReuseStrategy.ReuseCreated;

    /**
     * force usage of old JavacCompiler even if javax.tools is detected
     * @since 2.0
     */
    private boolean forceJavacCompilerUse=false;

    /**
     * force a different of the debug file containing the forked command run (such javac.sh)
     * @since 2.9.1
     */
    private String debugFileName;

    /**
     * configure <code>--enable-preview</code> of java compiler
     */
    private boolean enablePreview;

    /** value of <code>-implicit:</code> of java compiler */
    private String implicitOption;

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public void setOutputLocation( String outputLocation )
    {
        this.outputLocation = outputLocation;
    }

    public String getOutputLocation()
    {
        return outputLocation;
    }

    // ----------------------------------------------------------------------
    // Class path
    // ----------------------------------------------------------------------

    public void addClasspathEntry( String classpathEntry )
    {
        classpathEntries.add( classpathEntry );
    }

    public void setClasspathEntries( List<String> classpathEntries )
    {
        if ( classpathEntries == null )
        {
            this.classpathEntries = Collections.emptyList();
        }
        else
        {
            this.classpathEntries = new LinkedList<>( classpathEntries );
        }
    }

    public List<String> getClasspathEntries()
    {
        return Collections.unmodifiableList( classpathEntries );
    }

    // ----------------------------------------------------------------------
    // Module path
    // ----------------------------------------------------------------------

    public void addModulepathEntry( String modulepathEntry )
    {
        modulepathEntries.add( modulepathEntry );
    }

    public void setModulepathEntries( List<String> modulepathEntries )
    {
        if ( modulepathEntries == null )
        {
            this.modulepathEntries = Collections.emptyList();
        }
        else
        {
            this.modulepathEntries = new LinkedList<>( modulepathEntries );
        }
    }

    public List<String> getModulepathEntries()
    {
        return Collections.unmodifiableList( modulepathEntries );
    }

    // ----------------------------------------------------------------------
    // Source files
    // ----------------------------------------------------------------------

    public void setSourceFiles( Set<File> sourceFiles )
    {
        if ( sourceFiles == null )
        {
            this.sourceFiles = Collections.emptySet();
        }
        else
        {
            this.sourceFiles = new HashSet<>( sourceFiles );
        }
    }

    public Set<File> getSourceFiles()
    {
        return sourceFiles;
    }

    public void addSourceLocation( String sourceLocation )
    {
        sourceLocations.add( sourceLocation );
    }

    public void setSourceLocations( List<String> sourceLocations )
    {
        if ( sourceLocations == null )
        {
            this.sourceLocations = Collections.emptyList();
        }
        else
        {
            this.sourceLocations = new LinkedList<>( sourceLocations );
        }
    }

    public List<String> getSourceLocations()
    {
        return Collections.unmodifiableList( sourceLocations );
    }

    public void addInclude( String include )
    {
        includes.add( include );
    }

    public void setIncludes( Set<String> includes )
    {
        if ( includes == null )
        {
            this.includes = Collections.emptySet();
        }
        else
        {
            this.includes = new HashSet<>( includes );
        }
    }

    public Set<String> getIncludes()
    {
        return Collections.unmodifiableSet( includes );
    }

    public void addExclude( String exclude )
    {
        excludes.add( exclude );
    }

    public void setExcludes( Set<String> excludes )
    {
        if ( excludes == null )
        {
            this.excludes = Collections.emptySet();
        }
        else
        {
            this.excludes = new HashSet<>( excludes );
        }
    }

    public Set<String> getExcludes()
    {
        return Collections.unmodifiableSet( excludes );
    }

    // ----------------------------------------------------------------------
    // Compiler Settings
    // ----------------------------------------------------------------------

    public void setDebug( boolean debug )
    {
        this.debug = debug;
    }

    public boolean isDebug()
    {
        return debug;
    }

    public void setDebugLevel( String debugLevel )
    {
        this.debugLevel = debugLevel;
    }

    public String getDebugLevel()
    {
        return debugLevel;
    }

    public void setWarnings( String warnings )
    {
        this.warnings = warnings;
    }

    public boolean isShowWarnings()
    {
        return showWarnings;
    }

    public void setShowWarnings( boolean showWarnings )
    {
        this.showWarnings = showWarnings;
    }

    public boolean isShowDeprecation()
    {
        return showDeprecation;
    }

    public String getWarnings()
    {
        return warnings;
    }



    public void setShowLint( boolean showLint )
    {
        this.showLint = showLint;
    }

    public boolean isShowLint()
    {
        return this.showLint;
    }

    public void setShowDeprecation( boolean showDeprecation )
    {
        this.showDeprecation = showDeprecation;
    }

    public boolean isFailOnWarning()
    {
        return failOnWarning;
    }

    public void setFailOnWarning( boolean failOnWarnings )
    {
        this.failOnWarning = failOnWarnings;
    }

    public String getSourceVersion()
    {
        return sourceVersion;
    }

    public void setSourceVersion( String sourceVersion )
    {
        this.sourceVersion = sourceVersion;
    }

    public String getTargetVersion()
    {
        return targetVersion;
    }

    public void setTargetVersion( String targetVersion )
    {
        this.targetVersion = targetVersion;
    }

    public String getReleaseVersion()
    {
        return releaseVersion;
    }

    public void setReleaseVersion( String releaseVersion )
    {
        this.releaseVersion = releaseVersion;
    }

    public String getSourceEncoding()
    {
        return sourceEncoding;
    }

    public void setSourceEncoding( String sourceEncoding )
    {
        this.sourceEncoding = sourceEncoding;
    }

    public String getModuleVersion()
    {
        return moduleVersion;
    }

    public void setModuleVersion( String moduleVersion )
    {
        this.moduleVersion = moduleVersion;
    }

    public void addCompilerCustomArgument( String customArgument, String value )
    {
        customCompilerArguments.add( new AbstractMap.SimpleImmutableEntry<>( customArgument, value ) );
    }

    /**
     * @deprecated will be removed in 2.X use #getCustomCompilerArgumentsAsMap
     * @return
     */
    @Deprecated
    public LinkedHashMap<String, String> getCustomCompilerArguments()
    {
        LinkedHashMap<String, String> arguments = new LinkedHashMap<>( customCompilerArguments.size() );
        for ( Map.Entry<String, String> entry : customCompilerArguments )
        {
            arguments.put( entry.getKey(), entry.getValue() );
        }
        return arguments;
    }

    /**
     * @deprecated will be removed in 2.X use #setCustomCompilerArgumentsAsMap
     * @param customCompilerArguments
     */
    @Deprecated
    public void setCustomCompilerArguments( LinkedHashMap<String, String> customCompilerArguments )
    {
        setCustomCompilerArgumentsAsMap( customCompilerArguments );
    }

    /**
     * Get all unique argument keys and their value. In case of duplicate keys, last one added wins.
     *
     * @return
     * @see CompilerConfiguration#getCustomCompilerArgumentsEntries()
     */
    public Map<String, String> getCustomCompilerArgumentsAsMap()
    {
        LinkedHashMap<String, String> arguments = new LinkedHashMap<>( customCompilerArguments.size() );
        for ( Map.Entry<String, String> entry : customCompilerArguments )
        {
            arguments.put( entry.getKey(), entry.getValue() );
        }
        return arguments;
    }

    public void setCustomCompilerArgumentsAsMap( Map<String, String> customCompilerArguments )
    {
        this.customCompilerArguments = new ArrayList<>();
        if ( customCompilerArguments != null )
        {
            this.customCompilerArguments.addAll( customCompilerArguments.entrySet() );
        }
    }

    /**
     * In case argument keys are not unique, this will return all entries
     *
     * @return
     */
    public Collection<Map.Entry<String,String>> getCustomCompilerArgumentsEntries()
    {
        return customCompilerArguments;
    }

    public boolean isFork()
    {
        return fork;
    }

    public void setFork( boolean fork )
    {
        this.fork = fork;
    }

    public String getMeminitial()
    {
        return meminitial;
    }

    public void setMeminitial( String meminitial )
    {
        this.meminitial = meminitial;
    }

    public String getMaxmem()
    {
        return maxmem;
    }

    public void setMaxmem( String maxmem )
    {
        this.maxmem = maxmem;
    }

    public String getExecutable()
    {
        return executable;
    }

    public void setExecutable( String executable )
    {
        this.executable = executable;
    }

    public File getWorkingDirectory()
    {
        return workingDirectory;
    }

    public void setWorkingDirectory( File workingDirectory )
    {
        this.workingDirectory = workingDirectory;
    }

    public File getBuildDirectory()
    {
        return buildDirectory;
    }

    public void setBuildDirectory( File buildDirectory )
    {
        this.buildDirectory = buildDirectory;
    }

    public String getOutputFileName()
    {
        return outputFileName;
    }

    public void setOutputFileName( String outputFileName )
    {
        this.outputFileName = outputFileName;
    }

    public boolean isOptimize()
    {
        return optimize;
    }

    public void setOptimize( boolean optimize )
    {
        this.optimize = optimize;
    }

    public String getCompilerVersion()
    {
        return compilerVersion;
    }

    public void setCompilerVersion( String compilerVersion )
    {
        this.compilerVersion = compilerVersion;
    }

    public boolean isVerbose()
    {
        return verbose;
    }

    public void setVerbose( boolean verbose )
    {
        this.verbose = verbose;
    }

    public boolean isParameters()
    {
        return parameters;
    }

    public void setParameters(boolean parameters)
    {
        this.parameters = parameters;
    }

    public boolean isEnablePreview()
    {
        return enablePreview;
    }

    public void setEnablePreview(boolean enablePreview)
    {
        this.enablePreview = enablePreview;
    }

    public void setProc(String proc )
    {
        this.proc = proc;
    }

    public void setGeneratedSourcesDirectory( File generatedSourcesDirectory )
    {
        this.generatedSourcesDirectory = generatedSourcesDirectory;
    }

    public File getGeneratedSourcesDirectory()
    {
        return generatedSourcesDirectory;
    }

    public String getProc()
    {
        return proc;
    }

    public void setAnnotationProcessors( String[] annotationProcessors )
    {
        this.annotationProcessors = annotationProcessors;
    }

    public String[] getAnnotationProcessors()
    {
        return annotationProcessors;
    }

    /**
     * -processorpath parameter in jdk 1.6+. If specified, annotation processors are only searched in the processor
     * path. Otherwise they are searched in the classpath.
     *
     * @param entry processor path entry to add
     */
    public void addProcessorPathEntry(String entry) {
        if ( processorPathEntries == null ) {
            processorPathEntries = new LinkedList<>();
        }

        processorPathEntries.add( entry );
    }

    /**
     * -processorpath parameter in jdk 1.6+. If specified, annotation processors are only searched in the processor
     * path. Otherwise they are searched in the classpath.
     *
     * @return the processorPathEntries
     */
    public List<String> getProcessorPathEntries() {
        return processorPathEntries;
    }

    /**
     * -processorpath parameter in jdk 1.6+. If specified, annotation processors are only searched in the processor
     * path. Otherwise they are searched in the classpath.
     *
     * @param processorPathEntries the processorPathEntries to set
     */
    public void setProcessorPathEntries(List<String> processorPathEntries) {
        this.processorPathEntries = processorPathEntries;
    }

    
    public void addProcessorModulePathEntry(String entry) {
        if ( processorModulePathEntries == null ) {
            processorModulePathEntries = new LinkedList<>();
        }

        processorModulePathEntries.add( entry );
    }
    
    public List<String> getProcessorModulePathEntries()
    {
        return processorModulePathEntries;
    }
    
    public void setProcessorModulePathEntries( List<String> processorModulePathEntries )
    {
        this.processorModulePathEntries = processorModulePathEntries;
    }
    
    
    public CompilerReuseStrategy getCompilerReuseStrategy()
    {
        return compilerReuseStrategy;
    }

    public void setCompilerReuseStrategy( CompilerReuseStrategy compilerReuseStrategy )
    {
        this.compilerReuseStrategy = compilerReuseStrategy;
    }

    public String getDebugFileName()
    {
        return debugFileName;
    }

    public void setDebugFileName(String debugFileName)
    {
        this.debugFileName = debugFileName;
    }

    /**
     * Re-use strategy of the compiler (implement for java only).
     */
    public enum CompilerReuseStrategy
    {
        /**
         * Always reuse the same.
         * <b>Default strategy.</b>
         */
        ReuseSame( "reuseSame" ),
        /**
         * Re-create a new compiler for each use.
         */
        AlwaysNew( "alwaysNew" ),
        /**
         * Re-use already created compiler, create new one if non already exists.
         * <b>Will mimic a kind of pool to prevent different threads use the same.</b>
         */
        ReuseCreated( "reuseCreated" );

        private String strategy;

        CompilerReuseStrategy( String strategy )
        {
            this.strategy = strategy;
        }

        public String getStrategy()
        {
            return strategy;
        }

        @Override
        public String toString()
        {
            return "CompilerReuseStrategy:" + this.strategy;
        }
    }

    public boolean isForceJavacCompilerUse()
    {
        return forceJavacCompilerUse;
    }

    public void setForceJavacCompilerUse( boolean forceJavacCompilerUse )
    {
        this.forceJavacCompilerUse = forceJavacCompilerUse;
    }

    public String getImplicitOption()
    {
        return implicitOption;
    }

    public void setImplicitOption( String implicitOption )
    {
        this.implicitOption = implicitOption;
    }

    public String describe()
    {
        List<String> params = new ArrayList<>();

        if ( isFork() )
        {
            params.add( "forked" );
        }

        // base options: debug, optimize, verbose, deprecation
        if ( isDebug() )
        {
            if ( StringUtils.isNotEmpty( getDebugLevel() ) )
            {
                params.add( "debug:" + getDebugLevel() );
            }
            else
            {
                params.add( "debug" );
            }
        }
        if ( isOptimize() )
        {
            params.add( "optimize" );
        }
        if ( isVerbose() )
        {
            params.add( "verbose" );
        }
        if ( isShowDeprecation() )
        {
            params.add( "deprecation" );
        }

        // target bytecode options: release or target, module-path
        if ( !StringUtils.isEmpty( getReleaseVersion() ) )
        {
            params.add( "release " + getReleaseVersion() );
        }
        else if ( !StringUtils.isEmpty( getTargetVersion() ) )
        {
            params.add( "target " + getTargetVersion() );
        }
        if ( getModulepathEntries() != null && !getModulepathEntries().isEmpty() )
        {
            params.add( "module-path" );
        }
        return String.join( " ", params );
    }
}
