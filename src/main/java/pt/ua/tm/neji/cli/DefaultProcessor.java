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
package pt.ua.tm.neji.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.tm.gimli.corpus.Corpus;
import pt.ua.tm.gimli.model.CRFBase;
import pt.ua.tm.neji.context.Context;
import pt.ua.tm.neji.context.ContextProcessors;
import pt.ua.tm.neji.core.corpus.InputCorpus;
import pt.ua.tm.neji.core.corpus.OutputCorpus;
import pt.ua.tm.neji.core.module.Reader;
import pt.ua.tm.neji.core.module.Writer;
import pt.ua.tm.neji.core.pipeline.DefaultPipeline;
import pt.ua.tm.neji.core.pipeline.Pipeline;
import pt.ua.tm.neji.core.processor.BaseProcessor;
import pt.ua.tm.neji.dictionary.Dictionary;
import pt.ua.tm.neji.dictionary.DictionaryHybrid;
import pt.ua.tm.neji.exception.NejiException;
import pt.ua.tm.neji.misc.DTDTagger;
import pt.ua.tm.neji.ml.MLHybrid;
import pt.ua.tm.neji.ml.MLModel;
import pt.ua.tm.neji.nlp.NLP;
import pt.ua.tm.neji.reader.RawReader;
import pt.ua.tm.neji.reader.XMLReader;
import pt.ua.tm.neji.sentence.SentenceTagger;

/**
 * Default pipeline processor used in the CLI tool.
 * @author David Campos (<a href="mailto:david.campos@ua.pt">david.campos@ua.pt</a>)
 * @version 1.0
 * @since 1.0
 */
public class DefaultProcessor extends BaseProcessor {

    /** {@link org.slf4j.Logger} to be used in the class. */
    private static Logger logger = LoggerFactory.getLogger(DefaultProcessor.class);

    private String[] xmlTags;

    public DefaultProcessor(Context context, InputCorpus inputCorpus, OutputCorpus outputCorpus) {
        super(context, inputCorpus, outputCorpus);
    }

    public DefaultProcessor(Context context, InputCorpus inputCorpus, OutputCorpus outputCorpus,
                            String[] xmlTags) {
        super(context, inputCorpus, outputCorpus);
        this.xmlTags = xmlTags;
    }

    @Override
    public void run() {
        try {
            // Get context and processors
            Context context = getContext();
            ContextProcessors cp = context.take();

            // Get corpus
            Corpus corpus = getInputCorpus().getCorpus();

            // Create Pipeline
            Pipeline p = new DefaultPipeline();

            // Change DocType of the document (DTD)
            if (getInputCorpus().getFormat().equals(InputCorpus.InputFormat.XML) &&
                    getOutputCorpus().getFormat().equals(OutputCorpus.OutputFormat.XML)) {
                DTDTagger doc = new DTDTagger();
                p.add(doc);
            }

            // Reader
            Reader reader = null;
            if (getInputCorpus().getFormat().equals(InputCorpus.InputFormat.XML)) {
                reader = new XMLReader(xmlTags);
            } else if (getInputCorpus().getFormat().equals(InputCorpus.InputFormat.RAW)) {
                reader = new RawReader();
            } else {
                throw new RuntimeException("Invalid input format.");
            }
            p.add(reader);

            // Sentence tagger
            SentenceTagger stl = new SentenceTagger(cp.getSentenceSplitter());
            p.add(stl);

            // NLP
            NLP nlp = new NLP(corpus, cp.getParser());
            p.add(nlp);


            // Dictionary matching
            for (Dictionary d : getContext().getDictionaries()) {
                DictionaryHybrid dtl = new DictionaryHybrid(d, corpus);
                p.add(dtl);
            }

            // Machine learning
            for (int i = 0; i < getContext().getModels().size(); i++) {
                // Take model
                CRFBase crf = cp.getCRF(i);

                // Add ML recognizer to pipeline
                MLHybrid ml;
                MLModel model = getContext().getModels().get(i);
                if (model.hasNormalizationDictionaries()) {
                    ml = new MLHybrid(corpus, crf, model.getSemanticGroup(),
                            model.getNormalizationDictionaries());
                } else {
                    ml = new MLHybrid(corpus, crf, model.getSemanticGroup());
                }
                p.add(ml);
            }

            Writer writer = newWriter(corpus);

            if (writer != null) {
                p.add(writer);

                p.run(getInputCorpus().getInStream(), getOutputCorpus().getOutStream());
            } else {
                p.run(getInputCorpus().getInStream());

                logger.warn("Discarding processed output for file.");
            }

            // Return processors
            context.put(cp);

        } catch (NejiException | InterruptedException ex) {
            logger.error("ERROR:", ex);
            throw new RuntimeException(
                    "There was a problem annotating the stream with the identifier " + getInputCorpus().getCorpus().getIdentifier(),
                    ex);
        }

        logger.info("Done processing: {}", getInputCorpus().getCorpus().getIdentifier());

        // Clear memory
        System.gc();
    }
}
