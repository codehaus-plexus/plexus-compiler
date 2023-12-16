/*
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
 * @since 2.14.0
 */
public class EcjTextLogParser implements EcjLogParser {

    private static final String TEN_DASH = "----------";
    private static final String PATTERN_LEVEL_FILE = "^\\d+\\.\\s+(\\w+)\\s+in\\s+(.*)\\s+\\(at line (\\d+)\\)$";

    private Pattern levelFile = Pattern.compile(PATTERN_LEVEL_FILE);

    @Override
    public List<CompilerMessage> parse(File logFile, boolean errorsAsWarnings) throws Exception {
        List<CompilerMessage> ret = new ArrayList<>();
        try (BufferedReader in =
                new BufferedReader(new InputStreamReader(new FileInputStream(logFile), StandardCharsets.UTF_8))) {
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
