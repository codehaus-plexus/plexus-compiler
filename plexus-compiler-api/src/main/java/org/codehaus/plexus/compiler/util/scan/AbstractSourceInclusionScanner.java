package org.codehaus.plexus.compiler.util.scan;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.codehaus.plexus.compiler.util.scan.mapping.SourceMapping;
import org.codehaus.plexus.util.DirectoryScanner;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author jdcasey
 * @version $Id$
 */
public abstract class AbstractSourceInclusionScanner
    implements SourceInclusionScanner
{
    private final List sourceMappings = new ArrayList();

    public final void addSourceMapping( SourceMapping sourceMapping )
    {
        sourceMappings.add( sourceMapping );
    }

    protected final List getSourceMappings()
    {
        return Collections.unmodifiableList( sourceMappings );
    }

    protected String[] scanForSources( File sourceDir, Set sourceIncludes, Set sourceExcludes )
    {
        DirectoryScanner ds = new DirectoryScanner();
        ds.setFollowSymlinks( true );
        ds.setBasedir( sourceDir );

        String[] includes;
        if ( sourceIncludes.isEmpty() )
        {
            includes = new String[0];
        }
        else
        {
            includes = (String[]) sourceIncludes.toArray( new String[sourceIncludes.size()] );
        }

        ds.setIncludes( includes );

        String[] excludes;
        if ( sourceExcludes.isEmpty() )
        {
            excludes = new String[0];
        }
        else
        {
            excludes = (String[]) sourceExcludes.toArray( new String[sourceExcludes.size()] );
        }

        ds.setExcludes( excludes );
        ds.addDefaultExcludes();

        ds.scan();

        return ds.getIncludedFiles();
    }
}
