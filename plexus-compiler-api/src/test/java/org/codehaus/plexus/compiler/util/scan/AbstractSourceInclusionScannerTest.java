package org.codehaus.plexus.compiler.util.scan;

/*
 * Copyright 2006 The Apache Software Foundation.
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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.Set;

import junit.framework.TestCase;

import org.codehaus.plexus.compiler.util.scan.mapping.SuffixMapping;
import org.codehaus.plexus.util.IOUtil;

/**
 * Tests for all the implementations of <code>SourceInclusionScanner</code>
 * 
 * @author <a href="mailto:carlos@apache.org">Carlos Sanchez</a>
 * @version $Id$
 */
public class AbstractSourceInclusionScannerTest
    extends TestCase
{
    
    private static final String TESTFILE_DEST_MARKER_FILE =
        SourceInclusionScanner.class.getName().replace( '.', '/' ) + "-testMarker.txt";

    protected SourceInclusionScanner scanner;

    public void testGetIncludedSources() throws Exception
    {
        File base = new File( getTestBaseDir(), "testGetIncludedSources" );

        File sourceFile = new File( base, "file.java" );

        writeFile( sourceFile );

        sourceFile.setLastModified( System.currentTimeMillis() );

        SuffixMapping mapping = new SuffixMapping( ".java", ".xml" );

        scanner.addSourceMapping( mapping );

        Set includedSources = scanner.getIncludedSources( base, base );

        assertTrue( "no sources were included", includedSources.size() > 0 );

        Iterator it = includedSources.iterator();

        while ( it.hasNext() )
        {
            File file = (File) it.next();
            assertTrue( "file included does not exist", file.exists() );
        }
    }

    // ----------------------------------------------------------------------
    // Utilities
    // ----------------------------------------------------------------------

    protected File getTestBaseDir()
    {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        URL markerResource = cl.getResource( TESTFILE_DEST_MARKER_FILE );

        File basedir;

        if ( markerResource != null )
        {
            File marker = new File( markerResource.getPath() );

            basedir = marker.getParentFile().getAbsoluteFile();
        }
        else
        {
            // punt.
            System.out.println( "Cannot find marker file: \'" + TESTFILE_DEST_MARKER_FILE + "\' in classpath. " +
                                "Using '.' for basedir." );

            basedir = new File( "." ).getAbsoluteFile();
        }

        return basedir;
    }

    protected void writeFile( File file )
        throws IOException
    {
        FileWriter fWriter = null;
        try
        {
            File parent = file.getParentFile();
            if ( !parent.exists() )
            {
                parent.mkdirs();
            }

            file.deleteOnExit();

            fWriter = new FileWriter( file );

            fWriter.write( "This is just a test file." );
        }
        finally
        {
            IOUtil.close( fWriter );
        }
    }

}
