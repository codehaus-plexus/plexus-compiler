/* Created on Oct 4, 2004 */
package org.codehaus.plexus.compiler.ajc;

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

    private List aspectPath = new LinkedList();

    private List inJars = new LinkedList();

    private List inPath = new LinkedList();

    private String outputJar;

    private Map ajOptions = new TreeMap();

    private Map sourcePathResources;

    public void setAspectPath( List aspectPath )
    {
        this.aspectPath = new LinkedList( aspectPath );
    }

    public void addAspectPath( String aspectPath )
    {
        this.aspectPath.add( aspectPath );
    }

    public List getAspectPath()
    {
        return Collections.unmodifiableList( aspectPath );
    }

    public void setInJars( List inJars )
    {
        this.inJars = new LinkedList( inJars );
    }

    public void addInJar( String inJar )
    {
        this.inJars.add( inJar );
    }

    public List getInJars()
    {
        return Collections.unmodifiableList( inJars );
    }

    public void setInPath( List inPath )
    {
        this.inPath = new LinkedList( inPath );
    }

    public void addInPath( String inPath )
    {
        this.inPath.add( inPath );
    }

    public List getInPath()
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
    public void setAJOptions( Map ajOptions )
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
    public Map getAJOptions()
    {
        return Collections.unmodifiableMap( ajOptions );
    }

    public void setSourcePathResources( Map sourcePathResources )
    {
        this.sourcePathResources = new TreeMap( sourcePathResources );
    }

    public Map getSourcePathResources()
    {
        return sourcePathResources;
    }

}