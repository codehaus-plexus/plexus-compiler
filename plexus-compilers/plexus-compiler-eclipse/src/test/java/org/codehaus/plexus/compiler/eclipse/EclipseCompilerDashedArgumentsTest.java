package org.codehaus.plexus.compiler.eclipse;

import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.compiler.Compiler;
import org.codehaus.plexus.compiler.CompilerConfiguration;
import org.codehaus.plexus.util.FileUtils;
import org.junit.Assert;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on 22-4-18.
 */
public class EclipseCompilerDashedArgumentsTest extends PlexusTestCase {

    public static final String BAD_DOUBLEDASH_OPTION = "--grubbelparkplace";

    private CompilerConfiguration getConfig() throws Exception {
        String sourceDir = getBasedir() + "/src/test-input/src/main";

        List<String> filenames = FileUtils.getFileNames( new File( sourceDir ), "**/*.java", null, false, true );
        Collections.sort( filenames );
        Set<File> files = new HashSet<>();
        for(String filename : filenames)
        {
            files.add(new File(filename));
        }

        CompilerConfiguration compilerConfig = new CompilerConfiguration();
        compilerConfig.setDebug(false);
        compilerConfig.setShowDeprecation(false);

//            compilerConfig.setClasspathEntries( getClasspath() );
        compilerConfig.addSourceLocation( sourceDir );
        compilerConfig.setOutputLocation( getBasedir() + "/target/eclipse/classes");
        FileUtils.deleteDirectory( compilerConfig.getOutputLocation() );
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
    public void testDoubleDashOptionsArePassedWithTwoDashes() throws Exception
    {
        Compiler compiler = (Compiler) lookup( Compiler.ROLE, "eclipse" );
        CompilerConfiguration config = getConfig();
        config.addCompilerCustomArgument(BAD_DOUBLEDASH_OPTION, "b0rk3d");

        try
        {
            compiler.performCompile(config);
            Assert.fail("Expected an exception to be thrown");
        } catch(EcjFailureException x) {
            String ecjOutput = x.getEcjOutput();
            Assert.assertTrue("The output should report the failure with two dashes: " + ecjOutput
                    , ecjOutput.contains(BAD_DOUBLEDASH_OPTION) && ! ecjOutput.contains("-" + BAD_DOUBLEDASH_OPTION)
            );
        }
    }
}
