package org.codehaus.plexus.compiler.eclipse;

import java.io.File;
import java.util.List;

import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.compiler.CompilerMessage;
import org.codehaus.plexus.compiler.CompilerMessage.Kind;
import org.junit.Test;

/**
 * Tests for {@link EcjTextLogParser}. Tests are written against ecj 3.15.0
 * output.
 * 
 * @author <a href="mailto:jfaust@tsunamit.com">Jason Faust</a>
 * @since 2.8.6
 */
public class EcjTextLogParserTest extends PlexusTestCase {

    private static final String LOG_FILE_1 = "src/test/resources/org/codehaus/plexus/compiler/eclipse/3.15.0.log.txt";
    private static final String LOG_FILE_2 = "src/test/resources/org/codehaus/plexus/compiler/eclipse/log-text-0.log";
    private static final String LOG_FILE_3 = "src/test/resources/org/codehaus/plexus/compiler/eclipse/eclipse-withinfo.txt";
    private static final String LOG_FILE_4 = "src/test/resources/org/codehaus/plexus/compiler/eclipse/eclipse-columns.txt";

    /**
     * Test for example compiler output in non-XML format.
     * 
     * @throws Exception on test failure.
     */
    @Test
    public void testParse() throws Exception {
        File logFile = getTestFile(LOG_FILE_1);
        assertTrue("Input log file missing", logFile.exists());

        EcjTextLogParser parser = new EcjTextLogParser();
        List<CompilerMessage> cm = parser.parse(logFile, false);

        assertEquals("Wrong message count", 9, cm.size());

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
     * Test for example compiler output in non-XML format with errors reported as
     * warnings.
     * 
     * @throws Exception on test failure.
     */
    @Test
    public void testParseErrorsAsWarnings() throws Exception {
        File logFile = getTestFile(LOG_FILE_1);
        assertTrue("Input log file missing", logFile.exists());

        EcjTextLogParser parser = new EcjTextLogParser();
        List<CompilerMessage> cm = parser.parse(logFile, true);

        assertEquals("Wrong message count", 9, cm.size());

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
     * Test for ignoring output before first output delimiter.
     * 
     * @throws Exception on test failure.
     */
    @Test
    public void testHeaderErrors() throws Exception {
        File logFile = getTestFile(LOG_FILE_2);
        assertTrue("Input log file missing", logFile.exists());

        EcjTextLogParser parser = new EcjTextLogParser();
        List<CompilerMessage> cm = parser.parse(logFile, false);

        assertEquals("Wrong message count", 2, cm.size());

        checkCompilerMessage(cm.get(0), "Bad.java", Kind.ERROR, 6, 1, 5);
        checkCompilerMessage(cm.get(1), "Bad.java", Kind.ERROR, 6, 1, 5);
    }

    /**
     * Tests for parsing the contents of {@value #LOG_FILE_3}.
     * 
     * @throws Exception on test failure.
     */
    public void testParse3() throws Exception {
        File logFile = getTestFile(LOG_FILE_3);
        assertTrue("Input log file missing", logFile.exists());

        EcjTextLogParser parser = new EcjTextLogParser();
        List<CompilerMessage> cm = parser.parse(logFile, false);

        assertEquals("Wrong message count", 6, cm.size());

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
        assertTrue("Input log file missing", logFile.exists());

        EcjTextLogParser parser = new EcjTextLogParser();
        List<CompilerMessage> cm = parser.parse(logFile, false);

        assertEquals("Wrong message count", 1, cm.size());

        checkCompilerMessage(cm.get(0), "Column.java", Kind.ERROR, 2, 1, 5);
    }

    private void checkCompilerMessage(CompilerMessage cm, String file, Kind kind, int line, int startCol, int endCol) {
        assertTrue("Failure checking output for " + file, cm.getFile().endsWith(file));
        assertEquals("Failure checking output for " + file, kind, cm.getKind());
        assertEquals("Failure checking output for " + file, line, cm.getStartLine());
        assertEquals("Failure checking output for " + file, startCol, cm.getStartColumn());
        assertEquals("Failure checking output for " + file, endCol, cm.getEndColumn());
    }

}
