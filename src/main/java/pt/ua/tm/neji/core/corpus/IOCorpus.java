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

import java.io.File;
import org.apache.commons.lang.Validate;
import pt.ua.tm.gimli.corpus.Corpus;

/**
 * Abstract class encapsulating properties common to {@link InputCorpus} and
 * {@link OutputCorpus}.
 * 
 * @author Tiago Nunes 
 */
public abstract class IOCorpus extends CorpusWrapper {

    private final File file;
    private boolean compressed;

    
    public IOCorpus(final File file, final boolean compressed) {
        this(file, compressed, null);
    }
    
    public IOCorpus(final File file, final boolean compressed, final Corpus corpus) {
        super(corpus);
        Validate.notNull(file);
        this.file = file;
        this.compressed = compressed;
    }

    public final File getFile() {
        return file;
    }

    public boolean isCompressed() {
        return compressed;
    }    
}
