package org.codehaus.plexus.compiler.javac;

import org.codehaus.plexus.compiler.AbstractCompilerTest;

/**
 *
 *
 * @author <a href="mailto:jason@plexus.org">Jason van Zyl</a>
 *
 * @version $Id$
 */
public class JavacCompilerTest extends AbstractCompilerTest
{
    public JavacCompilerTest()
    {
        super();
    }

    public void setUp() throws Exception
    {
        super.setUp();
        getCompilerConfiguration().setDebug( true );
    }

    protected String getRoleHint()
    {
        return "javac";
    }
}
