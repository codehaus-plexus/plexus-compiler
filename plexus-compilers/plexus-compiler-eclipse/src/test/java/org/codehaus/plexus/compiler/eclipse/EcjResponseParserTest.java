/*
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
package org.codehaus.plexus.compiler.eclipse;

import java.io.File;
import java.util.List;

import org.codehaus.plexus.compiler.CompilerMessage;
import org.codehaus.plexus.compiler.CompilerMessage.Kind;
import org.codehaus.plexus.testing.PlexusTest;
import org.junit.jupiter.api.Test;

import static org.codehaus.plexus.testing.PlexusExtension.getTestFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link EcjResponseParser}. Tests are written against ecj 3.15.0
 * output.
 *
 * @author <a href="mailto:jfaust@tsunamit.com">Jason Faust</a>
 * @since 2.14.0
 */
@PlexusTest
public class EcjResponseParserTest {

    private static final String LOG_FILE_1 = "src/test/resources/org/codehaus/plexus/compiler/eclipse/3.15.0.log.xml";
    private static final String LOG_FILE_2 = "src/test/resources/org/codehaus/plexus/compiler/eclipse/log-xml-0.xml";
    private static final String LOG_FILE_3 =
            "src/test/resources/org/codehaus/plexus/compiler/eclipse/eclipse-withinfo.xml";
    private static final String LOG_FILE_4 =
            "src/test/resources/org/codehaus/plexus/compiler/eclipse/eclipse-columns.xml";

    /**
     * Test for example compiler output in XML format.
     *
     * @throws Exception on test failure.
     */
    @Test
    public void testParse() throws Exception {
        File logFile = getTestFile(LOG_FILE_1);
        assertTrue(logFile.exists(), "Input log file missing");

        EcjResponseParser parser = new EcjResponseParser();
        List<CompilerMessage> cm = parser.parse(logFile, false);

        assertEquals(9, cm.size(), "Wrong message count");

        checkCompilerMessage(cm.get(0), "Bad.java", Kind.ERROR, 6, 1, 5);
        checkCompilerMessage(cm.get(1), "Bad.java", Kind.ERROR, 6, 1, 5);
        checkCompilerMessage(cm.get(2), "Deprecation.java", Kind.WARNING, 7, 5, 30);
        checkCompilerMessage(cm.get(3), "ExternalDeps.java", Kind.ERROR, 4, 8, 17);
        checkCompilerMessage(cm.get(4), "ExternalDeps.java", Kind.ERROR, 12, 21, 31);
        checkCompilerMessage(cm.get(5), "Info.java", Kind.NOTE, 5, 11, 11);
        checkCompilerMessage(cm.get(6), "ReservedWord.java", Kind.ERROR, 5, 8, 13);
        checkCompilerMessage(cm.get(7), "UnknownSymbol.java", Kind.ERROR, 7, 1, 3);
        checkCompilerMessage(cm.get(8), "WrongClassname.java", Kind.ERROR, 3, 14, 27);
    }

    /**
     * Test for example compiler output in XML format with errors reported as
     * warnings.
     *
     * @throws Exception on test failure.
     */
    @Test
    public void testParseErrorsAsWarnings() throws Exception {
        File logFile = getTestFile(LOG_FILE_1);
        assertTrue(logFile.exists(), "Input log file missing");

        EcjResponseParser parser = new EcjResponseParser();
        List<CompilerMessage> cm = parser.parse(logFile, true);

        assertEquals(9, cm.size(), "Wrong message count");

        checkCompilerMessage(cm.get(0), "Bad.java", Kind.WARNING, 6, 1, 5);
        checkCompilerMessage(cm.get(1), "Bad.java", Kind.WARNING, 6, 1, 5);
        checkCompilerMessage(cm.get(2), "Deprecation.java", Kind.WARNING, 7, 5, 30);
        checkCompilerMessage(cm.get(3), "ExternalDeps.java", Kind.WARNING, 4, 8, 17);
        checkCompilerMessage(cm.get(4), "ExternalDeps.java", Kind.WARNING, 12, 21, 31);
        checkCompilerMessage(cm.get(5), "Info.java", Kind.NOTE, 5, 11, 11);
        checkCompilerMessage(cm.get(6), "ReservedWord.java", Kind.WARNING, 5, 8, 13);
        checkCompilerMessage(cm.get(7), "UnknownSymbol.java", Kind.WARNING, 7, 1, 3);
        checkCompilerMessage(cm.get(8), "WrongClassname.java", Kind.WARNING, 3, 14, 27);
    }

    /**
     * Test for ignoring top level errors.
     *
     * @throws Exception on test failure.
     */
    @Test
    public void testHeaderErrors() throws Exception {
        File logFile = getTestFile(LOG_FILE_2);
        assertTrue(logFile.exists(), "Input log file missing");

        EcjResponseParser parser = new EcjResponseParser();
        List<CompilerMessage> cm = parser.parse(logFile, false);

        assertEquals(2, cm.size(), "Wrong message count");

        checkCompilerMessage(cm.get(0), "Bad.java", Kind.ERROR, 6, 1, 5);
        checkCompilerMessage(cm.get(1), "Bad.java", Kind.ERROR, 6, 1, 5);
    }

    /**
     * Tests for parsing the contents of {@value #LOG_FILE_3}.
     *
     * @throws Exception on test failure.
     */
    public void testParse2() throws Exception {
        File logFile = getTestFile(LOG_FILE_3);
        assertTrue(logFile.exists(), "Input log file missing");

        EcjResponseParser parser = new EcjResponseParser();
        List<CompilerMessage> cm = parser.parse(logFile, false);

        assertEquals(6, cm.size(), "Wrong message count");

        checkCompilerMessage(cm.get(0), "ECE.java", Kind.ERROR, 8, 13, 16);
        checkCompilerMessage(cm.get(1), "ECE.java", Kind.ERROR, 16, 8, 40);
        checkCompilerMessage(cm.get(2), "ECE.java", Kind.WARNING, 22, 9, 9);
        checkCompilerMessage(cm.get(3), "ECE.java", Kind.WARNING, 27, 8, 40);
        checkCompilerMessage(cm.get(4), "ECE.java", Kind.NOTE, 33, 13, 18);
        checkCompilerMessage(cm.get(5), "ECE.java", Kind.NOTE, 35, 1, 95);
    }

    /**
     * Tests for parsing the contents of {@value #LOG_FILE_4}.
     *
     * @throws Exception on test failure.
     */
    public void testParse4() throws Exception {
        File logFile = getTestFile(LOG_FILE_4);
        assertTrue(logFile.exists(), "Input log file missing");

        EcjResponseParser parser = new EcjResponseParser();
        List<CompilerMessage> cm = parser.parse(logFile, false);

        assertEquals(1, cm.size(), "Wrong message count");

        checkCompilerMessage(cm.get(0), "Column.java", Kind.ERROR, 2, 1, 5);
    }

    private void checkCompilerMessage(CompilerMessage cm, String file, Kind kind, int line, int startCol, int endCol) {
        assertTrue(cm.getFile().endsWith(file), "Failure checking output for " + file);
        assertEquals(kind, cm.getKind(), "Failure checking output for " + file);
        assertEquals(line, cm.getStartLine(), "Failure checking output for " + file);
        assertEquals(startCol, cm.getStartColumn(), "Failure checking output for " + file);
        assertEquals(endCol, cm.getEndColumn(), "Failure checking output for " + file);
    }
}
