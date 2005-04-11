package org.codehaus.plexus.compiler.ajc;

import org.codehaus.plexus.compiler.AbstractCompilerTest;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.Artifact;

import java.util.LinkedList;
import java.util.List;
import java.io.File;

/**
 * @author <a href="mailto:jason@plexus.org">Jason van Zyl</a>
 * @version $Id$
 */
public class AspectJCompilerTest
    extends AbstractCompilerTest
{
    public AspectJCompilerTest()
    {
        super();
    }

    protected String getRoleHint()
    {
        return "aspectj";
    }

    protected int expectedErrors()
    {
        return 2;
    }

    protected List getClasspath()
        throws Exception
    {
        List cp = new LinkedList( super.getClasspath() );

        Artifact artifact = new DefaultArtifact( "aspectj", "aspectjrt", "1.2", "jar" );

        cp.add( getLocalArtifactPath( artifact ) );

        return cp;
    }

}
