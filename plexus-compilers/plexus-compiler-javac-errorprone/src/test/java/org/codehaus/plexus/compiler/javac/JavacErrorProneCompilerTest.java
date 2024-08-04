package org.codehaus.plexus.compiler.javac;

import org.codehaus.plexus.compiler.AbstractCompilerTest;

/**
 * @author <a href="mailto:jason@plexus.org">Jason van Zyl</a>
 */
public class JavacErrorProneCompilerTest extends AbstractCompilerTest {

    @Override
    protected String getRoleHint() {
        return "javac-with-errorprone";
    }

    @Override
    protected int expectedWarnings() {
        String javaVersion = getJavaVersion();
        if (javaVersion.startsWith("1.8")) {
            return 1;
        } else if (javaVersion.contains("18")
                || javaVersion.contains("19")
                || javaVersion.contains("20")
                || javaVersion.contains("23")) {
            return 5;
        } else if (javaVersion.contains("21") || javaVersion.contains("22")) {
            return 6;
        }
        return 2;
    }

    @Override
    protected int expectedErrors() {
        return 1;
    }

    @Override
    public String getSourceVersion() {
        return "1.8";
    }

    @Override
    public String getTargetVersion() {
        return "1.8";
    }
}
