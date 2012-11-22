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

package pt.ua.tm.neji.context;

import pt.ua.tm.gimli.external.wrapper.Parser;
import pt.ua.tm.gimli.model.CRFBase;
import pt.ua.tm.neji.sentencesplitter.SentenceSplitter;

import java.util.List;

/**
 * Helper that provides access to {@link Context} processors, namely sentence splitters, parsers and ML models.
 * @author David Campos (<a href="mailto:david.campos@ua.pt">david.campos@ua.pt</a>)
 * @version 1.0
 * @since 1.0
 */
public class ContextProcessors {

    private Parser parser;
    private SentenceSplitter splitter;
    private List<CRFBase> crfs;

    public ContextProcessors(final Parser parser, final SentenceSplitter splitter, final List<CRFBase> crfs) {
        this.parser = parser;
        this.splitter = splitter;
        this.crfs = crfs;
    }

    public Parser getParser() {
        return parser;
    }

    public SentenceSplitter getSentenceSplitter() {
        return splitter;
    }

    public List<CRFBase> getCRFs() {
        return crfs;
    }

    public CRFBase getCRF(int i) {
        return crfs.get(i);
    }
}
