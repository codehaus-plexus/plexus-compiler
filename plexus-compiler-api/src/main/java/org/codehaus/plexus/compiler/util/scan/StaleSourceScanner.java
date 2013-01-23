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

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author jdcasey
 */
public class StaleSourceScanner
    extends AbstractSourceInclusionScanner
{
    private final long lastUpdatedWithinMsecs;

    private final Set<String> sourceIncludes;

    private final Set<String> sourceExcludes;

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public StaleSourceScanner()
    {
        this( 0, Collections.singleton( "**/*" ), Collections.<String>emptySet() );
    }

    public StaleSourceScanner( long lastUpdatedWithinMsecs )
    {
        this( lastUpdatedWithinMsecs, Collections.singleton( "**/*" ), Collections.<String>emptySet() );
    }

    public StaleSourceScanner( long lastUpdatedWithinMsecs, Set<String> sourceIncludes, Set<String> sourceExcludes )
    {
        this.lastUpdatedWithinMsecs = lastUpdatedWithinMsecs;

        this.sourceIncludes = sourceIncludes;

        this.sourceExcludes = sourceExcludes;
    }

    // ----------------------------------------------------------------------
    // SourceInclusionScanner Implementation
    // ----------------------------------------------------------------------

    public Set<File> getIncludedSources( File sourceDir, File targetDir )
        throws InclusionScanException
    {
        List<SourceMapping> srcMappings = getSourceMappings();

        if ( srcMappings.isEmpty() )
        {
            return Collections.emptySet();
        }

        String[] potentialIncludes = scanForSources( sourceDir, sourceIncludes, sourceExcludes );

        Set<File> matchingSources = new HashSet<File>();

        for ( String path : potentialIncludes )
        {
            File sourceFile = new File( sourceDir, path );

            staleSourceFileTesting:
            for ( SourceMapping mapping : srcMappings )
            {
                Set<File> targetFiles = mapping.getTargetFiles( targetDir, path );

                // never include files that don't have corresponding target mappings.
                // the targets don't have to exist on the filesystem, but the
                // mappers must tell us to look for them.
                for ( File targetFile : targetFiles )
                {
                    if ( !targetFile.exists() || ( targetFile.lastModified() + lastUpdatedWithinMsecs
                        < sourceFile.lastModified() ) )
                    {
                        matchingSources.add( sourceFile );
                        break staleSourceFileTesting;
                    }
                }
            }
        }

        return matchingSources;
    }
}
