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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;
import org.apache.commons.lang.Validate;
import pt.ua.tm.gimli.corpus.Corpus;

/**
 * An Input corpus.
 * 
 * @author Tiago Nunes
 */
public class InputCorpus extends IOCorpus {
    
    public enum InputFormat {
        XML, RAW
    };
    private final InputStream inStream;
    private final InputFormat format;

    
    public InputCorpus(final File file, final InputFormat format,
            final boolean compressed) throws FileNotFoundException, IOException {
        this(file, newStreamForFile(file, compressed), format, compressed);
    }
    
    public InputCorpus(final File file, final InputFormat format,
            final boolean compressed, final Corpus corpus) throws FileNotFoundException, IOException {
        this(file, newStreamForFile(file, compressed), format, compressed, corpus);
    }
    
    public InputCorpus(final File file, final InputStream inStream,
            final InputFormat format, final boolean compressed) {
        this(file, inStream, format, compressed, null);
    }
    
    public InputCorpus(final File file, final InputStream inStream,
            final InputFormat format, final boolean compressed,
            final Corpus corpus) {
        super(file, compressed, corpus);
        Validate.notNull(inStream);
        Validate.notNull(format);
        Validate.isTrue(file.isFile() && file.canRead());
        
        this.inStream = inStream;
        this.format = format;
    }

    public InputStream getInStream() {
        return inStream;
    }

    public InputFormat getFormat() {
        return format;
    }
    
    public static InputStream newStreamForFile(File file, boolean compressed)
            throws FileNotFoundException, IOException {
        InputStream is = new FileInputStream(file);
        if (compressed) {
            is = new GZIPInputStream(is);
        }
        return is;
    }
}
