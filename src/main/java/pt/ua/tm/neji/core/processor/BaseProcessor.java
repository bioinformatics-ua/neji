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

package pt.ua.tm.neji.core.processor;

import org.apache.commons.lang.Validate;
import pt.ua.tm.gimli.corpus.Corpus;
import pt.ua.tm.neji.context.Context;
import pt.ua.tm.neji.core.corpus.InputCorpus;
import pt.ua.tm.neji.core.corpus.OutputCorpus;
import pt.ua.tm.neji.core.module.Writer;
import pt.ua.tm.neji.exception.NejiException;
import pt.ua.tm.neji.writer.*;

/**
 * Abstract pipeline processor encapsulating common functionality.
 *
 * @author Tiago Nunes
 */
public abstract class BaseProcessor implements Processor {

    private Context context;
    private InputCorpus inputCorpus;
    private OutputCorpus outputCorpus;

    public BaseProcessor(Context context, InputCorpus inputCorpus, OutputCorpus outputCorpus) {
        setContext(context);
        setInputCorpus(inputCorpus);
        setOutputCorpus(outputCorpus);
    }

    @Override
    public final Context getContext() {
        return context;
    }

    @Override
    public final InputCorpus getInputCorpus() {
        return inputCorpus;
    }

    @Override
    public final OutputCorpus getOutputCorpus() {
        return outputCorpus;
    }

    @Override
    public final void setContext(Context context) {
        Validate.notNull(context);
        this.context = context;
    }

    @Override
    public final void setInputCorpus(InputCorpus inputCorpus) {
        Validate.notNull(inputCorpus);
        this.inputCorpus = inputCorpus;
    }

    @Override
    public final void setOutputCorpus(OutputCorpus outputCorpus) {
        Validate.notNull(outputCorpus);
        this.outputCorpus = outputCorpus;
    }

    protected Writer newWriter(Corpus corpus) throws NejiException {
        switch (getOutputCorpus().getFormat()) {
            case A1:
                return new A1Writer(corpus);
            case CONLL:
                return new CoNLLWriter(corpus);
            case JSON:
                return new JSONWriter(corpus);
            case NEJI:
                return new NejiWriter(corpus);
            case XML:
                return new IeXMLWriter(corpus, 2, true, false);
            case B64:
                return new Base64Writer(corpus);
            case CUSTOM:
                return null; // It's the custom processor's responsability to install writer(s) or post-process/save the output.
            default:
                throw new RuntimeException("Invalid output format.");
        }
    }

}
