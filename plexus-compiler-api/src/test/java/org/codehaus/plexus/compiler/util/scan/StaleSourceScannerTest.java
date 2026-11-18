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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author jdcasey
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 */
class StaleSourceScannerTest extends AbstractSourceInclusionScannerTest {

    @BeforeEach
    void setUp() {
        scanner = new StaleSourceScanner();
    }

    @Test
    void defaultConstructorFindsOneStaleSource() throws Exception {
        File base = new File(getTestBaseDir(), "test1");
        long now = System.currentTimeMillis();

        writeFile(new File(base, "file.xml"), now - 60000);
        File sourceFile = writeFile(new File(base, "file.java"), now);

        addJavaToXmlMapping();

        Set<File> result = scanner.getIncludedSources(base, base);

        assertAll(
                () -> assertEquals(1, result.size(), "wrong number of stale sources returned"),
                () -> assertTrue(result.contains(sourceFile), "expected stale source file not found in result"));
    }

    @Test
    void defaultConstructorDoesNotFindStaleSourcesForUpToDateTarget() throws Exception {
        File base = new File(getTestBaseDir(), "test2");
        long now = System.currentTimeMillis();

        writeFile(new File(base, "file.java"), now - 60000);
        writeFile(new File(base, "file.xml"), now);

        addJavaToXmlMapping();

        assertTrue(scanner.getIncludedSources(base, base).isEmpty(), "expected no stale sources");
    }

    @Test
    void defaultConstructorFindsStaleSourceWhenTargetIsMissing() throws Exception {
        File base = new File(getTestBaseDir(), "test3");

        File sourceFile = writeFile(new File(base, "file.java"), System.currentTimeMillis());

        addJavaToXmlMapping();

        Set<File> result = scanner.getIncludedSources(base, base);

        assertAll(
                () -> assertEquals(1, result.size(), "wrong number of stale sources returned"),
                () -> assertTrue(result.contains(sourceFile), "expected stale source file not found in result"));
    }

    @Test
    void defaultConstructorFindsMissingTargetAndStaleTarget() throws Exception {
        File base = new File(getTestBaseDir(), "test4");
        long now = System.currentTimeMillis();

        writeFile(new File(base, "file2.xml"), now - 60000);
        File sourceFile = writeFile(new File(base, "file.java"), now);
        File sourceFile2 = writeFile(new File(base, "file2.java"), now);

        addJavaToXmlMapping();

        Set<File> result = scanner.getIncludedSources(base, base);

        assertAll(
                () -> assertEquals(2, result.size(), "wrong number of stale sources returned"),
                () -> assertTrue(result.contains(sourceFile), "expected stale source file not found in result"),
                () -> assertTrue(result.contains(sourceFile2), "expected stale source file not found in result"));
    }

    @Test
    void defaultConstructorOmitsUpToDateSource() throws Exception {
        File base = new File(getTestBaseDir(), "test5");
        long now = System.currentTimeMillis();

        // target/source (1): stale target, fresh source -> source is included.
        writeFile(new File(base, "file.xml"), now - 60000);
        File sourceFile = writeFile(new File(base, "file.java"), now);

        // target/source (2): fresh target, stale source -> source is omitted.
        writeFile(new File(base, "file2.java"), now - 60000);
        writeFile(new File(base, "file2.xml"), now);

        addJavaToXmlMapping();

        Set<File> result = scanner.getIncludedSources(base, base);

        assertAll(
                () -> assertEquals(1, result.size(), "wrong number of stale sources returned"),
                () -> assertTrue(result.contains(sourceFile), "expected stale source file not found in result"));
    }

