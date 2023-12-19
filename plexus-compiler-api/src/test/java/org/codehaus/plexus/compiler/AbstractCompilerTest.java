package org.codehaus.plexus.compiler;

import java.io.File;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbstractCompilerTest {

    @Test
    void getSourceFilesForSourceRootShouldReturnEmptyForNotExistingLocation() {

        CompilerConfiguration config = new CompilerConfiguration();
        File fileLocation = new File("non/existing/location").getAbsoluteFile();

        assertFalse(fileLocation.exists());

        Set<String> sourcesFile = AbstractCompiler.getSourceFilesForSourceRoot(config, fileLocation.getAbsolutePath());

        assertTrue(sourcesFile.isEmpty());
    }
}
