package pt.ua.tm.neji.reader;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
import monq.jfa.*;
import org.apache.commons.lang.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.tm.gimli.exception.GimliException;
import pt.ua.tm.neji.core.Tagger;
import pt.ua.tm.neji.global.XMLParsing;

/**
 *
 * @author david
 */
public class RawReader extends Tagger {

    private static Logger logger = LoggerFactory.getLogger(RawReader.class);

    public RawReader() throws GimliException {
        // Initialise parser
        try {
            Nfa nfa = new Nfa(".+", rawText);
            this.dfa = nfa.compile(DfaRun.UNMATCHED_COPY);
        } catch (ReSyntaxException ex) {
            throw new GimliException("There was a problem initializing the XML parser.", ex);
        } catch (CompileDfaException ex) {
            throw new GimliException("There was a problem compiling the Dfa to process the document.", ex);
        }
    }
    private AbstractFaAction rawText = new AbstractFaAction() {
        @Override
        public void invoke(StringBuffer yytext, int start, DfaRun runner) {

            StringBuilder sb = new StringBuilder();
            sb.append("<roi>");
            
            // Solve escaping problems from MEDLINE
            String textWithoutProblems = XMLParsing.solveXMLEscapingProblems(yytext.toString());

            // Unescape XML Tags
            String unescapedText = StringEscapeUtils.unescapeXml(textWithoutProblems);
            
            // New lines, are new ROIs
            unescapedText = unescapedText.replaceAll("\n", "</roi>\n<roi>");
            
            sb.append(unescapedText);
            sb.append("</roi>");

            yytext.replace(start, yytext.length(), sb.toString());
        }
    };
}
