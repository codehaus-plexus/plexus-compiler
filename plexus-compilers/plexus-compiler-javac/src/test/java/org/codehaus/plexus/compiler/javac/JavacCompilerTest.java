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

    protected String getRoleHint()
    {
        return "javac";
    }
}
