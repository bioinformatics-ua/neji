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

package pt.ua.tm.neji.core.module;


import monq.jfa.ByteCharSource;
import monq.jfa.DfaRun;
import pt.ua.tm.gimli.corpus.Corpus;
import pt.ua.tm.neji.exception.NejiException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Abstract class that integrates base functionalities of a {@link Loader} module.
 * @author David Campos (<a href="mailto:david.campos@ua.pt">david.campos@ua.pt</a>)
 * @version 1.0
 * @since 1.0
 */
public abstract class BaseLoader extends BaseModule implements Loader {

    private Corpus corpus;

    public BaseLoader(Corpus corpus) {
        super();
        this.corpus = corpus;
    }

    @Override
    public Corpus getCorpus() {
        return corpus;
    }

    @Override
    public void process(final InputStream in) throws NejiException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            DfaRun run = new DfaRun(getDFA());
            run.setIn(new ByteCharSource(in));
            run.filter();
        } catch (IOException ex) {
            throw new NejiException(ex);
        }
    }
}
