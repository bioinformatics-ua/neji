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
package pt.ua.tm.neji.core.pipeline;

import monq.jfa.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.tm.neji.core.module.Module;
import pt.ua.tm.neji.exception.NejiException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Default implementation of a pipeline. The input stream is the input of the first module, and the output of the first
 * module is the input of the second module and so on, until the last module provides the output to a storage resource
 * specified by the user.
 *
 * @author David Campos (<a href="mailto:david.campos@ua.pt">david.campos@ua.pt</a>)
 * @version 1.0
 * @since 1.0
 */
public class DefaultPipeline extends ArrayList<Module> implements Pipeline {

    /**
     * {@link Logger} to be used in the class.
     */
    private static Logger logger = LoggerFactory.getLogger(DefaultPipeline.class);

    public DefaultPipeline() {
        super();
    }

    @Override
    public Iterator<Module> iterator() {
        return super.iterator();
    }

    @Override
    public void run(InputStream input) throws NejiException {
        run(input, null);
    }

    @Override
    public void run(InputStream input, OutputStream out) throws NejiException {
        try {
            Nfa nfa = new Nfa(Nfa.NOTHING);
            Dfa dfa = nfa.compile(DfaRun.UNMATCHED_COPY);
            DfaRun previous = dfa.createRun();

            CharsetDecoder decoder = Charset.forName("UTF-8").newDecoder();
            ByteCharSource cs = new ByteCharSource(input);
            cs.setDecoder(decoder);

            previous.setIn(cs);

            for (Module a : this) {
                DfaRun inside = a.getRun();
                inside.setIn(previous);
                previous = inside;
            }
            if (out == null) {
                previous.filter();
            } else {
                PrintStream ps = new PrintStream(out, true, "UTF-8");
                previous.filter(ps);

                ps.close();
            }
        } catch (IOException | CompileDfaException ex) {
            throw new NejiException(ex);
        }
    }
}
