/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.ua.tm.neji.ml;

import java.util.List;
import monq.jfa.*;
import pt.ua.tm.gimli.corpus.Corpus;
import pt.ua.tm.gimli.corpus.Sentence;
import pt.ua.tm.gimli.exception.GimliException;
import pt.ua.tm.gimli.model.CRFBase;
import pt.ua.tm.gimli.processing.Abbreviation;
import pt.ua.tm.gimli.processing.Parentheses;
import pt.ua.tm.neji.annotator.Annotator;
import pt.ua.tm.neji.core.Hybrid;
import pt.ua.tm.neji.dictionary.Dictionary;
import pt.ua.tm.neji.normalization.Normalization;
import uk.ac.man.entitytagger.matching.Matcher;

/**
 *
 * @author david
 */
public class MLHybrid extends Hybrid {

    private CRFBase crf;
    private List<Dictionary> dictionaries;
    private boolean doNormalization;
    private int startSentence;
    private int sentence;
    private String group;

    public MLHybrid(Corpus corpus, CRFBase crf, String group) throws GimliException {
        this(corpus, crf, group, null);
        this.doNormalization = false;
    }

    public MLHybrid(Corpus corpus, CRFBase crf, String group, List<Dictionary> dictionaries) throws GimliException {
        super(corpus);
        assert (crf != null);

        this.crf = crf;
        this.dictionaries = dictionaries;
        this.doNormalization = true;
        this.group = group.toUpperCase();

        try {
            Nfa nfa = new Nfa(Nfa.NOTHING);
            nfa.or(Xml.STag("s"), start_sentence);
            nfa.or(Xml.ETag("s"), end_sentence);
            this.dfa = nfa.compile(DfaRun.UNMATCHED_COPY);
        } catch (CompileDfaException ex) {
            throw new GimliException("There was a problem compiling the Dfa to process the document.", ex);
        } catch (ReSyntaxException ex) {
            throw new GimliException("There is a syntax problem with the document.", ex);
        }
        this.startSentence = 0;
        this.sentence = 0;
    }
    private AbstractFaAction start_sentence = new AbstractFaAction() {

        @Override
        public void invoke(StringBuffer yytext, int start, DfaRun runner) {
            runner.collect = true;
            startSentence = yytext.indexOf(">", start) + 1;
        }
    };
    private AbstractFaAction end_sentence = new AbstractFaAction() {

        @Override
        public void invoke(StringBuffer yytext, int start, DfaRun runner) {

            try {
                Sentence s = corpus.getSentence(sentence);

                // Annotate sentence
                Annotator.annotate(s, crf);

                // Post-processing
                Parentheses.processRemoving(s);
                Abbreviation.process(s);

                // Normalization
                if (doNormalization) {
                    Normalization norm = new Normalization(dictionaries, group);
                    norm.normalize(s);
                } else {
                    // Move annotations to bucket
                    s.moveAnnotationsToTree(group);
                }

            } catch (GimliException ex) {
                throw new RuntimeException("There was a problem annotating the sentence.", ex);
            }

            runner.collect = false;
            sentence++;
        }
    };
}
