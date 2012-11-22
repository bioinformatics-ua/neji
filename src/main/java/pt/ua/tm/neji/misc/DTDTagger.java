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

import monq.jfa.AbstractFaAction;
import monq.jfa.DfaRun;
import monq.jfa.Nfa;
import monq.jfa.ReSyntaxException;
import org.slf4j.LoggerFactory;
import pt.ua.tm.neji.core.module.BaseTagger;
import pt.ua.tm.neji.exception.NejiException;

import java.util.logging.Logger;

/**
 * Module to replace the DTD header.
 *
 * @author David Campos (<a href="mailto:david.campos@ua.pt">david.campos@ua.pt</a>)
 * @version 1.0
 * @since 1.0
 */
public class DTDTagger extends BaseTagger {

    /**
     * {@link Logger} to be used in the class.
     */
    private static org.slf4j.Logger logger = LoggerFactory.getLogger(DTDTagger.class);
    private boolean inDocType;
    private int startDocType;
    private static String neji = "<!DOCTYPE PubmedArticleSet PUBLIC \"-//NLM//DTD PubMedArticle, 1st January 2011//EN\" \"http://bioinformatics.ua.pt/support/gimli/pubmed/gimli.dtd\">";

    public DTDTagger() throws NejiException {
        super();

        try {
            Nfa nfa = new Nfa(Nfa.NOTHING);
            nfa.or("<\\!DOCTYPE PubmedArticleSet", start_doctype);
            nfa.or(">", end_doctype);
            setNFA(nfa, DfaRun.UNMATCHED_COPY);
        } catch (ReSyntaxException ex) {
            throw new NejiException(ex);
        }
        inDocType = false;
        startDocType = 0;
    }
    private AbstractFaAction start_doctype = new AbstractFaAction() {
        @Override
        public void invoke(StringBuffer yytext, int start, DfaRun runner) {
            inDocType = true;
            startDocType = start;
            runner.collect = true;
        }
    };
    private AbstractFaAction end_doctype = new AbstractFaAction() {
        @Override
        public void invoke(StringBuffer yytext, int start, DfaRun runner) {
            if (inDocType) {
                int end = yytext.indexOf(">", start) + 1;

                yytext.replace(startDocType, end, neji);

                inDocType = false;
                runner.collect = false;
            }

        }
    };
}
