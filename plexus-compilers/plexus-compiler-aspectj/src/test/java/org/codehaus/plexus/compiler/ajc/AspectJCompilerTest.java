package org.codehaus.plexus.compiler.ajc;

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
    public AspectJCompilerTest( String s )
    {
        super( s );
    }

    protected String getRoleHint()
    {
        return "aspectj";
    }
    
    protected int expectedErrors()
    {
        return 4;
    }
}
