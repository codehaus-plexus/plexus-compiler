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

import java.util.Arrays;
import java.util.Collection;

import org.codehaus.plexus.compiler.AbstractCompilerTest;
import org.codehaus.plexus.compiler.CompilerConfiguration;

/**
 * @author <a href="mailto:jason@plexus.org">Jason van Zyl</a>
 * @author <a href="jason.faust@gmail.com">Jason Faust</a>
 */
public abstract class AbstractEclipseCompilerTest extends AbstractCompilerTest {

    private int expectedErrors;
    private int expectedWarnings;
    private int expectedNotes;
    private Collection<String> expectedOutputFiles;

    protected AbstractEclipseCompilerTest() {
        this(5, 1, 1);
    }

    protected AbstractEclipseCompilerTest(int expectedErrors, int expectedWarnings, int expectedNotes) {
        this(expectedErrors, expectedWarnings, expectedNotes, Arrays
                .asList(new String[] {
                        "org/codehaus/foo/Deprecation.class",
                        "org/codehaus/foo/ExternalDeps.class",
                        "org/codehaus/foo/Info.class",
                        "org/codehaus/foo/Person.class" }));
    }

    protected AbstractEclipseCompilerTest(int expectedErrors, int expectedWarnings, int expectedNotes,
            Collection<String> expectedOutputFiles) {
        this.expectedErrors = expectedErrors;
        this.expectedWarnings = expectedWarnings;
        this.expectedNotes = expectedNotes;
        this.expectedOutputFiles = expectedOutputFiles;
    }

    public void setUp() throws Exception {
        super.setUp();

        setCompilerDebug(true);
        setCompilerDeprecationWarnings(true);
    }

    @Override
    protected String getRoleHint() {
        return "eclipse";
    }

    @Override
    protected int expectedErrors() {
        return expectedErrors;
    }

    @Override
    protected int expectedWarnings() {
        return expectedWarnings;
    }

    @Override
    protected int expectedNotes() {
        return expectedNotes;
    }

    protected Collection<String> expectedOutputFiles() {
        return expectedOutputFiles;
    }

    @Override
    protected void configureCompilerConfig(CompilerConfiguration compilerConfig) {
        compilerConfig.setSourceVersion("1.8");
        compilerConfig.setTargetVersion("1.8");
    }
}
