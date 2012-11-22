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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.ua.tm.neji.nlp;

import com.aliasi.util.Pair;
import monq.jfa.*;
import org.slf4j.LoggerFactory;
import pt.ua.tm.gimli.corpus.Corpus;
import pt.ua.tm.gimli.corpus.Sentence;
import pt.ua.tm.gimli.exception.GimliException;
import pt.ua.tm.gimli.external.gdep.GDepCorpus;
import pt.ua.tm.gimli.external.gdep.GDepSentence;
import pt.ua.tm.gimli.external.wrapper.Parser;
import pt.ua.tm.neji.core.module.BaseLoader;
import pt.ua.tm.neji.exception.NejiException;

import java.util.List;
import java.util.logging.Logger;

/**
 * Module to perform Natural Language Processing, namely tokenization, lemmatization, POS tagging, chunking and
 * dependency parsing.
 *
 * @author David Campos (<a href="mailto:david.campos@ua.pt">david.campos@ua.pt</a>)
 * @version 1.0
 * @since 1.0
 */
public class NLP extends BaseLoader {

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

    public NLP(Corpus corpus, Parser parser, List<Pair> sentencesPositions) throws NejiException {
        super(corpus);
        this.parser = parser;
        this.sentencesPositions = sentencesPositions;
        this.setSentencePositions = true;

        try {
            Nfa nfa = new Nfa(Nfa.NOTHING);
            nfa.or(Xml.STag("s"), start_sentence);
            nfa.or(Xml.ETag("s"), end_sentence);
            setNFA(nfa, DfaRun.UNMATCHED_COPY);
        } catch (ReSyntaxException ex) {
            throw new NejiException(ex);
        }

        this.sentenceCounter = 0;
        this.inSentence = false;
        this.startSentence = 0;
    }

    public NLP(Corpus corpus, Parser parser) throws NejiException {
        super(corpus);
        this.parser = parser;
        this.setSentencePositions = false;

        try {
            Nfa nfa = new Nfa(Nfa.NOTHING);
            nfa.or(Xml.STag("s"), start_sentence);
            nfa.or(Xml.ETag("s"), end_sentence);
            setNFA(nfa, DfaRun.UNMATCHED_COPY);
        } catch (ReSyntaxException ex) {
            throw new NejiException(ex);
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

                Sentence s = new Sentence(getCorpus(), gs, sentence);

                if (setSentencePositions) {
                    //logger.info("{}", sentencesPositions);
                    Pair p = sentencesPositions.get(sentenceCounter);
                    s.setStartSource((Integer) p.a());
                    s.setEndSource((Integer) p.b());
                }

                getCorpus().addSentence(s);
            } catch (GimliException ex) {
                throw new RuntimeException("There was a problem parsing the sentence.", ex);
            }

            inSentence = false;
            sentenceCounter++;
            runner.collect = false;

        }
    };
}
