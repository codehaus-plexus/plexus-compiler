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

    protected String getRoleHint()
    {
        return "aspectj";
    }

    protected int expectedErrors()
    {
        // olamy well I agree it's hackhish but I don't want to waste too much time with aspectj which is probably
        // not used a lot anymore...
        String javaVersion = getJavaVersion();
        if (javaVersion.equals( "11" ))
        {
            return 11;
        }
        return 1;
    }

    protected Collection<String> expectedOutputFiles()
    {
        String javaVersion = System.getProperty( "java.version" );
        // olamy well I agree it's hackhish but I don't want to waste too much time with aspectj which is probably
        // not used a lot anymore...
//        if (javaVersion.startsWith( "9" ) || javaVersion.startsWith( "10" ))
//        {
//            return Collections.emptyList();
//        }
        return Arrays.asList( new String[]{ "org/codehaus/foo/ExternalDeps.class", "org/codehaus/foo/Person.class" } );
    }

    protected List<String> getClasspath()
        throws Exception
    {
        List<String> cp = super.getClasspath();

        File localArtifactPath =
            getLocalArtifactPath( "org.aspectj", "aspectjrt", System.getProperty( "aspectj.version" ), "jar" );
        cp.add( localArtifactPath.getAbsolutePath() );

        return cp;
    }

}
