package org.codehaus.plexus.compiler.jikes;

import org.codehaus.plexus.compiler.AbstractCompilerTest;

/**
 *
 *
 * @author <a href="mailto:jason@plexus.org">Jason van Zyl</a>
 *
 * @version $Id$
 */
public class JikesCompilerTest extends AbstractCompilerTest
{
    public JikesCompilerTest()
    {
        super();
    }

    protected String getRoleHint()
    {
        return "jikes";
    }
}
