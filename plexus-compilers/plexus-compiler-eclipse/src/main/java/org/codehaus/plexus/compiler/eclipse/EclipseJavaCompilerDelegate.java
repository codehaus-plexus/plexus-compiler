package org.codehaus.plexus.compiler.eclipse;

import java.io.PrintWriter;
import java.util.List;

import org.eclipse.jdt.core.compiler.CompilationProgress;
import org.eclipse.jdt.core.compiler.batch.BatchCompiler;

/**
 * Wraps API calls involving Java 17 class files from ECJ. {@link EclipseJavaCompiler} delegates to this class.
 * <p>
 * <b>Note:</b> This class needs to be compiled with target 17, while all the other classes in this module can be
 * compiled with target 11, as long as they do not directly import this class but use {@link Class#forName(String)} and
 * method handles to invoke any methods from here.
 */
public class EclipseJavaCompilerDelegate {
    static ClassLoader getClassLoader() {
        return BatchCompiler.class.getClassLoader();
    }

    static boolean batchCompile(List<String> args, PrintWriter devNull) {
        return BatchCompiler.compile(
                args.toArray(new String[0]), devNull, devNull, new BatchCompilerCompilationProgress());
    }

    private static class BatchCompilerCompilationProgress extends CompilationProgress {
        @Override
        public void begin(int i) {}

        @Override
        public void done() {}

        @Override
        public boolean isCanceled() {
            return false;
        }

        @Override
        public void setTaskName(String s) {}

        @Override
        public void worked(int i, int i1) {}
    }
}
