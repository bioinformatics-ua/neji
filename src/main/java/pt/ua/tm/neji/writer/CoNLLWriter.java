/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.ua.tm.neji.writer;

import java.util.logging.Level;
import monq.jfa.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.tm.gimli.corpus.Corpus;
import pt.ua.tm.gimli.corpus.Sentence;
import pt.ua.tm.gimli.corpus.Token;
import pt.ua.tm.gimli.exception.GimliException;
import pt.ua.tm.neji.core.Tagger;

/**
 *
 * @author david
 */
public class CoNLLWriter extends Tagger {

    /**
     * {@link Logger} to be used in the class.
     */
    private static Logger logger = LoggerFactory.getLogger(CoNLLWriter.class);
    private Corpus corpus;
    private int sentenceCounter;
    private int startSentence;

    public CoNLLWriter(final Corpus corpus) throws GimliException {
        super();
        this.corpus = corpus;
        this.sentenceCounter = 0;
        this.startSentence = 0;

        try {
            Nfa nfa = new Nfa(Nfa.NOTHING);
            nfa.or(Xml.STag("s"), start_sentence);
            nfa.or(Xml.ETag("s"), end_sentence);
            this.dfa = nfa.compile(DfaRun.UNMATCHED_DROP, eof);


        } catch (ReSyntaxException ex) {
            throw new GimliException("There was a problem compiling the Dfa to process the document.", ex);
        } catch (CompileDfaException ex) {
            throw new GimliException("There was a problem compiling the Dfa to process the document.", ex);
        }
    }
    private AbstractFaAction eof = new AbstractFaAction() {
        @Override
        public void invoke(StringBuffer yytext, int start, DfaRun runner) {


//            Gson gson = new GsonBuilder().setPrettyPrinting().create();
//            String jsonText = gson.toJson(json);

            
            
            StringBuilder sb = new StringBuilder();

            for (Sentence s : corpus) {
                for (int i = 0; i < s.size(); i++) {
                    Token t = s.getToken(i);
                    sb.append(t.getText());
                    sb.append("\t");
                    
                    if (t.getFeature("LEMMA") != null) {
                        sb.append(t.getFeature("LEMMA"));
                        sb.append("\t");
                    }
                    
                    if (t.getFeature("POS") != null) {
                        sb.append(t.getFeature("POS"));
                        sb.append("\t");
                    }
                    
                    if (t.getFeature("CHUNK") != null) {
                        sb.append(t.getFeature("CHUNK"));
                        sb.append("\t");
                    }
                    
                    if (t.getFeature("DEP_TOK") != null) {
                        sb.append(t.getFeature("DEP_TOK"));
                        sb.append("\t");
                    }
                    
                    if (t.getFeature("DEP_TAG") != null) {
                        sb.append(t.getFeature("DEP_TAG"));
                        sb.append("\t");
                    }
                    
                    //sb.append(t.getLabel());
                    sb.append("\n");
                }
                sb.append("\n");
            }


            yytext.replace(0, yytext.length(), sb.toString());
            runner.collect = false;
        }
    };
    private AbstractFaAction start_sentence = new AbstractFaAction() {
        @Override
        public void invoke(StringBuffer yytext, int start, DfaRun runner) {
            runner.collect = true;
//            startSentence = yytext.indexOf(">", start) + 1;
            startSentence = start;
        }
    };
    private AbstractFaAction end_sentence = new AbstractFaAction() {
        @Override
        public void invoke(StringBuffer yytext, int start, DfaRun runner) {
//            yytext.replace(startSentence, start + 4, "");
        }
    };
}
