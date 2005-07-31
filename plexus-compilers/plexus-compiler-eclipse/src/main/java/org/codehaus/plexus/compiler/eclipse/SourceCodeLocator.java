package org.codehaus.plexus.compiler.eclipse;

/**
 * The MIT License
 *
 * Copyright (c) 2005, The Codehaus
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import java.util.List;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.io.File;

/**
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @version $Id$
 */
public class SourceCodeLocator
{
    private List sourceRoots;

    private Map cache;

    public SourceCodeLocator( List sourceRoots )
    {
        this.sourceRoots = sourceRoots;

        cache = new HashMap();
    }

    public File findSourceCodeForClass( String className )
    {
        File f = (File) cache.get( className );

        if ( f != null )
        {
            return f;
        }

        String sourceName = className.replace( '.', System.getProperty( "file.separator" ).charAt( 0 ) );

        sourceName += ".java";

        f = findInRoots( sourceName );

        cache.put( className, f );

        return f;
    }

    private File findInRoots( String s )
    {
        for ( Iterator it = sourceRoots.iterator(); it.hasNext(); )
        {
            String root = (String) it.next();

            File f = new File( root, s );

            if ( f.exists() )
            {
                return f;
            }
        }

        return null;
    }
}
