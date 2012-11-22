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
package pt.ua.tm.neji.ml;

import monq.jfa.*;
import pt.ua.tm.gimli.corpus.Corpus;
import pt.ua.tm.gimli.corpus.Sentence;
import pt.ua.tm.gimli.exception.GimliException;
import pt.ua.tm.gimli.model.CRFBase;
import pt.ua.tm.gimli.processing.Abbreviation;
import pt.ua.tm.gimli.processing.Parentheses;
import pt.ua.tm.neji.annotator.Annotator;
import pt.ua.tm.neji.core.module.BaseHybrid;
import pt.ua.tm.neji.dictionary.Dictionary;
import pt.ua.tm.neji.exception.NejiException;
import pt.ua.tm.neji.normalization.Normalization;

import java.util.List;

/**
 * Module to perform concept recognition using Machine Learning.
 * @author David Campos (<a href="mailto:david.campos@ua.pt">david.campos@ua.pt</a>)
 * @version 1.0
 * @since 1.0
 */
public class MLHybrid extends BaseHybrid {

    private CRFBase crf;
    private List<Dictionary> dictionaries;
    private boolean doNormalization;
    private int startSentence;
    private int sentence;
    private String group;

    public MLHybrid(Corpus corpus, CRFBase crf, String group) throws NejiException {
        this(corpus, crf, group, null);
        this.doNormalization = false;
    }

    public MLHybrid(Corpus corpus, MLModel model, CRFBase crf) throws NejiException {
        this(corpus, crf, model.getSemanticGroup(), model.getNormalizationDictionaries());
    }

    public MLHybrid(Corpus corpus, CRFBase crf, String group, List<Dictionary> dictionaries) throws NejiException {
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
            setNFA(nfa, DfaRun.UNMATCHED_COPY);
        } catch (ReSyntaxException ex) {
            throw new NejiException(ex);
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

                // Get start and end of sentence
                int startSentence = yytext.indexOf("<s id=");
                int endSentence = yytext.lastIndexOf("</s>") + 4;

                int realStart = yytext.indexOf(">", startSentence) + 1;
                int realEnd = endSentence - 4;

                // Get sentence with XML tags
                String sentenceText = yytext.substring(realStart, realEnd);

                Sentence s = getCorpus().getSentence(sentence);

                // Annotate sentence
                Annotator.annotate(s, crf);

                // Post-processing
                Parentheses.processRemoving(s);
                Abbreviation.process(s);

                // Normalization
                if (doNormalization) {
                    Normalization norm = new Normalization(dictionaries, group, sentenceText);
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
