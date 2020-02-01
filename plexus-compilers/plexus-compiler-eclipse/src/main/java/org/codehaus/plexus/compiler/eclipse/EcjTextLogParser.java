package org.codehaus.plexus.compiler.eclipse;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codehaus.plexus.compiler.CompilerMessage;
import org.codehaus.plexus.compiler.CompilerMessage.Kind;

/**
 * Parser for non-XML Eclipse Compiler output.
 * 
 * @author <a href="mailto:jfaust@tsunamit.com">Jason Faust</a>
 * @since 2.8.6
 */
public class EcjTextLogParser implements EcjLogParser {

    private static final String TEN_DASH = "----------";
    private static final String PATTERN_LEVEL_FILE = "^\\d+\\.\\s+(\\w+)\\s+in\\s+(.*)\\s+\\(at line (\\d+)\\)$";

    private Pattern levelFile = Pattern.compile(PATTERN_LEVEL_FILE);

    @Override
    public List<CompilerMessage> parse(File logFile, boolean errorsAsWarnings) throws Exception {
        List<CompilerMessage> ret = new ArrayList<>();
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(new FileInputStream(logFile), StandardCharsets.UTF_8))) {
            String line;
            List<String> buffer = new LinkedList<>();
            boolean header = true;

            while ((line = in.readLine()) != null) {
                if (header) {
                    if (!line.startsWith(TEN_DASH)) {
                        continue;
                    }
                    header = false;
                }
                if (line.startsWith(TEN_DASH)) {
                    if (!buffer.isEmpty()) {
                        ret.add(parse(buffer, errorsAsWarnings));
                    }
                    buffer.clear();
                } else {
                    buffer.add(line);
                }
            }
        }
        return ret;
    }

    private CompilerMessage parse(List<String> buffer, boolean errorsAsWarnings) {

        Kind kind = null;
        String file = null;
        int lineNum = 0;
        int startCol = 0;
        int endCol = 0;
        String message = null;

        // First line, kind, file, lineNum
        if (buffer.size() > 1) {
            String str = buffer.get(0);
            Matcher matcher = levelFile.matcher(str);
            if (matcher.find()) {
                file = matcher.group(2);
                kind = decodeKind(matcher.group(1), errorsAsWarnings);
                lineNum = Integer.parseInt(matcher.group(3));
            }
        }

        // Last line, message
        if (buffer.size() >= 2) {
            String str = buffer.get(buffer.size() - 1);
            message = str.trim();
        }

        // 2nd to last line, columns
        if (buffer.size() >= 3) {
            String str = buffer.get(buffer.size() - 2);
            startCol = str.indexOf('^');
            endCol = str.lastIndexOf('^');
        }

        return new CompilerMessage(file, kind, lineNum, startCol, lineNum, endCol, message);
    }

    private Kind decodeKind(String str, boolean errorsAsWarnings) {
        if (str.equals("ERROR")) {
            return errorsAsWarnings ? Kind.WARNING : Kind.ERROR;
        }
        if (str.equals("INFO")) {
            return Kind.NOTE;
        }
        return Kind.WARNING;
    }

}
