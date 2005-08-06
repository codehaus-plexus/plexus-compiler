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

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * @author jdcasey
 * @version $Id$
 */
public class CompilerConfiguration
{
    private String outputLocation;

    private List classpathEntries = new LinkedList();

    // ----------------------------------------------------------------------
    // Sources
    // ----------------------------------------------------------------------

    private Set sourceFiles = new HashSet();

    private List sourceLocations = new LinkedList();

    private Set includes = new HashSet();

    private Set excludes = new HashSet();

    // ----------------------------------------------------------------------
    // Compiler Settings
    // ----------------------------------------------------------------------

    private boolean debug;

    private boolean showWarnings;

    private boolean showDeprecation;

    private String sourceVersion;

    private String targetVersion;

    private String sourceEncoding;

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    private Map compilerOptions = new TreeMap();

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
        this.classpathEntries = new LinkedList( classpathEntries );
    }

    public List getClasspathEntries()
    {
        return Collections.unmodifiableList( classpathEntries );
    }

    // ----------------------------------------------------------------------
    // Source files
    // ----------------------------------------------------------------------

    public void addSourceLocation( String sourceLocation )
    {
        sourceLocations.add( sourceLocation );
    }

    public void setSourceLocations( List sourceLocations )
    {
        this.sourceLocations = new LinkedList( sourceLocations );
    }

    public List getSourceLocations()
    {
        return Collections.unmodifiableList( sourceLocations );
    }

    public void setSourceFiles( Set sourceFiles )
    {
        this.sourceFiles = sourceFiles;
    }

    public Set getSourceFiles()
    {
        return sourceFiles;
    }

    public void addInclude( String include )
    {
        includes.add( include );
    }

    public void setIncludes( Set includes )
    {
        this.includes = new HashSet( includes );
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
        this.excludes = new HashSet( excludes );
    }

    public Set getExcludes()
    {
        return Collections.unmodifiableSet( excludes );
    }

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

    // ----------------------------------------------------------------------
    // Generic options
    // ----------------------------------------------------------------------

    public void addCompilerOption( String optionName, String optionValue )
    {
        compilerOptions.put( optionName, optionValue );
    }

    public void setCompilerOptions( Map compilerOptions )
    {
        this.compilerOptions = new TreeMap( compilerOptions );
    }

    public Map getCompilerOptions()
    {
        return Collections.unmodifiableMap( compilerOptions );
    }

    public boolean isShowDeprecation()
    {
        return showDeprecation;
    }

    public void setShowDeprecation( boolean showDeprecation )
    {
        this.showDeprecation = showDeprecation;
    }
}
