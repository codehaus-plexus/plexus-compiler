/* Created on Oct 4, 2004 */
package org.codehaus.plexus.compiler.ajc;

import java.io.File;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.codehaus.plexus.compiler.CompilerConfiguration;

/**
 * @author jdcasey
 */
public class AspectJCompilerConfiguration
    extends CompilerConfiguration
{

    private List<String> aspectPath = new LinkedList<String>();

    private List<String> inJars = new LinkedList<String>();

    private List<String> inPath = new LinkedList<String>();

    private String outputJar;

    private Map<String, String> ajOptions = new TreeMap<String, String>();

    private Map<String, File> sourcePathResources;

    public void setAspectPath( List<String> aspectPath )
    {
        this.aspectPath = new LinkedList<String>( aspectPath );
    }

    public void addAspectPath( String aspectPath )
    {
        this.aspectPath.add( aspectPath );
    }

    public List<String> getAspectPath()
    {
        return Collections.unmodifiableList( aspectPath );
    }

    public void setInJars( List<String> inJars )
    {
        this.inJars = new LinkedList<String>( inJars );
    }

    public void addInJar( String inJar )
    {
        this.inJars.add( inJar );
    }

    public List<String> getInJars()
    {
        return Collections.unmodifiableList( inJars );
    }

    public void setInPath( List<String> inPath )
    {
        this.inPath = new LinkedList<String>( inPath );
    }

    public void addInPath( String inPath )
    {
        this.inPath.add( inPath );
    }

    public List<String> getInPath()
    {
        return Collections.unmodifiableList( inPath );
    }

    public void setOutputJar( String outputJar )
    {
        this.outputJar = outputJar;
    }

    public String getOutputJar()
    {
        return outputJar;
    }

    /**
     * Ignored, not supported yet
     * @param ajOptions
     */
    public void setAJOptions( Map<String, String> ajOptions )
    {
        //TODO
        //this.ajOptions = new TreeMap( ajOptions );
    }

    public void setAJOption( String optionName, String optionValue )
    {
        this.ajOptions.put( optionName, optionValue );
    }

    /**
     * Ignored, not supported yet
     * @return empty Map
     */
    public Map<String, String> getAJOptions()
    {
        return Collections.unmodifiableMap( ajOptions );
    }

    public void setSourcePathResources( Map<String, File> sourcePathResources )
    {
        this.sourcePathResources = new TreeMap<String, File>( sourcePathResources );
    }

    public Map<String, File> getSourcePathResources()
    {
        return sourcePathResources;
    }

}