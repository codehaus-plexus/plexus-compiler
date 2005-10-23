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

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.LinkedHashMap;

/**
 * @author jdcasey
 * @version $Id$
 */
public class CompilerConfiguration
{
    private String outputLocation;

    private List classpathEntries = new LinkedList();
    
    // ----------------------------------------------------------------------
    // Source Files
    // ----------------------------------------------------------------------

    private Set sourceFiles = new HashSet();

    private List sourceLocations = new LinkedList();

    private Set includes = new HashSet();

    private Set excludes = new HashSet();

    // ----------------------------------------------------------------------
    // Compiler Settings
    // ----------------------------------------------------------------------

    private boolean debug;

    private boolean showWarnings = true;

    private boolean showDeprecation;

    private String sourceVersion;

    private String targetVersion;

    private String sourceEncoding;

    private LinkedHashMap customCompilerArguments = new LinkedHashMap();

    private boolean fork;
    
    private boolean optimize;

    private String meminitial;

    private String maxmem;

    private String executable;

    private File workingDirectory;
    
    private String compilerVersion;
    
    private boolean verbose = false;
    
    /**
     * A build temporary directory, eg target/.
     *
     * Used by the compiler implementation to put temporary files.
     */
    private File buildDirectory;

    /**
     * Used to control the name of the output file when compiling a set of
     * sources to a single file.
     */
    private String outputFileName;

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

    public void setClasspathEntries( List classpathEntries )
    {
        if ( classpathEntries == null )
        {
            this.classpathEntries = Collections.EMPTY_LIST;
        }
        else
        {
            this.classpathEntries = new LinkedList( classpathEntries );
        }
    }

    public List getClasspathEntries()
    {
        return Collections.unmodifiableList( classpathEntries );
    }

    // ----------------------------------------------------------------------
    // Source files
    // ----------------------------------------------------------------------

    public void setSourceFiles( Set sourceFiles )
    {
        if ( sourceFiles == null )
        {
            this.sourceFiles = Collections.EMPTY_SET;
        }
        else
        {
            this.sourceFiles = new HashSet( sourceFiles );
        }
    }

    public Set getSourceFiles()
    {
        return sourceFiles;
    }

    public void addSourceLocation( String sourceLocation )
    {
        sourceLocations.add( sourceLocation );
    }

    public void setSourceLocations( List sourceLocations )
    {
        if ( sourceLocations == null )
        {
            this.sourceLocations = Collections.EMPTY_LIST;
        }
        else
        {
            this.sourceLocations = new LinkedList( sourceLocations );
        }
    }

    public List getSourceLocations()
    {
        return Collections.unmodifiableList( sourceLocations );
    }

    public void addInclude( String include )
    {
        includes.add( include );
    }

    public void setIncludes( Set includes )
    {
        if ( includes == null )
        {
            this.includes = Collections.EMPTY_SET;
        }
        else
        {
            this.includes = new HashSet( includes );
        }
    }

    public Set getIncludes()
    {
        return Collections.unmodifiableSet( includes );
    }

    public void addExclude( String exclude )
    {
        excludes.add( exclude );
    }

    public void setExcludes( Set excludes )
    {
        if ( excludes == null )
        {
            this.excludes = Collections.EMPTY_SET;
        }
        else
        {
            this.excludes = new HashSet( excludes );
        }
    }

    public Set getExcludes()
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

    public void setShowWarnings( boolean showWarnings )
    {
        this.showWarnings = showWarnings;
    }

    public boolean isShowWarnings()
    {
        return showWarnings;
    }

    public boolean isShowDeprecation()
    {
        return showDeprecation;
    }

    public void setShowDeprecation( boolean showDeprecation )
    {
        this.showDeprecation = showDeprecation;
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

    public String getSourceEncoding()
    {
        return sourceEncoding;
    }

    public void setSourceEncoding( String sourceEncoding )
    {
        this.sourceEncoding = sourceEncoding;
    }

    public void addCompilerCustomArgument( String customArgument, String value )
    {
        customCompilerArguments.put( customArgument, value );
    }

    public LinkedHashMap getCustomCompilerArguments()
    {
        return new LinkedHashMap( customCompilerArguments );
    }

    public void setCustomCompilerArguments( LinkedHashMap customCompilerArguments )
    {
        if ( customCompilerArguments == null )
        {
            this.customCompilerArguments = new LinkedHashMap();
        }
        else
        {
            this.customCompilerArguments = customCompilerArguments;
        }
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

    public void setVerbose(boolean verbose)
    {
        this.verbose = verbose;
    }
}
