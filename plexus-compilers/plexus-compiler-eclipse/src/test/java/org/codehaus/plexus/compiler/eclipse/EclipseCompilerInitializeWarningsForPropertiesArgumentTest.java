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

import java.util.Collections;

import org.codehaus.plexus.compiler.CompilerConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author <a href="mailto:jason@plexus.org">Jason van Zyl</a>
 * @author <a href="jfaust@tsunamit.com">Jason Faust</a>
 */
public class EclipseCompilerInitializeWarningsForPropertiesArgumentTest extends AbstractEclipseCompilerTest {

    @SuppressWarnings("unchecked")
    public EclipseCompilerInitializeWarningsForPropertiesArgumentTest() {
        super(0, 0, 0, Collections.EMPTY_LIST);
    }

    @Override
    protected void configureCompilerConfig(CompilerConfiguration compilerConfig) {
        super.configureCompilerConfig(compilerConfig);
        compilerConfig.addCompilerCustomArgument("-properties", "file_does_not_exist");
    }

    @Test
    @Override
    public void testCompilingSources() throws Exception {
        EcjFailureException exception = assertThrows(
                EcjFailureException.class,
                super::testCompilingSources,
                "looking up the properties file should have thrown an exception");
        assertTrue(
                exception.getMessage().startsWith("Failed to run the ecj compiler: Properties file"),
                "Unexpected compiler error");
    }
}
