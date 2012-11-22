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
package pt.ua.tm.neji.dictionary;

import monq.jfa.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.tm.gimli.config.Resources;
import pt.ua.tm.gimli.corpus.AnnotationID;
import pt.ua.tm.gimli.corpus.Corpus;
import pt.ua.tm.gimli.corpus.Identifier;
import pt.ua.tm.gimli.corpus.Sentence;
import pt.ua.tm.neji.core.module.BaseHybrid;
import pt.ua.tm.neji.exception.NejiException;
import pt.ua.tm.neji.util.Char;
import uk.ac.man.entitytagger.Mention;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Hybrid module to perform dictionary matching and load the resulting concepts into the internal {@link Corpus}
 * representation.
 *
 * @author David Campos (<a href="mailto:david.campos@ua.pt">david.campos@ua.pt</a>)
 * @version 1.0
 * @since 1.0
 */
public class DictionaryHybrid extends BaseHybrid {

    /**
     * {@link Logger} to be used in the class.
     */
    private static Logger logger = LoggerFactory.getLogger(DictionaryHybrid.class);
    private Dictionary dictionary;
    private int startSentence;
    private int sentence;

    public DictionaryHybrid(Dictionary dictionary, Corpus corpus) throws NejiException {
        super(corpus);
        assert (dictionary != null);
        this.dictionary = dictionary;

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

            StringBuilder sb = new StringBuilder(yytext.substring(startSentence, start));
            String sentenceText = sb.toString();

            List<Mention> mentions = dictionary.getMatcher().match(sentenceText);


            // Filter mentions to remove intersections (does not happen frequently)
            List<Mention> toRemove = new ArrayList<Mention>();
            for (int i = 0; i < mentions.size() - 1; i++) {
                for (int j = i + 1; j < mentions.size(); j++) {
                    Mention m1 = mentions.get(i);
                    Mention m2 = mentions.get(j);
                    int size_m1 = m1.getText().length();
                    int size_m2 = m2.getText().length();

                    if (m1.getStart() >= m2.getStart() && m1.getStart() <= m2.getEnd()) {
                        if (size_m1 > size_m2) {
                            if (!toRemove.contains(m2)) {
                                toRemove.add(m2);
                            }
                        } else {
                            if (!toRemove.contains(m1)) {
                                toRemove.add(m1);
                            }
                        }
                    } else if (m1.getEnd() >= m2.getStart() && m1.getEnd() <= m2.getEnd()) {
                        if (size_m1 > size_m2) {
                            if (!toRemove.contains(m2)) {
                                toRemove.add(m2);
                            }
                        } else {
                            if (!toRemove.contains(m1)) {
                                toRemove.add(m1);
                            }
                        }
                    }
                }
            }

            mentions.removeAll(toRemove);

            // Get pattern for stopwords recognition
            Pattern stopwords;
            try {
                stopwords = Resources.getStopwordsPattern();
            } catch (Exception ex) {
                logger.error("There was a problem loading the stopwords pattern matcher.", ex);
                return;
            }

            // Add annotations
            int endLastEntity = 0;
            int previousNumChars = 0;
            for (Mention m : mentions) {

                // Discard stopwords recognition
                java.util.regex.Matcher mather = stopwords.matcher(m.getText());
                if (mather.matches()) {
                    continue;
                }

                String sentenceBeforeEntity = sentenceText.substring(endLastEntity, m.getStart());

                int numChars = Char.getNumNonWhiteSpaceChars(sentenceBeforeEntity);

                int startEntityChars = previousNumChars + numChars;
                int endEntityChars = previousNumChars + numChars + Char.getNumNonWhiteSpaceChars(m.getText()) - 1;

                String id = m.getIdsToString();
                Sentence s = getCorpus().getSentence(sentence);
                AnnotationID a = AnnotationID.newAnnotationIDByCharPositions(s, startEntityChars, endEntityChars, 1.0);

                if (a != null) {
                    List<Identifier> ids = Identifier.getIdentifiersFromText(id);
                    a.setIDs(ids);
                    s.addAnnotationToTree(a);
                }

                startSentence = m.getEnd() + 1;
                previousNumChars = endEntityChars + 1;
                endLastEntity = m.getEnd();
            }

            runner.collect = false;
            sentence++;
        }
    };
}
