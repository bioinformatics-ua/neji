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

package pt.ua.tm.neji.core.corpus;

import pt.ua.tm.gimli.corpus.Corpus;

/**
 * Container for a {@link pt.ua.tm.gimli.corpus.Corpus} object.
 *
 * @author Tiago Nunes
 */
public class CorpusWrapper {

    private Corpus corpus;

    public CorpusWrapper() {
        this(null);
    }

    public CorpusWrapper(Corpus corpus) {
        this.corpus = corpus;
    }


    public Corpus getCorpus() {
        return corpus;
    }

    public void setCorpus(Corpus corpus) {
        this.corpus = corpus;
    }

    public boolean hasCorpus() {
        return corpus != null;
    }
}