    @Test
    void msecsThresholdFiltersSourcesByTargetAge() throws Exception {
        File base = new File(getTestBaseDir(), "test6");
        long now = System.currentTimeMillis();

        writeFile(new File(base, "file.xml"), now - 8000);
        writeFile(new File(base, "file.java"), now);
        writeFile(new File(base, "file2.xml"), now - 12000);
        File sourceFile2 = writeFile(new File(base, "file2.java"), now);

        scanner = new StaleSourceScanner(10000);
        addJavaToXmlMapping();

        Set<File> result = scanner.getIncludedSources(base, base);

        assertAll(
                () -> assertEquals(1, result.size(), "wrong number of stale sources returned"),
                () -> assertTrue(result.contains(sourceFile2), "expected stale source file not found in result"));
    }

    @Test
    void includePatternSelectsOnlyMatchingSources() throws Exception {
        File base = new File(getTestBaseDir(), "test7");
        long now = System.currentTimeMillis();

        for (int i = 1; i <= 3; i++) {
            writeFile(new File(base, "file" + i + ".xml"), now - 12000);
            writeFile(new File(base, "file" + i + ".java"), now);
        }

        scanner = new StaleSourceScanner(0, Collections.singleton("*3.java"), Collections.emptySet());
        addJavaToXmlMapping();

        Set<File> result = scanner.getIncludedSources(base, base);

        assertAll(
                () -> assertEquals(1, result.size(), "wrong number of stale sources returned"),
                () -> assertTrue(result.contains(new File(base, "file3.java")), "expected source 3 only"));
    }

    @Test
    void excludePatternDropsMatchingSources() throws Exception {
        File base = new File(getTestBaseDir(), "test8");
        long now = System.currentTimeMillis();

        writeFile(new File(base, "fileX.xml"), now - 12000);
        writeFile(new File(base, "fileX.java"), now);
        for (int i = 2; i <= 3; i++) {
            writeFile(new File(base, "file" + i + ".xml"), now - 12000);
            writeFile(new File(base, "file" + i + ".java"), now);
        }

        scanner = new StaleSourceScanner(0, Collections.singleton("**/*"), Collections.singleton("*X.*"));
        addJavaToXmlMapping();

        Set<File> result = scanner.getIncludedSources(base, base);

        assertAll(
                () -> assertEquals(2, result.size(), "wrong number of stale sources returned"),
                () -> assertTrue(result.contains(new File(base, "file2.java")), "expected source 2"),
                () -> assertTrue(result.contains(new File(base, "file3.java")), "expected source 3"));
    }

    @Test
    void singleTargetSourceMappingTracksOutputFile() throws Exception {
        File src = new File(getTestBaseDir(), "test9-src");
        File target = new File(getTestBaseDir(), "test9-target");
        long now = System.currentTimeMillis();

        // The output file is missing.
        File fooCs = writeFile(new File(src, "Foo.cs"), now - 10000);

        SourceMapping mapping = new SingleTargetSourceMapping(".cs", "Application.exe");
        scanner = new StaleSourceScanner(0);
        scanner.addSourceMapping(mapping);

        Set<File> missingTarget = scanner.getIncludedSources(src, target);
        assertAll(() -> assertEquals(1, missingTarget.size()), () -> assertTrue(missingTarget.contains(fooCs)));

        // Add another source file.
        File barCs = writeFile(new File(src, "Bar.cs"), now - 20000);

        Set<File> twoSources = scanner.getIncludedSources(src, target);
        assertAll(
                () -> assertEquals(2, twoSources.size()),
                () -> assertTrue(twoSources.contains(fooCs)),
                () -> assertTrue(twoSources.contains(barCs)));

        // Add the result file, newer than both sources.
        File applicationExe = writeFile(new File(target, "Application.exe"), now);
        assertTrue(scanner.getIncludedSources(src, target).isEmpty());

        // Make the result older than Foo.cs again.
        applicationExe.setLastModified(now - 15000);

        Set<File> staleTarget = scanner.getIncludedSources(src, target);
        assertAll(() -> assertEquals(1, staleTarget.size()), () -> assertTrue(staleTarget.contains(fooCs)));
    }

    private void addJavaToXmlMapping() {
        scanner.addSourceMapping(new SuffixMapping(".java", ".xml"));
    }
}
