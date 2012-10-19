/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.ua.tm.neji.nlp;

import com.aliasi.util.Pair;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import monq.jfa.AbstractFaAction;
import monq.jfa.CompileDfaException;
import monq.jfa.DfaRun;
import monq.jfa.Nfa;
import monq.jfa.ReSyntaxException;
import monq.jfa.Xml;
import org.slf4j.LoggerFactory;
import pt.ua.tm.gimli.corpus.Corpus;
import pt.ua.tm.gimli.corpus.Sentence;
import pt.ua.tm.gimli.exception.GimliException;
import pt.ua.tm.gimli.external.gdep.GDepCorpus;
import pt.ua.tm.gimli.external.gdep.GDepSentence;
import pt.ua.tm.gimli.external.wrapper.Parser;
import pt.ua.tm.neji.core.Loader;

/**
 *
 * @author david
 */
public class NLP extends Loader {

    /**
     * {@link Logger} to be used in the class.
     */
    private static org.slf4j.Logger logger = LoggerFactory.getLogger(NLP.class);
    private int sentenceCounter;
    private boolean inSentence;
    private int startSentence;
    private Parser parser;
    private GDepCorpus gdep = new GDepCorpus();
    private List<Pair> sentencesPositions;
    private boolean setSentencePositions;

    public NLP(Corpus corpus, Parser parser, List<Pair> sentencesPositions) throws GimliException {
        super(corpus);
        this.parser = parser;
        this.sentencesPositions = sentencesPositions;
        this.setSentencePositions = true;

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

        this.sentenceCounter = 0;
        this.inSentence = false;
        this.startSentence = 0;
    }

    public NLP(Corpus corpus, Parser parser) throws GimliException {
        super(corpus);
        this.parser = parser;
        this.setSentencePositions = false;

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

        this.sentenceCounter = 0;
        this.inSentence = false;
        this.startSentence = 0;
    }
    private AbstractFaAction start_sentence = new AbstractFaAction() {
        @Override
        public void invoke(StringBuffer yytext, int start, DfaRun runner) {
            inSentence = true;
            startSentence = yytext.indexOf(">", start) + 1;
            runner.collect = true;
        }
    };
    private AbstractFaAction end_sentence = new AbstractFaAction() {
        @Override
        public void invoke(StringBuffer yytext, int start, DfaRun runner) {
            String sentence = yytext.substring(startSentence, start);

            try {
                GDepSentence gs = new GDepSentence(gdep, parser.parse(sentence));

                Sentence s = new Sentence(corpus, gs, sentence);

                if (setSentencePositions) {
                    //logger.info("{}", sentencesPositions);
                    Pair p = sentencesPositions.get(sentenceCounter);
                    s.setStartSource((Integer) p.a());
                    s.setEndSource((Integer) p.b());
                }

                corpus.addSentence(s);
            } catch (GimliException ex) {
                throw new RuntimeException("There was a problem parsing the sentence.", ex);
            }

            inSentence = false;
            sentenceCounter++;
            runner.collect = false;

        }
    };
}
