package org.codehaus.plexus.compiler.javac;

import org.codehaus.plexus.compiler.AbstractCompilerTest;

/**
 * @author <a href="mailto:jason@plexus.org">Jason van Zyl</a>
 */
public class JavacErrorProneCompilerTest
    extends AbstractCompilerTest
{

    protected boolean java8() {
        return System.getProperty( "java.version" ).startsWith( "1.8" );
    }

    @Override
    protected String getRoleHint()
    {
        return "javac-with-errorprone";
    }

    @Override
    protected int expectedWarnings()
    {
        if ( java8() ) {
            return 1;
        }
        else
        {
            return 2;
        }
    }

    @Override
    protected int expectedErrors()
    {
        return 1;
    }

    @Override
    public String getSourceVersion()
    {
        return "1.8";
    }

    @Override
    public String getTargetVersion()
    {
        return "1.8";
    }
}
