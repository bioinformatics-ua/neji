/*
 * Copyright (c) 2012 David Campos, University of Aveiro.
 *
 * Neji is a framework for modular biomedical concept recognition made easy, fast and accessible.
 *
 * This project is licensed under the Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported License.
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-sa/3.0/.
 *
 * This project is a free software, you are free to copy, distribute, change and transmit it. However, you may not use
 * it for commercial purposes.
 *
 * It is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package pt.ua.tm.neji.reader;


import monq.jfa.*;
import org.apache.commons.lang.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.tm.neji.core.module.BaseReader;
import pt.ua.tm.neji.exception.NejiException;
import pt.ua.tm.neji.util.XMLParsing;

/**
 * Module to tag regions of interest from a XML document.
 * @author David Campos (<a href="mailto:david.campos@ua.pt">david.campos@ua.pt</a>)
 * @version 1.0
 * @since 1.0
 */
public class XMLReader extends BaseReader {

    private static Logger logger = LoggerFactory.getLogger(XMLReader.class);
    private int startText;
    private boolean inText;

    public XMLReader(String[] xmlTags) throws NejiException {

        // Initialise XML parser
        try {
            Nfa nfa = new Nfa(Nfa.NOTHING);
            for (int i = 0; i < xmlTags.length; i++) {
                nfa.or(Xml.STag(xmlTags[i]), start_text);
                nfa.or(Xml.ETag(xmlTags[i]), end_text);
            }
            setNFA(nfa, DfaRun.UNMATCHED_COPY);
        } catch (ReSyntaxException ex) {
            throw new NejiException(ex);
        }
        startText = 0;
        inText = false;
    }


    private AbstractFaAction start_text = new AbstractFaAction() {
        @Override
        public void invoke(StringBuffer yytext, int start, DfaRun runner) {
            inText = true;
            runner.collect = true;
            startText = yytext.indexOf(">", start) + 1;
        }
    };
    private AbstractFaAction end_text = new AbstractFaAction() {
        @Override
        public void invoke(StringBuffer yytext, int start, DfaRun runner) {
            if (inText) {
                StringBuilder sb = new StringBuilder();
                sb.append("<roi>");

                // Solve escaping problems from MEDLINE
                String textWithoutProblems = XMLParsing.solveXMLEscapingProblems(yytext.substring(startText, start));

                // Unescape XML Tags
                String unescapedText = StringEscapeUtils.unescapeXml(textWithoutProblems);

                // New lines, are new ROIs
                unescapedText = unescapedText.replaceAll("\n", "</roi>\n<roi>");

                sb.append(unescapedText);
                sb.append("</roi>");

                yytext.replace(startText, start, sb.toString());
                inText = false;
                runner.collect = false;
            }
        }
    };
}
