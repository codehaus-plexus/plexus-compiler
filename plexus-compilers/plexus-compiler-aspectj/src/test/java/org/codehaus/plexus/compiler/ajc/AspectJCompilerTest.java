package org.codehaus.plexus.compiler.ajc;

import java.util.LinkedList;
import java.util.List;

import org.codehaus.plexus.compiler.AbstractCompilerTest;

/**
 *
 *
 * @author <a href="mailto:jason@plexus.org">Jason van Zyl</a>
 *
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
    {
        List cp = new LinkedList(super.getClasspath());
        cp.add(getMavenRepoLocal() + "/aspectj/jars/aspectjrt-1.2.jar");
        
        return cp;
    }
    
}
