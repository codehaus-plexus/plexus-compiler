package org.codehaus.plexus.compiler.eclipse;

import org.codehaus.plexus.compiler.AbstractCompilerTest;

/**
 *
 *
 * @author <a href="mailto:jason@plexus.org">Jason van Zyl</a>
 *
 * @version $Id$
 */
public class EclipseCompilerTest
    extends AbstractCompilerTest
{
   
    public EclipseCompilerTest()
    {
        super();
    }
    

    protected String getRoleHint()
    {
        return "eclipse";
    }

    protected int expectedErrors()
    {
        return 2;
    }
}
