/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.ua.tm.neji.global;

import org.apache.commons.lang.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author david
 */
public class XMLParsing {

    private static Logger logger = LoggerFactory.getLogger(XMLParsing.class);

    public static String solveXMLEscapingProblems(String text) {
        // Remove no breaking white spaces
        text = text.replaceAll("\\u00A0", " ");

        // Solve MEDLINE bug that puts an XML tag with various lines
//        text = text.replaceAll("\n\\s+", " ");
//        text = text.replaceAll("\n", " ");

        // Solve MEDLINE bug of HTML codes escaped twice
        text = StringEscapeUtils.unescapeXml(text);

        // Solve MEDLINE bug that escapes HTML4 hex codes
        text = text.replaceAll("&amp;#", "&#");

        return text;
    }
}
