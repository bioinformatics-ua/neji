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

package pt.ua.tm.neji.misc;

import monq.jfa.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.tm.neji.core.module.BaseTagger;
import pt.ua.tm.neji.exception.NejiException;

/**
 * Module to remove XML and HTML tags.
 * @author David Campos (<a href="mailto:david.campos@ua.pt">david.campos@ua.pt</a>)
 * @version 1.0
 * @since 1.0
 */
public class TagCleaner extends BaseTagger {

    private static Logger logger = LoggerFactory.getLogger(TagCleaner.class);
    private int startTag;
    private int startContent;
    boolean inTag;

    public TagCleaner() throws NejiException{
        try {
            Nfa nfa = new Nfa(Nfa.NOTHING);
            nfa.or(".+", text);
            setNFA(nfa, DfaRun.UNMATCHED_COPY);
        } catch (ReSyntaxException ex) {
            throw new NejiException(ex);
        }
    }
    
    public TagCleaner(String tag) throws NejiException {
        super();

        try {
            Nfa nfa = new Nfa(Nfa.NOTHING);

            nfa.or(Xml.STag(tag), start_tag);
            nfa.or(Xml.ETag(tag), end_tag);

            setNFA(nfa, DfaRun.UNMATCHED_COPY);
        } catch (ReSyntaxException ex) {
            throw new NejiException(ex);
        }

        this.startTag = 0;
        this.startContent = 0;
    }

    private AbstractFaAction start_tag = new AbstractFaAction() {
        @Override
        public void invoke(StringBuffer yytext, int start, DfaRun runner) {
            inTag = true;
            startContent = yytext.indexOf(">", start) + 1;
            startTag = start;
            runner.collect = true;
        }
    };
    private AbstractFaAction end_tag = new AbstractFaAction() {
        @Override
        public void invoke(StringBuffer yytext, int start, DfaRun runner) {

            String content = yytext.substring(startContent, start);

            yytext.replace(startTag, yytext.indexOf(">", start) + 1, content);

            inTag = false;
            runner.collect = false;
        }
    };
    
    private AbstractFaAction text = new AbstractFaAction() {
        @Override
        public void invoke(StringBuffer yytext, int start, DfaRun runner) {
            int size = yytext.length();
            String text = yytext.toString();
            text = text.replaceAll("\\<.*?>", "");
            yytext.replace(start, size, text);
        }
    };
}
