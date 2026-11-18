package org.codehaus.plexus.compiler.util.scan.mapping;

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
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author jdcasey
 */
class SuffixMappingTest {
    private static final File BASEDIR = new File(".");
    private static final String BASE = "path/to/file";

    @Test
    @DisplayName("A single suffix pair yields one target file for a matching source")
    void singleClassFileForSingleJavaFile() {
        SuffixMapping mapping = new SuffixMapping(".java", ".class");

        Set<File> results = mapping.getTargetFiles(BASEDIR, BASE + ".java");

        assertAll(
                () -> assertEquals(1, results.size(), "Returned wrong number of target files"),
                () -> assertEquals(
                        new File(BASEDIR, BASE + ".class"), results.iterator().next(), "Target file is wrong"));
    }

    @Test
    void noClassFileWhenSourceHasWrongSuffix() {
        SuffixMapping mapping = new SuffixMapping(".java", ".class");

        assertTrue(mapping.getTargetFiles(BASEDIR, BASE + ".xml").isEmpty());
    }

    @Test
    void oneClassFileAndOneXmlFileForSingleJavaFile() {
        Set<String> targets = new HashSet<>();
        targets.add(".class");
        targets.add(".xml");
        SuffixMapping mapping = new SuffixMapping(".java", targets);

        Set<File> results = mapping.getTargetFiles(BASEDIR, BASE + ".java");

        assertAll(
                () -> assertEquals(2, results.size(), "Returned wrong number of target files"),
                () -> assertTrue(results.contains(new File(BASEDIR, BASE + ".class")), "Missing class target"),
                () -> assertTrue(results.contains(new File(BASEDIR, BASE + ".xml")), "Missing xml target"));
    }

    @Test
    void noTargetFilesWhenSourceHasWrongSuffix() {
        Set<String> targets = new HashSet<>();
        targets.add(".class");
        targets.add(".xml");
        SuffixMapping mapping = new SuffixMapping(".java", targets);

        assertTrue(mapping.getTargetFiles(BASEDIR, BASE + ".apt").isEmpty());
    }

    @Test
    void singleTargetMapperOnlyMatchesItsOwnSuffix() {
        File basedir = new File("target/");
        SingleTargetSourceMapping mapping = new SingleTargetSourceMapping(".cs", "/foo");

        assertAll(
                () -> assertTrue(mapping.getTargetFiles(basedir, BASE + ".apt").isEmpty()),
                () -> assertEquals(
                        1, mapping.getTargetFiles(basedir, BASE + ".cs").size()));
    }
}
