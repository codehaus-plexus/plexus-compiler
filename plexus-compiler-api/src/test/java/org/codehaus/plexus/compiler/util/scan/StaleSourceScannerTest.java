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

import java.io.File;
import java.util.Collections;
import java.util.Set;

import org.codehaus.plexus.compiler.util.scan.mapping.SingleTargetSourceMapping;
import org.codehaus.plexus.compiler.util.scan.mapping.SourceMapping;
import org.codehaus.plexus.compiler.util.scan.mapping.SuffixMapping;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

/**
 * @author jdcasey
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 */
public class StaleSourceScannerTest
    extends AbstractSourceInclusionScannerTest
{

    @BeforeEach
    public void setUp()
        throws Exception
    {
        scanner = new StaleSourceScanner();
    }

    @Test
    public void testWithDefaultConstructorShouldFindOneStaleSource()
        throws Exception
    {
        File base = new File( getTestBaseDir(), "test1" );

        long now = System.currentTimeMillis();

        File targetFile = new File( base, "file.xml" );

        writeFile( targetFile );

        targetFile.setLastModified( now - 60000 );

        File sourceFile = new File( base, "file.java" );

        writeFile( sourceFile );

        sourceFile.setLastModified( now );

        SuffixMapping mapping = new SuffixMapping( ".java", ".xml" );

        scanner.addSourceMapping( mapping );

        Set<File> result = scanner.getIncludedSources( base, base );

        assertThat( "wrong number of stale sources returned.", result.size(), is( 1) );

        assertThat( "expected stale source file not found in result", result, Matchers.contains( sourceFile ) );
    }

    @Test
    public void testWithDefaultConstructorShouldNotFindStaleSources()
        throws Exception
    {
        File base = new File( getTestBaseDir(), "test2" );

        long now = System.currentTimeMillis();

        File sourceFile = new File( base, "file.java" );

        writeFile( sourceFile );

        sourceFile.setLastModified( now - 60000 );

        File targetFile = new File( base, "file.xml" );

        writeFile( targetFile );

        targetFile.setLastModified( now );

        SuffixMapping mapping = new SuffixMapping( ".java", ".xml" );

        scanner.addSourceMapping( mapping );

        Set<File> result = scanner.getIncludedSources( base, base );

        assertThat( "wrong number of stale sources returned.", result.size(), is( 0 ) );

        assertThat( "expected stale source file not found in result", result, empty( ) );
    }

    @Test
    public void testWithDefaultConstructorShouldFindStaleSourcesBecauseOfMissingTargetFile()
        throws Exception
    {
        File base = new File( getTestBaseDir(), "test3" );

        File sourceFile = new File( base, "file.java" );

        writeFile( sourceFile );

        SuffixMapping mapping = new SuffixMapping( ".java", ".xml" );

        scanner.addSourceMapping( mapping );

        Set<File> result = scanner.getIncludedSources( base, base );

        assertThat( "wrong number of stale sources returned.", result.size(), is( 1) );

        assertThat( "expected stale source file not found in result", result, contains( sourceFile ) );
    }

    @Test
    public void testWithDefaultConstructorShouldFindStaleSourcesOneBecauseOfMissingTargetAndOneBecauseOfStaleTarget()
        throws Exception
    {
        File base = new File( getTestBaseDir(), "test4" );

        long now = System.currentTimeMillis();

        File targetFile = new File( base, "file2.xml" );

        writeFile( targetFile );

        targetFile.setLastModified( now - 60000 );

        File sourceFile = new File( base, "file.java" );

        writeFile( sourceFile );

        File sourceFile2 = new File( base, "file2.java" );

        writeFile( sourceFile2 );

        sourceFile2.setLastModified( now );

        SuffixMapping mapping = new SuffixMapping( ".java", ".xml" );

        scanner.addSourceMapping( mapping );

        Set<File> result = scanner.getIncludedSources( base, base );

        assertThat( "wrong number of stale sources returned.", result.size(), is( 2) );

        assertThat( "expected stale source file not found in result", result, containsInAnyOrder( sourceFile, sourceFile2 ) );

    }

    @Test
    public void testWithDefaultConstructorShouldFindOneStaleSourcesWithStaleTargetAndOmitUpToDateSource()
        throws Exception
    {
        File base = new File( getTestBaseDir(), "test5" );

        long now = System.currentTimeMillis();

        // target/source (1) should result in source being included.

        // write the target file first, and set the lastmod to some time in the
        // past to ensure this.
        File targetFile = new File( base, "file.xml" );

        writeFile( targetFile );

        targetFile.setLastModified( now - 60000 );

        // now write the source file, and set the lastmod to now.
        File sourceFile = new File( base, "file.java" );

        writeFile( sourceFile );

        sourceFile.setLastModified( now );

        // target/source (2) should result in source being omitted.

        // write the source file first, and set the lastmod to some time in the
        // past to ensure this.
        File sourceFile2 = new File( base, "file2.java" );

        writeFile( sourceFile2 );

        sourceFile2.setLastModified( now - 60000 );

        // now write the target file, with lastmod of now.
        File targetFile2 = new File( base, "file2.xml" );

        writeFile( targetFile2 );

        targetFile2.setLastModified( now );

        SuffixMapping mapping = new SuffixMapping( ".java", ".xml" );

        scanner.addSourceMapping( mapping );

        Set<File> result = scanner.getIncludedSources( base, base );

        assertThat( "wrong number of stale sources returned.", result.size(), is( 1 ) );

        assertThat( "expected stale source file not found in result", result, contains( sourceFile ) );
    }

    @Test
    public void testConstructedWithMsecsShouldReturnOneSourceFileOfTwoDueToLastMod()
        throws Exception
    {
        File base = new File( getTestBaseDir(), "test6" );

        long now = System.currentTimeMillis();

        File targetFile = new File( base, "file.xml" );

        writeFile( targetFile );

        // should be within the threshold of lastMod for stale sources.
        targetFile.setLastModified( now - 8000 );

        File sourceFile = new File( base, "file.java" );

        writeFile( sourceFile );

        // modified 'now' for comparison with the above target file.
        sourceFile.setLastModified( now );

        File targetFile2 = new File( base, "file2.xml" );

        writeFile( targetFile2 );

        targetFile2.setLastModified( now - 12000 );

        File sourceFile2 = new File( base, "file2.java" );

        writeFile( sourceFile2 );

        // modified 'now' for comparison to above target file.
        sourceFile2.setLastModified( now );

        SuffixMapping mapping = new SuffixMapping( ".java", ".xml" );

        scanner = new StaleSourceScanner( 10000 );

        scanner.addSourceMapping( mapping );

        Set<File> result = scanner.getIncludedSources( base, base );

        assertThat( "wrong number of stale sources returned.", result.size(), is( 1 ) );

        assertThat( "expected stale source file not found in result", result, contains( sourceFile2 ) );
    }

    @Test
    public void testConstructedWithMsecsIncludesAndExcludesShouldReturnOneSourceFileOfThreeDueToIncludePattern()
        throws Exception
    {
        File base = new File( getTestBaseDir(), "test7" );

        long now = System.currentTimeMillis();

        File targetFile = new File( base, "file.xml" );

        writeFile( targetFile );

        // should be within the threshold of lastMod for stale sources.
        targetFile.setLastModified( now - 12000 );

        File sourceFile = new File( base, "file.java" );

        writeFile( sourceFile );

        // modified 'now' for comparison with the above target file.
        sourceFile.setLastModified( now );

        File targetFile2 = new File( base, "file2.xml" );

        writeFile( targetFile2 );

        targetFile2.setLastModified( now - 12000 );

        File sourceFile2 = new File( base, "file2.java" );

        writeFile( sourceFile2 );

        // modified 'now' for comparison to above target file.
        sourceFile2.setLastModified( now );

        File targetFile3 = new File( base, "file3.xml" );

        writeFile( targetFile3 );

        targetFile3.setLastModified( now - 12000 );

        File sourceFile3 = new File( base, "file3.java" );

        writeFile( sourceFile3 );

        // modified 'now' for comparison to above target file.
        sourceFile3.setLastModified( now );

        SuffixMapping mapping = new SuffixMapping( ".java", ".xml" );

        scanner = new StaleSourceScanner( 0, Collections.singleton( "*3.java" ), Collections.<String>emptySet() );

        scanner.addSourceMapping( mapping );

        Set<File> result = scanner.getIncludedSources( base, base );

        assertThat( "wrong number of stale sources returned.", result.size(), is( 1 ) );

        assertThat( "expected stale source file not found in result", result, contains( sourceFile3 ) );
    }

    @Test
    public void testConstructedWithMsecsIncludesAndExcludesShouldReturnTwoSourceFilesOfThreeDueToExcludePattern()
        throws Exception
    {
        File base = new File( getTestBaseDir(), "test8" );

        long now = System.currentTimeMillis();

        File targetFile = new File( base, "fileX.xml" );

        writeFile( targetFile );

        // should be within the threshold of lastMod for stale sources.
        targetFile.setLastModified( now - 12000 );

        File sourceFile = new File( base, "fileX.java" );

        writeFile( sourceFile );

        // modified 'now' for comparison with the above target file.
        sourceFile.setLastModified( now );

        File targetFile2 = new File( base, "file2.xml" );

        writeFile( targetFile2 );

        targetFile2.setLastModified( now - 12000 );

        File sourceFile2 = new File( base, "file2.java" );

        writeFile( sourceFile2 );

        // modified 'now' for comparison to above target file.
        sourceFile2.setLastModified( now );

        File targetFile3 = new File( base, "file3.xml" );

        writeFile( targetFile3 );

        targetFile3.setLastModified( now - 12000 );

        File sourceFile3 = new File( base, "file3.java" );

        writeFile( sourceFile3 );

        // modified 'now' for comparison to above target file.
        sourceFile3.setLastModified( now );

        SuffixMapping mapping = new SuffixMapping( ".java", ".xml" );

        scanner = new StaleSourceScanner( 0, Collections.singleton( "**/*" ), Collections.singleton( "*X.*" ) );

        scanner.addSourceMapping( mapping );

        Set<File> result = scanner.getIncludedSources( base, base );

        assertThat( "wrong number of stale sources returned.", result.size(), is( 2 ) );

        assertThat( "expected stale source file not found in result", result, containsInAnyOrder( sourceFile2, sourceFile3 ) );

    }

    @Test
    public void testSingleFileSourceMapping()
        throws Exception
    {
        File src = new File( getTestBaseDir(), "test9-src" );

        File target = new File( getTestBaseDir(), "test9-target" );

        long now = System.currentTimeMillis();

        // ----------------------------------------------------------------------
        // The output file is missing
        // ----------------------------------------------------------------------

        File fooCs = new File( src, "Foo.cs" );

        writeFile( fooCs );

        fooCs.setLastModified( now - 10000 );

        SourceMapping mapping = new SingleTargetSourceMapping( ".cs", "Application.exe" );

        scanner = new StaleSourceScanner( 0 );

        scanner.addSourceMapping( mapping );

        Set<File> result = scanner.getIncludedSources( src, target );

        assertThat( result.size(), is( 1 ) );

        assertThat( result, contains( fooCs ) );

        // ----------------------------------------------------------------------
        // Add another source file
        // ----------------------------------------------------------------------

        File barCs = new File( src, "Bar.cs" );

        writeFile( barCs );

        barCs.setLastModified( now - 20000 );

        result = scanner.getIncludedSources( src, target );

        assertThat( result.size(), is( 2 ) );

        assertThat( result, containsInAnyOrder( fooCs, barCs ) );



        // ----------------------------------------------------------------------
        // Now add the result file
        // ----------------------------------------------------------------------

        File applicationExe = new File( target, "Application.exe" );

        writeFile( applicationExe );

        applicationExe.setLastModified( now );

        result = scanner.getIncludedSources( src, target );

        assertThat( result, empty() );


        // ----------------------------------------------------------------------
        // Make Application.exe older than the Foo.cs
        // ----------------------------------------------------------------------

        applicationExe.setLastModified( now - 15000 );

        result = scanner.getIncludedSources( src, target );

        assertThat( result.size(), is( 1 ) );

        assertThat( result, contains( fooCs ) );
    }

}
