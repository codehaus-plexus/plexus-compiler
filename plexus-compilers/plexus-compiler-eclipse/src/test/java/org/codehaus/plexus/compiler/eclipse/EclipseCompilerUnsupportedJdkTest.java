package org.codehaus.plexus.compiler.eclipse;

import javax.inject.Inject;
import javax.inject.Named;

import java.io.File;
import java.util.Set;
import java.util.stream.Collectors;

import org.codehaus.plexus.compiler.Compiler;
import org.codehaus.plexus.compiler.CompilerConfiguration;
import org.codehaus.plexus.compiler.CompilerException;
import org.codehaus.plexus.testing.PlexusTest;
import org.codehaus.plexus.util.FileUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import static org.codehaus.plexus.testing.PlexusExtension.getBasedir;
import static org.junit.jupiter.api.Assertions.assertThrows;

@PlexusTest
@Isolated("changes static variable")
public class EclipseCompilerUnsupportedJdkTest {
    static final boolean IS_JDK_SUPPORTED = EclipseJavaCompiler.isJdkSupported;

    @Inject
    @Named("eclipse")
    Compiler compiler;

    @BeforeAll
    public static void setUpClass() {
        EclipseJavaCompiler.isJdkSupported = false;
    }

    @AfterAll
    public static void cleanUpClass() {
        EclipseJavaCompiler.isJdkSupported = IS_JDK_SUPPORTED;
    }

    @Test
    public void testUnsupportedJdk() {
        CompilerException error = assertThrows(CompilerException.class, () -> compiler.performCompile(getConfig()));
        MatcherAssert.assertThat(error.getMessage(), Matchers.containsString("ECJ needs JRE 17+"));
    }

    private CompilerConfiguration getConfig() throws Exception {
        String sourceDir = getBasedir() + "/src/test-input/src/main";
        Set<File> sourceFiles = FileUtils.getFileNames(new File(sourceDir), "**/*.java", null, false, true).stream()
                .map(File::new)
                .collect(Collectors.toSet());
        CompilerConfiguration compilerConfig = new CompilerConfiguration();
        compilerConfig.addSourceLocation(sourceDir);
        compilerConfig.setOutputLocation(getBasedir() + "/target/eclipse/classes");
        compilerConfig.setSourceFiles(sourceFiles);
        compilerConfig.setTargetVersion("1.8");
        compilerConfig.setSourceVersion("1.8");
        return compilerConfig;
    }
}
