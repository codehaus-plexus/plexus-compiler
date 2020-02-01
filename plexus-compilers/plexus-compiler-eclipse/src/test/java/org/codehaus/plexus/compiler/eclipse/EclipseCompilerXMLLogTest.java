package org.codehaus.plexus.compiler.eclipse;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.plexus.compiler.CompilerConfiguration;

/**
 * Test of a XML user log specified.
 * 
 * @author <a href="jfaust@tsunamit.com">Jason Faust</a>
 * @since 2.8.6
 */
public class EclipseCompilerXMLLogTest extends AbstractEclipseCompilerTest {

    private List<String> logFiles = new ArrayList<>();
    private int idx = 0;

    @Override
    protected void configureCompilerConfig(CompilerConfiguration compilerConfig, String filename) {
        super.configureCompilerConfig(compilerConfig, filename);
        String logFile = getTestPath("/target/" + getRoleHint() + "/log-xml-" + idx + ".xml");
        compilerConfig.addCompilerCustomArgument("-log", logFile);
        idx++;
    }

    @Override
    public void testCompilingSources() throws Exception {
        super.testCompilingSources();
        for (String file : logFiles) {
            assertTrue(new File(file).exists());
        }
    }

}
