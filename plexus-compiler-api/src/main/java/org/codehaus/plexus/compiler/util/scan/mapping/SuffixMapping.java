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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author jdcasey
 */
public final class SuffixMapping implements SourceMapping {
    private final String sourceSuffix;

    private final Set<String> targetSuffixes;

    public SuffixMapping(String sourceSuffix, String targetSuffix) {
        this.sourceSuffix = sourceSuffix;

        this.targetSuffixes = Collections.singleton(targetSuffix);
    }

    public SuffixMapping(String sourceSuffix, Set<String> targetSuffixes) {
        this.sourceSuffix = sourceSuffix;

        this.targetSuffixes = Collections.unmodifiableSet(targetSuffixes);
    }

    public Set<File> getTargetFiles(File targetDir, String source) {
        Set<File> targetFiles = new HashSet<>();

        if (source.endsWith(sourceSuffix)) {
            String base = source.substring(0, source.length() - sourceSuffix.length());

            for (String suffix : targetSuffixes) {
                targetFiles.add(new File(targetDir, base + suffix));
            }
        }

        return targetFiles;
    }
}
