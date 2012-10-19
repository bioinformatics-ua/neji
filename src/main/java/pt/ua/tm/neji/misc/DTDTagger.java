package pt.ua.tm.neji.misc;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


import pt.ua.tm.neji.core.Tagger;
import java.util.logging.Logger;
import monq.jfa.*;
import org.slf4j.LoggerFactory;
import pt.ua.tm.gimli.exception.GimliException;

/**
 *
 * @author david
 */
public class DTDTagger extends Tagger {

    /**
     * {@link Logger} to be used in the class.
     */
    private static org.slf4j.Logger logger = LoggerFactory.getLogger(DTDTagger.class);
    private boolean inDocType;
    private int startDocType;
    private static String neji = "<!DOCTYPE PubmedArticleSet PUBLIC \"-//NLM//DTD PubMedArticle, 1st January 2011//EN\" \"http://bioinformatics.ua.pt/support/gimli/pubmed/gimli.dtd\">";

    public DTDTagger() throws GimliException {
        super();

        try {
            Nfa nfa = new Nfa(Nfa.NOTHING);
            nfa.or("<\\!DOCTYPE PubmedArticleSet", start_doctype);
            nfa.or(">", end_doctype);
            this.dfa = nfa.compile(DfaRun.UNMATCHED_COPY);
        }
        catch (CompileDfaException ex) {
            throw new GimliException("There was a problem compiling the Dfa to process the document.", ex);
        }
        catch (ReSyntaxException ex) {
            throw new GimliException("There is a syntax problem with the document.", ex);
        }
        inDocType = false;
        startDocType = 0;
    }
    private AbstractFaAction start_doctype = new AbstractFaAction() {

        @Override
        public void invoke(StringBuffer yytext, int start, DfaRun runner) {
            inDocType = true;
            startDocType = start;
            runner.collect = true;
        }
    };
    private AbstractFaAction end_doctype = new AbstractFaAction() {

        @Override
        public void invoke(StringBuffer yytext, int start, DfaRun runner) {
            if (inDocType) {
                int end = yytext.indexOf(">", start) + 1;

                yytext.replace(startDocType, end, neji);

                inDocType = false;
                runner.collect = false;
            }

        }
    };
}
