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
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Set;

import org.codehaus.plexus.compiler.util.scan.mapping.SuffixMapping;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.io.FileMatchers.anExistingFile;

/**
 * Tests for all the implementations of <code>SourceInclusionScanner</code>
 *
 * @author <a href="mailto:carlos@apache.org">Carlos Sanchez</a>
 */
public abstract class AbstractSourceInclusionScannerTest {

    private static final String TESTFILE_DEST_MARKER_FILE =
            SourceInclusionScanner.class.getName().replace('.', '/') + "-testMarker.txt";

    protected SourceInclusionScanner scanner;

    @Test
    public void testGetIncludedSources() throws Exception {
        File base = new File(getTestBaseDir(), "testGetIncludedSources");

        File sourceFile = new File(base, "file.java");

        writeFile(sourceFile);

        sourceFile.setLastModified(System.currentTimeMillis());

        SuffixMapping mapping = new SuffixMapping(".java", ".xml");

        scanner.addSourceMapping(mapping);

        Set<File> includedSources = scanner.getIncludedSources(base, base);

        assertThat("no sources were included", includedSources, not(empty()));

        for (File file : includedSources) {
            assertThat("file included does not exist", file, anExistingFile());
        }
    }

    // ----------------------------------------------------------------------
    // Utilities
    // ----------------------------------------------------------------------

    protected File getTestBaseDir() throws URISyntaxException {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        URL markerResource = cl.getResource(TESTFILE_DEST_MARKER_FILE);

        File basedir;

        if (markerResource != null) {
            File marker = new File(markerResource.toURI());

            basedir = marker.getParentFile().getAbsoluteFile();
        } else {
            // punt.
            System.out.println("Cannot find marker file: \'" + TESTFILE_DEST_MARKER_FILE + "\' in classpath. "
                    + "Using '.' for basedir.");

            basedir = new File(".").getAbsoluteFile();
        }

        return basedir;
    }

    protected void writeFile(File file) throws IOException {

        File parent = file.getParentFile();
        if (!parent.exists()) {
            parent.mkdirs();
        }

        file.deleteOnExit();

        try (FileWriter fWriter = new FileWriter(file)) {
            fWriter.write("This is just a test file.");
        }
    }
}
