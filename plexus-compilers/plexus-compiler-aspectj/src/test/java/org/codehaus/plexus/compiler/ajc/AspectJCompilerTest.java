package org.codehaus.plexus.compiler.ajc;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.codehaus.plexus.compiler.AbstractCompilerTest;

/**
 * @author <a href="mailto:jason@plexus.org">Jason van Zyl</a>
 */
public class AspectJCompilerTest
    extends AbstractCompilerTest
{
    public AspectJCompilerTest()
    {
        super();
    }

    @Override
    protected String getRoleHint()
    {
        return "aspectj";
    }

    @Override
    protected Collection<String> expectedOutputFiles()
    {
        return Arrays.asList( "org/codehaus/foo/ExternalDeps.class", "org/codehaus/foo/Person.class" );
    }

    @Override
    protected List<String> getClasspath()
        throws Exception
    {
        List<String> classpath = super.getClasspath();
        String aspectjVersion = System.getProperty( "aspectj.version" );
        File aspectjRuntime = getLocalArtifactPath( "org.aspectj", "aspectjrt", aspectjVersion, "jar" );
        classpath.add( aspectjRuntime.getAbsolutePath() );
        return classpath;
    }

}
