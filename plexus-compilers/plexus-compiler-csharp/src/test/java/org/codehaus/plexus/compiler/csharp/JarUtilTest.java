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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for JarUtil to verify protection against Zip Slip vulnerability.
 */
class JarUtilTest {

    @TempDir
    Path tempDir;

    @ParameterizedTest(name = "rejects traversal entry: {0}")
    @ValueSource(strings = {"../../evil.txt", "subdir/../../evil.txt"})
    @DisplayName("Path traversal entries are rejected")
    void zipSlipProtection(String entryName) throws Exception {
        File jarFile = createJar("malicious.jar", entryName, "malicious content");
        Path extractDir = Files.createDirectories(tempDir.resolve("extract"));

        IOException exception = assertThrows(IOException.class, () -> JarUtil.extract(extractDir, jarFile));

        assertAll(
                () -> assertEquals("Bad zip entry", exception.getMessage()),
                () -> assertFalse(Files.exists(tempDir.resolve("evil.txt")), "Evil file should not exist"));
    }

    @Test
    void normalExtraction() throws Exception {
        File jarFile = tempDir.resolve("normal.jar").toFile();
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarFile))) {
            addEntry(jos, "file.txt", "normal content");
            addEntry(jos, "subdir/", null);
            addEntry(jos, "subdir/subfile.txt", "subdirectory content");
        }

        Path extractDir = Files.createDirectories(tempDir.resolve("extract"));
        JarUtil.extract(extractDir, jarFile);

        assertExtracted(extractDir.resolve("file.txt"), "normal content");
        assertExtracted(extractDir.resolve("subdir/subfile.txt"), "subdirectory content");
    }

    private File createJar(String name, String entryName, String content) throws IOException {
        File jarFile = tempDir.resolve(name).toFile();
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarFile))) {
            addEntry(jos, entryName, content);
        }
        return jarFile;
    }

    private static void addEntry(JarOutputStream jar, String name, String content) throws IOException {
        jar.putNextEntry(new JarEntry(name));
        if (content != null) {
            jar.write(content.getBytes(StandardCharsets.UTF_8));
        }
        jar.closeEntry();
    }

    private static void assertExtracted(Path path, String expectedContent) throws IOException {
        assertAll(
                () -> assertTrue(Files.exists(path), "File should exist: " + path),
                () -> assertEquals(expectedContent, new String(Files.readAllBytes(path), StandardCharsets.UTF_8)));
    }
}
