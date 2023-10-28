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
import javax.inject.Inject;
import javax.inject.Named;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.codehaus.plexus.compiler.Compiler;
import org.codehaus.plexus.compiler.CompilerConfiguration;
import org.codehaus.plexus.testing.PlexusTest;
import org.codehaus.plexus.util.FileUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.codehaus.plexus.testing.PlexusExtension.getBasedir;

/**
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on 22-4-18.
 */
@PlexusTest
public class EclipseCompilerDashedArgumentsTest {

    public static final String BAD_DOUBLEDASH_OPTION = "--grubbelparkplace";

    @Inject
    @Named("eclipse")
    Compiler compiler;

    private CompilerConfiguration getConfig() throws Exception {
        String sourceDir = getBasedir() + "/src/test-input/src/main";

        List<String> filenames = FileUtils.getFileNames(new File(sourceDir), "**/*.java", null, false, true);
        Collections.sort(filenames);
        Set<File> files = new HashSet<>();
        for (String filename : filenames) {
            files.add(new File(filename));
        }

        CompilerConfiguration compilerConfig = new CompilerConfiguration();
        compilerConfig.setDebug(false);
        compilerConfig.setShowDeprecation(false);

        //            compilerConfig.setClasspathEntries( getClasspath() );
        compilerConfig.addSourceLocation(sourceDir);
        compilerConfig.setOutputLocation(getBasedir() + "/target/eclipse/classes");
        FileUtils.deleteDirectory(compilerConfig.getOutputLocation());
        //        compilerConfig.addInclude( filename );
        compilerConfig.setForceJavacCompilerUse(false);
        compilerConfig.setSourceFiles(files);

        compilerConfig.setTargetVersion("1.8");
        compilerConfig.setSourceVersion("1.8");
        return compilerConfig;
    }

    /**
     * Start the eclipse compiler with a bad option that has two dashes. It should abort, and the error
     * message should show the actual bad option with two dashes. This ensures that both dashes are passed
     * to the compiler proper.
     *
     * This also tests that con-compile errors are shown properly, as the error caused by
     * the invalid option is not part of the error output but part of the data sent to stdout/stderr.
     */
    @Test
    public void testDoubleDashOptionsArePassedWithTwoDashes() throws Exception {
        CompilerConfiguration config = getConfig();
        config.addCompilerCustomArgument(BAD_DOUBLEDASH_OPTION, "b0rk3d");

        EcjFailureException x =
                Assertions.assertThrows(EcjFailureException.class, () -> compiler.performCompile(config));

        MatcherAssert.assertThat(x.getEcjOutput(), Matchers.containsString(BAD_DOUBLEDASH_OPTION));
        MatcherAssert.assertThat(x.getEcjOutput(), Matchers.not(Matchers.containsString("-" + BAD_DOUBLEDASH_OPTION)));
    }
}
