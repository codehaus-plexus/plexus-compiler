package org.codehaus.plexus.compiler;

import java.util.List;

/**
 *
 *
 * @author <a href="mailto:jason@plexus.org">Jason van Zyl</a>
 *
 * @version $Id$
 */
public interface Compiler
{
    static String ROLE = Compiler.class.getName();

    List compile( String[] classPathElements, String[] sourceDirectories, String destinationDirectory )
        throws Exception;
}

