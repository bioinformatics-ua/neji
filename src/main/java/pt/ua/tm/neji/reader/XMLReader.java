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
public class XMLReader extends Tagger {
    
    private static Logger logger = LoggerFactory.getLogger(XMLReader.class);
    private int startText;
    private boolean inText;
    
    public XMLReader(String[] xmlTags) throws GimliException {

        // Initialise XML parser
        try {
            Nfa nfa = new Nfa(Nfa.NOTHING);
            for (int i = 0; i < xmlTags.length; i++) {
                nfa.or(Xml.STag(xmlTags[i]), start_text);
                nfa.or(Xml.ETag(xmlTags[i]), end_text);
            }
            this.dfa = nfa.compile(DfaRun.UNMATCHED_COPY);
        } catch (CompileDfaException ex) {
            throw new GimliException("There was a problem compiling the Dfa to process the document.", ex);
        } catch (ReSyntaxException ex) {
            throw new GimliException("There is a syntax problem with the document.", ex);
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
