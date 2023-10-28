package org.codehaus.plexus.compiler.j2objc;

import org.codehaus.plexus.compiler.CompilerMessage;
import org.codehaus.plexus.compiler.CompilerMessage.Kind;

/**
 * Handle the output of J2ObjC
 *
 * @author lmaitre
 */
public class DefaultJ2ObjCCompilerParser {

    private static String ERROR_PREFIX = "error: ";

    private static String CONVERT_PREFIX = "translating ";

    private static String TRANSLATION_PREFIX = "Translated ";

    /**
     * Parse a line of log, reading the error and translating lines.
     *
     * @param line
     * @return The compiler message for this line or null if there is no need of
     * a message.
     */
    public static CompilerMessage parseLine(String line) {
        String file = null;
        boolean error = false;
        int startline = -1;
        int startcolumn = -1;
        int endline = -1;
        int endcolumn = -1;
        String message;

        if (line.startsWith(ERROR_PREFIX)) {
            message = line.substring(ERROR_PREFIX.length());
            error = true;
        } else if (line.startsWith(CONVERT_PREFIX)) {
            message = line;
        } else if (line.startsWith(TRANSLATION_PREFIX)) {
            message = line;
        } else {
            System.err.println("Unknown output: " + line);

            return null;
        }

        return new CompilerMessage(
                file, error ? Kind.ERROR : Kind.NOTE, startline, startcolumn, endline, endcolumn, message);
    }
}
