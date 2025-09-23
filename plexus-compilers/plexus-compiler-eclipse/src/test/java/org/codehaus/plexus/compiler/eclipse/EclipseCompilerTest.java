package org.codehaus.plexus.compiler.eclipse;

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
import java.util.Arrays;
import java.util.Collection;

import org.codehaus.plexus.compiler.AbstractCompilerTest;
import org.codehaus.plexus.compiler.CompilerConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author <a href="mailto:jason@plexus.org">Jason van Zyl</a>
 */
public class EclipseCompilerTest extends AbstractCompilerTest {

    @BeforeEach
    public void setUp() {
        setCompilerDebug(true);
        setCompilerDeprecationWarnings(true);
    }

    @Override
    protected String getRoleHint() {
        return "eclipse";
    }

    @Override
    protected int expectedErrors() {
        return 5;
    }

    @Override
    protected int expectedWarnings() {
        return 1;
    }

    @Override
    protected Collection<String> expectedOutputFiles() {
        String javaVersion = getJavaVersion();
        if (javaVersion.contains("9.0")
                || javaVersion.contains("11")
                || javaVersion.contains("17")
                || javaVersion.contains("21")
                || javaVersion.contains("25")) {
            return Arrays.asList(
                    "org/codehaus/foo/Deprecation.class",
                    "org/codehaus/foo/ExternalDeps.class",
                    "org/codehaus/foo/Person.class");
        }
        return Arrays.asList(
                "org/codehaus/foo/Deprecation.class",
                "org/codehaus/foo/ExternalDeps.class",
                "org/codehaus/foo/Person.class",
                "org/codehaus/foo/ReservedWord.class");
    }

    // The test is fairly meaningless as we can not validate anything
    @Test
    public void testCustomArgument() throws Exception {
        CompilerConfiguration compilerConfig = createMinimalCompilerConfig();

        compilerConfig.addCompilerCustomArgument("-key", "value");

        getCompiler().performCompile(compilerConfig);
    }

    @Test
    public void testInitializeWarningsForPropertiesArgument() {
        CompilerConfiguration compilerConfig = createMinimalCompilerConfig();

        compilerConfig.addCompilerCustomArgument("-properties", "file_does_not_exist");

        IllegalArgumentException e =
                assertThrows(IllegalArgumentException.class, () -> getCompiler().performCompile(compilerConfig));
        assertThat("Message must start with 'Properties file'", e.getMessage(), startsWith("Properties file"));
    }

    private CompilerConfiguration createMinimalCompilerConfig() {
        CompilerConfiguration compilerConfig = new CompilerConfiguration();
        compilerConfig.setOutputLocation("target/" + getRoleHint() + "/classes-CustomArgument");
        return compilerConfig;
    }
}
