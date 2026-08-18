package org.codehaus.plexus.compiler;

/**
 * The MIT License
 *
 * Copyright (c) 2004, The Codehaus
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
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.codehaus.plexus.testing.PlexusTest;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 */
@PlexusTest
public abstract class AbstractCompilerTest {
    private boolean compilerDebug = false;

    private boolean compilerDeprecationWarnings = false;

    private boolean forceJavacCompilerUse = false;

    @Inject
    private Map<String, Compiler> compilers;

    protected abstract String getRoleHint();

    protected void setCompilerDebug(boolean flag) {
        compilerDebug = flag;
    }

    protected void setCompilerDeprecationWarnings(boolean flag) {
        compilerDeprecationWarnings = flag;
    }

    public void setForceJavacCompilerUse(boolean forceJavacCompilerUse) {
        this.forceJavacCompilerUse = forceJavacCompilerUse;
    }

    protected final Compiler getCompiler() {
        return compilers.get(getRoleHint());
    }

    protected List<String> getClasspath() throws Exception {
        List<String> cp = new ArrayList<>();

        cp.add(getJarPath("org.apache.commons.lang.StringUtils").getAbsolutePath());

        return cp;
    }

    /**
     * Locates the jar a class was loaded from, so that a test can put a dependency of its own on the classpath it
     * asks the compiler to use. The dependency is declared in the pom like any other, and found here through the
     * class loader rather than by guessing at a path inside the local repository.
     *
     * @param className fully qualified name of a class in the wanted jar
     * @return the jar holding that class
     */
    protected static File getJarPath(String className) throws Exception {
        Class<?> type = Class.forName(className);
        CodeSource source = type.getProtectionDomain().getCodeSource();
        assertNotNull(source, "test prerequisite: no code source for " + className);

        File jar = new File(source.getLocation().toURI());
        assertTrue(jar.canRead(), "test prerequisite: unreadable jar for " + className + ": " + jar);

        return jar;
    }

    protected void configureCompilerConfig(CompilerConfiguration compilerConfig) {}

    @Test
    public void testCompilingSources() throws Exception {
        List<CompilerMessage> messages = new ArrayList<>();
        Collection<String> files = new ArrayList<>();

        for (CompilerConfiguration compilerConfig : getCompilerConfigurations()) {
            File outputDir = new File(compilerConfig.getOutputLocation());

            messages.addAll(getCompiler().performCompile(compilerConfig).getCompilerMessages());

            if (outputDir.isDirectory()) {
                files.addAll(normalizePaths(FileUtils.getFileNames(outputDir, null, null, false)));
            }
        }

        int numCompilerErrors = compilerErrorCount(messages);

        int numCompilerWarnings = compilerWarningCount(messages);

        int expectedErrors = expectedErrors();

        if (expectedErrors != numCompilerErrors) {
            System.out.println(numCompilerErrors + " error(s) found:");
            List<String> errors = new ArrayList<>();
            for (CompilerMessage error : messages) {
                if (!error.isError()) {
                    continue;
                }

                System.out.println("----");
                System.out.println(error.getFile());
                System.out.println(error.getMessage());
                System.out.println("----");
                errors.add(error.getMessage());
            }

            assertEquals(
                    expectedErrors,
                    numCompilerErrors,
                    "Wrong number of compilation errors (" + numCompilerErrors + "/" + expectedErrors + ") : "
                            + displayLines(errors));
        }

        int expectedWarnings = expectedWarnings();
        if (expectedWarnings != numCompilerWarnings) {
            List<String> warnings = new ArrayList<>();
            System.out.println(numCompilerWarnings + " warning(s) found:");
            for (CompilerMessage warning : messages) {
                if (!isWarning(warning)) {
                    continue;
                }

                System.out.println("----");
                System.out.println(warning.getFile());
                System.out.println(warning.getMessage());
                System.out.println("----");
                warnings.add(warning.getMessage());
            }

            assertEquals(
                    expectedWarnings,
                    numCompilerWarnings,
                    "Wrong number (" + numCompilerWarnings + "/" + expectedWarnings + ") of compilation warnings: "
                            + displayLines(warnings));
        }

        List<String> expectedFiles = normalizePaths(expectedOutputFiles());
        assertEquals(
                expectedFiles.size(),
                files.size(),
                "Number of expected output files does not match: " + files + " vs " + expectedFiles);
        assertTrue(
                files.containsAll(expectedFiles),
                "Output files do not contain all expected files: expected=" + expectedFiles + " actual=" + files);
    }

