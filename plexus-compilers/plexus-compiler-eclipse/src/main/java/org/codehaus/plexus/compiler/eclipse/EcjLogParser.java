package org.codehaus.plexus.compiler.eclipse;

import java.io.File;
import java.util.List;

import org.codehaus.plexus.compiler.CompilerMessage;

/**
 * Log parser interface.
 * 
 * @author <a href="mailto:jfaust@tsunamit.com">Jason Faust</a>
 * @since 2.8.6
 */
public interface EcjLogParser {

    /**
     * Pares an Eclipse Compiler log file.
     * 
     * @param logFile          file to parse.
     * @param errorsAsWarnings if errors should be down-graded to warnings.
     * @return the messages.
     * @throws Exception on parse errors.
     */
    List<CompilerMessage> parse(File logFile, boolean errorsAsWarnings) throws Exception;

}
