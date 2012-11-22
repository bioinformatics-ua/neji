/*
 * Copyright (c) 2012 David Campos, University of Aveiro and Erasmus Medical Center.
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
package pt.ua.tm.relation.corpus;

import monq.jfa.*;
import pt.ua.tm.gimli.corpus.Corpus;
import pt.ua.tm.gimli.corpus.Sentence;
import pt.ua.tm.neji.core.module.BaseLoader;
import pt.ua.tm.neji.disambiguator.Disambiguator;
import pt.ua.tm.neji.exception.NejiException;

/**
 * @author David Campos (<a href="mailto:david.campos@ua.pt">david.campos@ua.pt</a>)
 * @version 1.0
 * @since 1.0
 */
public class Disambiguate extends BaseLoader {

    private int sentenceCounter;
    private int depth;
    private boolean mergeNestedSameGroup, discardSameGroupByPriority;

    public Disambiguate(Corpus corpus, final int depth, final boolean mergeNestedSameGroup,
            final boolean discardSameGroupByPriority) throws NejiException {
        super(corpus);
        this.depth = depth;
        this.mergeNestedSameGroup = mergeNestedSameGroup;
        this.discardSameGroupByPriority = discardSameGroupByPriority;
        this.sentenceCounter = 0;

        try {
            Nfa nfa = new Nfa(Nfa.NOTHING);
            nfa.or(Xml.GoofedElement("s"), end_sentence);
            setNFA(nfa, DfaRun.UNMATCHED_COPY);
        } catch (ReSyntaxException ex) {
            throw new NejiException(ex);
        }
    }
    private AbstractFaAction end_sentence = new AbstractFaAction() {
        @Override
        public void invoke(StringBuffer yytext, int start, DfaRun runner) {
            // Get respective sentence from corpus
            Corpus corpus = getCorpus();
            Sentence s = corpus.getSentence(sentenceCounter);

            Disambiguator.disambiguate(s, depth, mergeNestedSameGroup, discardSameGroupByPriority);

            // Change state variables
            sentenceCounter++;
        }
    };
}
