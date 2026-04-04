package org.codehaus.plexus.compiler;

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
import javax.inject.Inject;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import org.codehaus.plexus.testing.PlexusTest;
import org.codehaus.plexus.util.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 */
@PlexusTest
public abstract class AbstractCompilerTckTest {
    private static final String EOL = System.lineSeparator();

    protected String roleHint;

    private TestInfo testInfo;

    @Inject
    private Map<String, Compiler> compilers;

    @BeforeEach
    final void setup(TestInfo testInfo) {
        this.testInfo = testInfo;
    }

    @Test
    public void testDeprecation() throws Exception {
        File foo = new File(getSrc(), "Foo.java");

        writeFileWithDeprecatedApi(foo, "Foo");

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        CompilerConfiguration configuration = new CompilerConfiguration();

        configuration.setShowDeprecation(true);

        configuration.addSourceLocation(getSrc().getAbsolutePath());

        List<CompilerMessage> result = compile(configuration);

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        assertEquals(1, result.size());

        CompilerMessage error = result.get(0);

        assertFalse(error.isError());

        assertTrue(error.getMessage().contains("Date"));

        assertTrue(error.getMessage().contains("deprecated"));
    }

    @Test
    public void testWarning() throws Exception {
        File foo = new File(getSrc(), "Foo.java");

        writeFileWithWarning(foo, "Foo");

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        CompilerConfiguration configuration = new CompilerConfiguration();

        configuration.setShowWarnings(true);

        configuration.addSourceLocation(getSrc().getAbsolutePath());

        List<CompilerMessage> result = compile(configuration);

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        assertEquals(1, result.size());

        CompilerMessage error = result.get(0);

        assertFalse(error.isError());

        assertTrue(error.getMessage().contains("finally block does not complete normally"));
    }

    protected List<CompilerMessage> compile(CompilerConfiguration configuration) throws Exception {
        // ----------------------------------------------------------------------
        // Set up configuration
        // ----------------------------------------------------------------------

        File compilerOutput = getCompilerOutput();

        if (compilerOutput.exists()) {
            org.apache.commons.io.FileUtils.deleteDirectory(compilerOutput);
        }

        configuration.setOutputLocation(compilerOutput.getAbsolutePath());

        // ----------------------------------------------------------------------
        // Compile!
        // ----------------------------------------------------------------------

        List<CompilerMessage> result =
                getCompiler().performCompile(configuration).getCompilerMessages();

        assertNotNull(result);

        return result;
    }

    private Compiler getCompiler() {
        return compilers.get(roleHint);
    }

    private File getCompilerOutput() {
        return new File("target/compiler-output/"
                + testInfo.getTestMethod().map(Method::getName).orElseThrow(null));
    }

    private File getSrc() {
        return new File("target/compiler-src/"
                + testInfo.getTestMethod().map(Method::getName).orElseThrow(null));
    }

    protected void writeFileWithDeprecatedApi(File path, String className) throws IOException {
        File parent = path.getParentFile();

        if (!parent.exists()) {
            assertTrue(parent.mkdirs());
        }

        String source = "import java.util.Date;" + EOL + ""
                + EOL + "public class "
                + className + "" + EOL + "{"
                + EOL + "    private static Date date = new Date( \"foo\" );"
                + EOL + "    static "
                + EOL + "    { "
                + EOL + "        Date date = "
                + className + ".date; " + EOL + "        Date date2 = date; "
                + EOL + "        date = date2; "
                + EOL + "    }"
                + EOL + "}";

        FileUtils.fileWrite(path.getAbsolutePath(), source);
    }

    protected void writeFileWithWarning(File path, String className) throws IOException {
        File parent = path.getParentFile();

        if (!parent.exists()) {
            assertTrue(parent.mkdirs());
        }

        String source = "public class " + className + "" + EOL + "{"
                + EOL + "    public void foo()"
                + EOL + "    {"
                + EOL + "        try{ throw new java.io.IOException(); }"
                + EOL + "        finally { return; }"
                + EOL + "    }"
                + EOL + "}";

        FileUtils.fileWrite(path.getAbsolutePath(), source);
    }
}
