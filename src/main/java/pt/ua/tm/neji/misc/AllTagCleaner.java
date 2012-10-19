package pt.ua.tm.neji.misc;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


import pt.ua.tm.neji.core.Tagger;
import monq.jfa.*;
import pt.ua.tm.gimli.exception.GimliException;

/**
 *
 * @author david
 */
public class AllTagCleaner extends Tagger {

    public AllTagCleaner() throws GimliException {
        super();
        // Initialise XML parser
        try {
            Nfa nfa = new Nfa(Nfa.NOTHING);
            nfa.or(".+", text);
            this.dfa = nfa.compile(DfaRun.UNMATCHED_COPY);
        }
        catch (ReSyntaxException ex) {
            throw new GimliException("There was a problem initializing the XML parser.", ex);
        }
        catch (CompileDfaException ex) {
            throw new GimliException("There was a problem compiling the Dfa to process the document.", ex);
        }
    }
    private AbstractFaAction text = new AbstractFaAction() {

        @Override
        public void invoke(StringBuffer yytext, int start, DfaRun runner) {
            int size = yytext.length();
            String text = yytext.toString();
            text = text.replaceAll("\\<.*?>", "");
            yytext.replace(start, size, text);
        }
    };
}
