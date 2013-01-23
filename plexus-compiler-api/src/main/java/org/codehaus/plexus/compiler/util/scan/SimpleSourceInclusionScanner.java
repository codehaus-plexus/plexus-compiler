package org.codehaus.plexus.compiler.util.scan;

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

import org.codehaus.plexus.compiler.util.scan.mapping.SourceMapping;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 */
public class SimpleSourceInclusionScanner
    extends AbstractSourceInclusionScanner
{
    private Set<String> sourceIncludes;

    private Set<String> sourceExcludes;

    public SimpleSourceInclusionScanner( Set<String> sourceIncludes, Set<String> sourceExcludes )
    {
        this.sourceIncludes = sourceIncludes;

        this.sourceExcludes = sourceExcludes;
    }

    public Set<File> getIncludedSources( File sourceDir, File targetDir )
        throws InclusionScanException
    {
        List<SourceMapping> srcMappings = getSourceMappings();

        if ( srcMappings.isEmpty() )
        {
            return Collections.emptySet();
        }

        String[] potentialSources = scanForSources( sourceDir, sourceIncludes, sourceExcludes );

        Set<File> matchingSources = new HashSet<File>( potentialSources != null ? potentialSources.length : 0 );

        if ( potentialSources != null )
        {
            for ( String potentialSource : potentialSources )
            {
                matchingSources.add( new File( sourceDir, potentialSource ) );
            }
        }

        return matchingSources;
    }
}
