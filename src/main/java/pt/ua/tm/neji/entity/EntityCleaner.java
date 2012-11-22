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
import pt.ua.tm.neji.core.module.BaseTagger;
import pt.ua.tm.neji.exception.NejiException;

/**
 * Module to clean entity annotations from input stream.
 * @author David Campos (<a href="mailto:david.campos@ua.pt">david.campos@ua.pt</a>)
 * @version 1.0
 * @since 1.0
 */
public class EntityCleaner extends BaseTagger {

    private boolean inSentence;
    private boolean inEntity;
    private int startEntity;

    public EntityCleaner() throws NejiException {
        super();

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

        this.inSentence = false;
        this.inEntity = false;
        this.startEntity = 0;
    }

    private AbstractFaAction start_sentence = new AbstractFaAction() {

        @Override
        public void invoke(StringBuffer yytext, int start, DfaRun runner) {
            inSentence = true;
            runner.collect = true;
        }
    };
    private AbstractFaAction end_sentence = new AbstractFaAction() {

        @Override
        public void invoke(StringBuffer yytext, int start, DfaRun runner) {
            inSentence = false;
            runner.collect = false;
        }
    };
    private AbstractFaAction start_entity = new AbstractFaAction() {

        @Override
        public void invoke(StringBuffer yytext, int start, DfaRun runner) {
            if (inSentence) {
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

                int start_id = entity.indexOf("id=\"") + 4;
                int end_id = entity.indexOf("\">", start_id);
                String text = entity.substring(end_id + 2, entity.length());


                inEntity = false;
                yytext.replace(startEntity, start + 5, text);
            }
        }
    };
}
