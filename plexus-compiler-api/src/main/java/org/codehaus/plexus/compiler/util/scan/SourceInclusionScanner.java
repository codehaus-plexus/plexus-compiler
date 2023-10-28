package org.codehaus.plexus.compiler.util.scan;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

import java.io.File;
import java.util.Set;

import org.codehaus.plexus.compiler.util.scan.mapping.SourceMapping;

/**
 * @author jdcasey
 */
public interface SourceInclusionScanner {
    void addSourceMapping(SourceMapping sourceMapping);

    /**
     * @param sourceDir
     * @param targetDir
     * @return <code>Set</code> of <code>File</code> objects
     * @throws InclusionScanException
     */
    Set<File> getIncludedSources(File sourceDir, File targetDir) throws InclusionScanException;
}
