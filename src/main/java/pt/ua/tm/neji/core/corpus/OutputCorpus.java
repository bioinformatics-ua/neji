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

import org.apache.commons.lang.Validate;
import pt.ua.tm.gimli.corpus.Corpus;

import java.io.*;
import java.util.zip.GZIPOutputStream;

/**
 * An output corpus.
 *
 * @author Tiago Nunes
 */
public class OutputCorpus extends IOCorpus {

    public enum OutputFormat {
        A1, NEJI, JSON, CONLL, XML, B64, CUSTOM
    }

    ;

    private final OutputStream outStream;
    private final OutputFormat format;

    public OutputCorpus(final File file, final OutputFormat format,
                        final boolean compressed) throws FileNotFoundException, IOException {
        this(file, newStreamForFile(file, compressed), format, compressed);
    }

    public OutputCorpus(final File file, final OutputFormat format,
                        final boolean compressed, final Corpus corpus) throws FileNotFoundException, IOException {
        this(file, newStreamForFile(file, compressed), format, compressed, corpus);
    }

    public OutputCorpus(final File file, final OutputStream outStream,
                        final OutputFormat format, final boolean compressed) {
        this(file, outStream, format, compressed, null);
    }

    public OutputCorpus(final File file, final OutputStream outStream,
                        final OutputFormat format, final boolean compressed,
                        final Corpus corpus) {
        super(file, compressed, corpus);
        Validate.notNull(outStream);
        Validate.notNull(format);
        Validate.isTrue(file.isFile() && file.canRead());

        this.outStream = outStream;
        this.format = format;
    }

    public OutputStream getOutStream() {
        return outStream;
    }

    public OutputFormat getFormat() {
        return format;
    }

    public static File newOutputFile(String outputFolder, String name,
                                     OutputFormat format, boolean compressed) {
        String filename = String.format("%s.%s%s", name,
                format.toString().toLowerCase(), compressed ? ".gz" : "");

        return new File(outputFolder, filename);
    }

    public static OutputStream newStreamForFile(File file, boolean compressed)
            throws FileNotFoundException, IOException {
        OutputStream os = new FileOutputStream(file);
        if (compressed) {
            os = new GZIPOutputStream(os);
        }
        return os;
    }
}
