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

package pt.ua.tm.neji.sentence;

import com.aliasi.util.Pair;
import monq.jfa.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.tm.gimli.exception.GimliException;
import pt.ua.tm.neji.core.module.BaseTagger;
import pt.ua.tm.neji.exception.NejiException;
import pt.ua.tm.neji.sentencesplitter.SentenceSplitter;

import java.util.List;

/**
 * Module to perform sentence splitting and tagging.
 * @author David Campos (<a href="mailto:david.campos@ua.pt">david.campos@ua.pt</a>)
 * @version 1.0
 * @since 1.0
 */
public class SentenceTagger extends BaseTagger {

    /** {@link Logger} to be used in the class. */
    private Logger logger = LoggerFactory.getLogger(SentenceTagger.class);
    private int startText;
    private int startTag;
    private boolean inText;
    private int sentenceCounter;
    private SentenceSplitter sentencesplitter;
    private List<Pair> sentencesPositions;
    private boolean isToProvidePositions;

    /**
     * Tag sentences parsing XML content of the specified tags.
     *
     * @param xmlTags Tags to extract content to be used.
     * @throws GimliException Problem loading the sentence tagger.
     */
    public SentenceTagger(final SentenceSplitter sentencesplitter) throws NejiException {
        super();

        this.sentencesplitter = sentencesplitter;
        this.isToProvidePositions = false;

        // Initialise XML parser
        try {
            Nfa nfa = new Nfa(Nfa.NOTHING);
            nfa.or(Xml.STag("roi"), start_text);
            nfa.or(Xml.ETag("roi"), end_text);
            setNFA(nfa, DfaRun.UNMATCHED_COPY);
        } catch (ReSyntaxException ex) {
            throw new NejiException(ex);
        }
        startText = 0;
        startTag = 0;
        sentenceCounter = 0;
        inText = false;
    }

    /**
     * Tag sentences parsing XML content of the specified tags.
     *
     * @param xmlTags Tags to extract content to be used.
     * @throws GimliException Problem loading the sentence tagger.
     */
    public SentenceTagger(final SentenceSplitter sentencesplitter, List<Pair> sentencesPositions)
            throws NejiException {
        super();

        this.sentencesplitter = sentencesplitter;
        this.sentencesPositions = sentencesPositions;
        this.isToProvidePositions = true;

        // Initialise XML parser
        try {
            Nfa nfa = new Nfa(Nfa.NOTHING);
            nfa.or(Xml.STag("roi"), start_text);
            nfa.or(Xml.ETag("roi"), end_text);
            setNFA(nfa, DfaRun.UNMATCHED_COPY);
        } catch (ReSyntaxException ex) {
            throw new NejiException(ex);
        }
        startText = 0;
        startTag = 0;
        sentenceCounter = 0;
        inText = false;
    }

    private AbstractFaAction start_text = new AbstractFaAction() {
        @Override
        public void invoke(StringBuffer yytext, int start, DfaRun runner) {
            inText = true;
            runner.collect = true;
            startText = yytext.indexOf(">", start) + 1;
            startTag = start;
        }
    };
    private AbstractFaAction end_text = new AbstractFaAction() {
        @Override
        public void invoke(StringBuffer yytext, int start, DfaRun runner) {
            if (inText) {
                StringBuffer sb = new StringBuffer(yytext.substring(startText, start));

                int[][] indices = sentencesplitter.split(sb.toString());

                int offset = 0;
                for (int i = 0; i < indices.length; i++) {
                    int s = offset + indices[i][0];
                    int e = offset + indices[i][1];

                    if (isToProvidePositions) {
                        Pair p = new Pair(indices[i][0], indices[i][1] - 1);
                        sentencesPositions.add(p);
                    }


//                    String prefix = "<s id=\"" + sentenceCounter++ + "\">";

                    String prefix = "<s";
                    prefix += " id=\"" + sentenceCounter++ + "\"";
//                    prefix += " start=\"" + (s - offset) + "\"";
//                    prefix += " end=\"" + (e - offset - 1) + "\"";
                    prefix += ">";

                    String suffix = "</s>";

                    String taggedSentence = prefix + sb.substring(s, e) + suffix;

                    sb.replace(s, e, taggedSentence);

                    offset += prefix.length() + suffix.length();
                }

                yytext.replace(startTag, yytext.indexOf(">", start) + 1, sb.toString());

                inText = false;
                runner.collect = false;
            }
        }
    };
}
