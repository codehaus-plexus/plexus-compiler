package org.codehaus.plexus.compiler.ajc;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.versioning.VersionRange;
import org.codehaus.plexus.compiler.AbstractCompilerTest;

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
        return 1;
    }

    protected List getClasspath()
        throws Exception
    {
        List cp = super.getClasspath();

        VersionRange versionRange = VersionRange.createFromVersion( "1.5.0" );

        Artifact artifact = new DefaultArtifact( "aspectj", "aspectjrt", versionRange, Artifact.SCOPE_RUNTIME,
                                                 "jar", null, new DefaultArtifactHandler( "jar" ) );

        cp.add( getLocalArtifactPath( artifact ).getAbsolutePath() );

        return cp;
    }

}
