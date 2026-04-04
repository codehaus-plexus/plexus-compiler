package org.codehaus.plexus.compiler.csharp;

/**
 * The MIT License
 *
 * Copyright (c) 2005, The Codehaus
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for JarUtil to verify protection against Zip Slip vulnerability.
 */
public class JarUtilTest {

    @Test
    public void testZipSlipProtection(@TempDir Path tempDir) throws Exception {
        // Create a malicious JAR with a path traversal entry
        File jarFile = tempDir.resolve("malicious.jar").toFile();
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarFile))) {
            // Try to escape the extraction directory using path traversal
            JarEntry entry = new JarEntry("../../evil.txt");
            jos.putNextEntry(entry);
            jos.write("malicious content".getBytes());
            jos.closeEntry();
        }

        // Create extraction directory
        Path extractDir = tempDir.resolve("extract");
        Files.createDirectories(extractDir);

        // Attempt to extract - should throw IOException due to path traversal attempt
        IOException exception = assertThrows(IOException.class, () -> {
            JarUtil.extract(extractDir, jarFile);
        });

        assertEquals("Bad zip entry", exception.getMessage());

        // Verify that the file was not created outside the extraction directory
        Path evilFile = tempDir.resolve("evil.txt");
        assertFalse(Files.exists(evilFile), "Evil file should not exist");
    }

    @Test
    public void testNormalExtraction(@TempDir Path tempDir) throws Exception {
        // Create a normal JAR with valid entries
        File jarFile = tempDir.resolve("normal.jar").toFile();
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarFile))) {
            // Add a normal file entry
            JarEntry entry = new JarEntry("file.txt");
            jos.putNextEntry(entry);
            jos.write("normal content".getBytes());
            jos.closeEntry();

            // Add a file in a subdirectory
            JarEntry dirEntry = new JarEntry("subdir/");
            jos.putNextEntry(dirEntry);
            jos.closeEntry();

            JarEntry subFileEntry = new JarEntry("subdir/subfile.txt");
            jos.putNextEntry(subFileEntry);
            jos.write("subdirectory content".getBytes());
            jos.closeEntry();
        }

        // Create extraction directory
        Path extractDir = tempDir.resolve("extract");
        Files.createDirectories(extractDir);

        // Extract the JAR - should succeed without exception
        JarUtil.extract(extractDir, jarFile);

        // Verify files were created correctly
        Path extractedFile = extractDir.resolve("file.txt");
        assertTrue(Files.exists(extractedFile), "File should exist");
        assertEquals("normal content", new String(Files.readAllBytes(extractedFile)), "File content should match");

        Path extractedSubFile = extractDir.resolve("subdir/subfile.txt");
        assertTrue(Files.exists(extractedSubFile), "Subdir file should exist");
        assertEquals(
                "subdirectory content",
                new String(Files.readAllBytes(extractedSubFile)),
                "Subdir file content should match");
    }

    @Test
    public void testZipSlipWithComplexPath(@TempDir Path tempDir) throws Exception {
        // Create a malicious JAR with a more complex path traversal
        File jarFile = tempDir.resolve("complex-malicious.jar").toFile();
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarFile))) {
            // Use a path like "subdir/../../evil.txt"
            JarEntry entry = new JarEntry("subdir/../../evil.txt");
            jos.putNextEntry(entry);
            jos.write("malicious content".getBytes());
            jos.closeEntry();
        }

        // Create extraction directory
        Path extractDir = tempDir.resolve("extract");
        Files.createDirectories(extractDir);

        // Attempt to extract - should throw IOException due to path traversal attempt
        IOException exception = assertThrows(IOException.class, () -> {
            JarUtil.extract(extractDir, jarFile);
        });

        assertEquals("Bad zip entry", exception.getMessage());

        // Verify that the file was not created outside the extraction directory
        Path evilFile = tempDir.resolve("evil.txt");
        assertFalse(Files.exists(evilFile), "Evil file should not exist");
    }
}