    protected String displayLines(List<String> warnings) {
        // with java8 could be as simple as String.join(System.lineSeparator(), warnings)
        StringBuilder sb = new StringBuilder(System.lineSeparator());
        for (String warning : warnings) {
            sb.append('-').append(warning).append(System.lineSeparator());
        }
        return sb.toString();
    }

    private List<CompilerConfiguration> getCompilerConfigurations() throws Exception {
        String sourceDir = "src/test-input/src/main";

        List<String> filenames = FileUtils.getFileNames(new File(sourceDir), "**/*.java", null, false, true);
        Collections.sort(filenames);

        List<CompilerConfiguration> compilerConfigurations = new ArrayList<>();

        int index = 0;
        for (Iterator<String> it = filenames.iterator(); it.hasNext(); index++) {
            String filename = it.next();

            CompilerConfiguration compilerConfig = new CompilerConfiguration();

            compilerConfig.setDebug(compilerDebug);

            compilerConfig.setShowDeprecation(compilerDeprecationWarnings);

            compilerConfig.setClasspathEntries(getClasspath());

            compilerConfig.addSourceLocation(sourceDir);

            compilerConfig.setOutputLocation("target/" + getRoleHint() + "/classes-" + index);

            FileUtils.deleteDirectory(compilerConfig.getOutputLocation());

            compilerConfig.addInclude(filename);

            compilerConfig.setForceJavacCompilerUse(this.forceJavacCompilerUse);

            configureCompilerConfig(compilerConfig);

            String target = getTargetVersion();
            if (StringUtils.isNotEmpty(target)) {
                compilerConfig.setTargetVersion(target);
            }

            String source = getSourceVersion();
            if (StringUtils.isNotEmpty(source)) {
                compilerConfig.setSourceVersion(source);
            }

            compilerConfigurations.add(compilerConfig);
        }

        return compilerConfigurations;
    }

    public String getTargetVersion() {
        return null;
    }

    public String getSourceVersion() {
        return null;
    }

    private List<String> normalizePaths(Collection<String> relativePaths) {
        return relativePaths.stream()
                .map(s -> s.replace(File.separatorChar, '/'))
                .collect(Collectors.toList());
    }

    protected int compilerErrorCount(List<CompilerMessage> messages) {
        int count = 0;

        for (CompilerMessage message : messages) {
            count += message.isError() ? 1 : 0;
        }

        return count;
    }

    protected int compilerWarningCount(List<CompilerMessage> messages) {
        int count = 0;

        for (CompilerMessage message : messages) {
            count += isWarning(message) ? 1 : 0;
        }

        return count;
    }

    private static boolean isWarning(CompilerMessage message) {
        return message.getKind() == CompilerMessage.Kind.WARNING
                || message.getKind() == CompilerMessage.Kind.MANDATORY_WARNING;
    }

    protected int expectedErrors() {
        return 1;
    }

    protected int expectedWarnings() {
        return 0;
    }

    protected Collection<String> expectedOutputFiles() {
        return Collections.emptyList();
    }

    protected String getJavaVersion() {
        String javaVersion = System.getProperty("java.version");
        String realJavaVersion = javaVersion;

        int dotIdx = javaVersion.indexOf(".");
        if (dotIdx > -1) {
            int lastDot = dotIdx;

            // find the next dot, so we can trim up to this point.
            dotIdx = javaVersion.indexOf(".", lastDot + 1);
            if (dotIdx > lastDot) {
                javaVersion = javaVersion.substring(0, dotIdx);
            }
        }

        System.out.println("java.version is: " + realJavaVersion + "\ntrimmed java version is: " + javaVersion
                + "\ncomparison: \"1.5\".compareTo( \"" + javaVersion + "\" ) == " + ("1.5".compareTo(javaVersion))
                + "\n");

        return javaVersion;
    }
}
