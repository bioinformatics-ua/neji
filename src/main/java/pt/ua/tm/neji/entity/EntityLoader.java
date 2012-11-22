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

package pt.ua.tm.neji.entity;

import monq.jfa.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.tm.gimli.corpus.AnnotationID;
import pt.ua.tm.gimli.corpus.Corpus;
import pt.ua.tm.gimli.corpus.Identifier;
import pt.ua.tm.gimli.corpus.Sentence;
import pt.ua.tm.neji.core.module.BaseLoader;
import pt.ua.tm.neji.exception.NejiException;
import pt.ua.tm.neji.util.Char;

import java.util.List;

/**
 * Module to load entity annotations from the input stream to the internal {@link Corpus}.
 * @author David Campos (<a href="mailto:david.campos@ua.pt">david.campos@ua.pt</a>)
 * @version 1.0
 * @since 1.0
 */
public class EntityLoader extends BaseLoader {

    /** {@link Logger} to be used in the class. */
    private static Logger logger = LoggerFactory.getLogger(EntityLoader.class);
    private int sentence;
    private boolean inSentence;
    private boolean inEntity;
    private int startEntity;
    private int startSentence;
    private int previousNumChars;

    public EntityLoader(Corpus corpus) throws NejiException {
        super(corpus);

        try {
            Nfa nfa = new Nfa(Nfa.NOTHING);
            nfa.or(Xml.STag("s"), start_sentence);
            nfa.or(Xml.ETag("s"), end_sentence);
            nfa.or(Xml.STag("e"), start_entity);
            nfa.or(Xml.ETag("e"), end_entity);
            setNFA(nfa, DfaRun.UNMATCHED_COPY);
        } catch (ReSyntaxException ex) {
            throw new NejiException(ex);
        }

        this.sentence = 0;
        this.inSentence = false;
        this.inEntity = false;
        this.startEntity = 0;
        this.startSentence = 0;
        this.previousNumChars = 0;
    }

    private AbstractFaAction start_sentence = new AbstractFaAction() {

        @Override
        public void invoke(StringBuffer yytext, int start, DfaRun runner) {
            inSentence = true;
            startSentence = yytext.indexOf(">", start) + 1;
            runner.collect = true;
            previousNumChars = 0;
        }
    };
    private AbstractFaAction end_sentence = new AbstractFaAction() {

        @Override
        public void invoke(StringBuffer yytext, int start, DfaRun runner) {
            inSentence = false;
            sentence++;
            runner.collect = false;
        }
    };
    private AbstractFaAction start_entity = new AbstractFaAction() {

        @Override
        public void invoke(StringBuffer yytext, int start, DfaRun runner) {
            if (inSentence) {
                //logger.info("ENTITY: {}", yytext);

                /*
                 * map.clear(); Xml.splitElement(map, yytext, start); String
                 * entity = map.get(">");
                 *
                 * logger.info("ENTITY: {}", entity);
                 */

                inEntity = true;
                startEntity = start;
            }
        }
    };
    private AbstractFaAction end_entity = new AbstractFaAction() {

        @Override
        public void invoke(StringBuffer yytext, int start, DfaRun runner) {
            if (inEntity) {

                String entity = yytext.substring(startEntity, start);

                //logger.info("ENTITY: {}", entity);

                int start_id = entity.indexOf("id=\"") + 4;
                int end_id = entity.indexOf("\">", start_id);
                String id = entity.substring(start_id, end_id);

                //logger.info("ID: {}", id);

                String text = entity.substring(end_id + 2, entity.length());
                //logger.info("TEXT: {}", text);

                String sentenceBeforeEntity = yytext.substring(startSentence, startEntity);
                //logger.info("BEFORE ENTITY: {}", sentenceBeforeEntity);

                int numChars = Char.getNumNonWhiteSpaceChars(sentenceBeforeEntity);

                int startEntityChars = previousNumChars + numChars;
                int endEntityChars = previousNumChars + numChars + Char.getNumNonWhiteSpaceChars(text) - 1;

                //logger.info("[{} - {}]", startEntityChars, endEntityChars);

                //String entityType = id.substring(id.lastIndexOf(":") + 1);
                //addAnnotation(corpus.getSentence(sentence), startEntityChars, endEntityChars, entityType, id);

                Sentence s = getCorpus().getSentence(sentence);
                AnnotationID a = AnnotationID.newAnnotationIDByCharPositions(s, startEntityChars, endEntityChars, 1.0);

                if (a != null) {
                    List<Identifier> ids = Identifier.getIdentifiersFromText(id);
                    a.setIDs(ids);
                    s.addAnnotationToTree(a);
                }

                //logger.info("");

                inEntity = false;
                startSentence = start + 4;
                previousNumChars = endEntityChars + 1;
            }
        }
    };
}
