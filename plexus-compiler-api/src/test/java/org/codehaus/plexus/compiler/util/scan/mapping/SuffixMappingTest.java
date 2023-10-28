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

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

/**
 * @author jdcasey
 */
public class SuffixMappingTest {

    @Test
    public void testShouldReturnSingleClassFileForSingleJavaFile() {
        String base = "path/to/file";

        File basedir = new File(".");

        SuffixMapping mapping = new SuffixMapping(".java", ".class");

        Set<File> results = mapping.getTargetFiles(basedir, base + ".java");

        assertThat("Returned wrong number of target files.", results.size(), is(1));

        assertThat("Target file is wrong.", results.iterator().next(), is(new File(basedir, base + ".class")));
    }

    @Test
    public void testShouldNotReturnClassFileWhenSourceFileHasWrongSuffix() {
        String base = "path/to/file";

        File basedir = new File(".");

        SuffixMapping mapping = new SuffixMapping(".java", ".class");

        Set<File> results = mapping.getTargetFiles(basedir, base + ".xml");

        assertThat("Returned wrong number of target files.", results, empty());
    }

    @Test
    public void testShouldReturnOneClassFileAndOneXmlFileForSingleJavaFile() {
        String base = "path/to/file";

        File basedir = new File(".");

        Set<String> targets = new HashSet<>();
        targets.add(".class");
        targets.add(".xml");

        SuffixMapping mapping = new SuffixMapping(".java", targets);

        Set<File> results = mapping.getTargetFiles(basedir, base + ".java");

        assertThat("Returned wrong number of target files.", results.size(), is(2));

        assertThat(
                "Targets do not contain class target.",
                results,
                containsInAnyOrder(new File(basedir, base + ".class"), new File(basedir, base + ".xml")));
    }

    @Test
    public void testShouldReturnNoTargetFilesWhenSourceFileHasWrongSuffix() {
        String base = "path/to/file";

        File basedir = new File(".");

        Set<String> targets = new HashSet<>();
        targets.add(".class");
        targets.add(".xml");

        SuffixMapping mapping = new SuffixMapping(".java", targets);

        Set<File> results = mapping.getTargetFiles(basedir, base + ".apt");

        assertThat("Returned wrong number of target files.", results, empty());
    }

    @Test
    public void testSingleTargetMapper() throws Exception {
        String base = "path/to/file";

        File basedir = new File("target/");

        SingleTargetSourceMapping mapping = new SingleTargetSourceMapping(".cs", "/foo");

        Set<File> results = mapping.getTargetFiles(basedir, base + ".apt");

        assertThat(results, empty());

        results = mapping.getTargetFiles(basedir, base + ".cs");

        assertThat(results.size(), is(1));
    }
}
