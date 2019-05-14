package org.codehaus.plexus.compiler.eclipse;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.plexus.compiler.CompilerMessage;
import org.codehaus.plexus.compiler.CompilerMessage.Kind;

/**
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * @author <a href="mailto:jfaust@tsunamit.com">Jason Faust</a>
 * Created on 31-3-18.
 */
public class EcjResponseParser implements EcjLogParser {
    /*--------------------------------------------------------------*/
    /*	CODING:	Decode ECJ -log format results.						*/
    /*--------------------------------------------------------------*/

    private static final XMLInputFactory FACTORY = getStreamFactory();

    private static XMLInputFactory getStreamFactory() {
        XMLInputFactory xmlif = XMLInputFactory.newInstance();
        xmlif.setProperty(XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
        xmlif.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
        xmlif.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
        return xmlif;
    }

    /**
     * @param xmltf            the xml file
     * @param errorsAsWarnings should we treat errors as warnings
     *                         Scan the specified response file for compilation messages.
     */
    @Override
    public List<CompilerMessage> parse(File xmltf, boolean errorsAsWarnings) throws Exception {
        // if(xmltf.length() < 80)
        //	return;

        List<CompilerMessage> list = new ArrayList<>();

        try (Reader src = new BufferedReader(new InputStreamReader(new FileInputStream(xmltf), "utf-8"))) {
            XMLStreamReader xsr = FACTORY.createXMLStreamReader(src);

            // scan for "source" elements, skip all else.
            while (xsr.hasNext()) {
                int type = xsr.next();
                if (type == XMLStreamConstants.START_ELEMENT && "source".equals(xsr.getLocalName())) {
                    decodeSourceElement(list, xsr, errorsAsWarnings);
                }
            }
        }
        return list;
    }

    private void decodeSourceElement(List<CompilerMessage> list, XMLStreamReader xsr, boolean errorsAsWarnings)
            throws Exception {
        String filename = xsr.getAttributeValue(null, "path");

        // -- Got a file- call handler
        File path = new File(filename).getCanonicalFile();
        while (xsr.hasNext()) {
            int type = xsr.nextTag();
            if (type == XMLStreamConstants.START_ELEMENT) {
                if ("problems".equals(xsr.getLocalName())) {
                    decodeProblems(list, path.toString(), xsr, errorsAsWarnings);
                } else {
                    ignoreTillEnd(xsr);
                }
            } else if (type == XMLStreamConstants.END_ELEMENT) {
                return;
            }
        }
    }

    /**
     * Locate "problem" nodes.
     */
    private void decodeProblems(
            List<CompilerMessage> list, String sourcePath, XMLStreamReader xsr, boolean errorsAsWarnings)
            throws Exception {
        while (xsr.hasNext()) {
            int type = xsr.nextTag();
            if (type == XMLStreamConstants.START_ELEMENT) {
                if ("problem".equals(xsr.getLocalName())) {
                    decodeProblem(list, sourcePath, xsr, errorsAsWarnings);
                } else {
                    ignoreTillEnd(xsr);
                }

            } else if (type == XMLStreamConstants.END_ELEMENT) {
                return;
            }
        }
    }

    private void decodeProblem(
            List<CompilerMessage> list, String sourcePath, XMLStreamReader xsr, boolean errorsAsWarnings)
            throws Exception {
        String id = xsr.getAttributeValue(null, "optionKey"); // Key for the problem
        int startline = getInt(xsr, "line");
        int column = 0;
        int endCol = 0;
        String sev = xsr.getAttributeValue(null, "severity");
        String message = "Unknown message?";

        // -- Go watch for "message" and "source_context"
        while (xsr.hasNext()) {
            int type = xsr.nextTag();
            if (type == XMLStreamConstants.START_ELEMENT) {
                if ("message".equals(xsr.getLocalName())) {
                    message = xsr.getAttributeValue(null, "value");
                }
                if ("source_context".equals(xsr.getLocalName())) {
                    column = getInt(xsr, "sourceStart");
                    if (column != -1) {
                        column += 1; // Offset to 1-based index
                    } else {
                        column = 0;
                    }
                    endCol = getInt(xsr, "sourceEnd");
                    if (endCol != -1) {
                        endCol += 1; // Offset to 1-based index
                    } else {
                        endCol = 0;
                    }
                }
                ignoreTillEnd(xsr);
            } else if (type == XMLStreamConstants.END_ELEMENT) {
                break;
            }
        }

        Kind msgtype;
        if ("warning".equalsIgnoreCase(sev)) {
            msgtype = Kind.WARNING;
        } else if ("error".equalsIgnoreCase(sev)) {
            msgtype = errorsAsWarnings ? Kind.WARNING : Kind.ERROR;
        } else if ("info".equalsIgnoreCase(sev)) {
            msgtype = Kind.NOTE;
        } else {
            msgtype = Kind.OTHER;
        }

        CompilerMessage cm = new CompilerMessage(sourcePath, msgtype, startline, column, startline, endCol, message);
        list.add(cm);
    }

    private static void ignoreTillEnd(XMLStreamReader xsr) throws Exception {
        int depth = 1;
        while (xsr.hasNext()) {
            int type = xsr.next();
            if (type == XMLStreamConstants.START_ELEMENT) {
                depth++;
            } else if (type == XMLStreamConstants.END_ELEMENT) {
                depth--;
                if (depth == 0) {
                    return;
                }
            }
        }
    }

    private static int getInt(XMLStreamReader xsr, String name) throws IOException {
        String v = xsr.getAttributeValue(null, name);
        if (null == v) {
            return -1;
        }
        try {
            return Integer.parseInt(v.trim());
        } catch (Exception x) {
            throw new IOException("Illegal integer value '" + v + "' in attribute " + name);
        }
    }
}
