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
package pt.ua.tm.neji.writer;

import monq.jfa.AbstractFaAction;
import monq.jfa.DfaRun;
import monq.jfa.Nfa;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.tm.gimli.corpus.Corpus;
import pt.ua.tm.neji.core.module.BaseWriter;
import pt.ua.tm.neji.exception.NejiException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

/**
 * Writer that serializes the {@link Corpus} representation.
 *
 * @author Tiago Nunes
 * @version 1.0
 * @since 1.0
 */
public class Base64Writer extends BaseWriter {

    /**
     * {@link Logger} to be used in the class.
     */
    private static Logger logger = LoggerFactory.getLogger(Base64Writer.class);
    private CorpusDumper dumper;

    public Base64Writer(final Corpus corpus) throws NejiException {
        super();
        this.dumper = new CorpusDumper(corpus);
        Nfa nfa = new Nfa(Nfa.NOTHING);
        setNFA(nfa, DfaRun.UNMATCHED_COPY, this.dumper.newFaAction());
    }

    private static final class CorpusDumper {

        private final Corpus corpus;

        public CorpusDumper(Corpus corpus) {
            this.corpus = corpus;
        }

        public AbstractFaAction newFaAction() {
            return new AbstractFaAction() {
                @Override
                public void invoke(StringBuffer yytext, int start, DfaRun runner) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ObjectOutputStream oos;
                    try {
                        oos = new ObjectOutputStream(baos);
                        oos.writeObject(CorpusDumper.this.corpus);
                        oos.close();
                    } catch (IOException ex) {
                        logger.error("Error serializing corpus object", ex);
                        throw new RuntimeException("Error serializing corpus object", ex);
                    }

                    yytext.replace(0, yytext.length(), Base64.encodeBase64String(baos.toByteArray()));
                    runner.collect = false;
                }
            };
        }
    }
}
