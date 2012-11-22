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

package pt.ua.tm.neji.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.tm.gimli.corpus.Corpus;
import pt.ua.tm.neji.context.Context;
import pt.ua.tm.neji.context.ContextProcessors;
import pt.ua.tm.neji.core.batch.Batch;
import pt.ua.tm.neji.core.corpus.InputCorpus;
import pt.ua.tm.neji.core.corpus.InputCorpus.InputFormat;
import pt.ua.tm.neji.core.corpus.OutputCorpus;
import pt.ua.tm.neji.core.corpus.OutputCorpus.OutputFormat;
import pt.ua.tm.neji.core.processor.BaseProcessor;
import pt.ua.tm.neji.dictionary.Dictionary;
import pt.ua.tm.neji.dictionary.DictionaryHybrid;
import pt.ua.tm.neji.batch.FileBatchExecutor;
import pt.ua.tm.neji.core.pipeline.DefaultPipeline;
import pt.ua.tm.neji.exception.NejiException;
import pt.ua.tm.neji.ml.MLHybrid;
import pt.ua.tm.neji.nlp.NLP;
import pt.ua.tm.neji.reader.XMLReader;
import pt.ua.tm.neji.sentence.SentenceTagger;
import pt.ua.tm.neji.writer.IeXMLWriter;

/**
 * Test a pipeline processor.
 * @author David Campos (<a href="mailto:david.campos@ua.pt">david.campos@ua.pt</a>)
 * @version 1.0
 * @since 1.0
 */
public class TestProcessor extends BaseProcessor {
    /** {@link org.slf4j.Logger} to be used in the class. */
    private static Logger logger = LoggerFactory.getLogger(TestProcessor.class);

    public TestProcessor(Context context, InputCorpus input, OutputCorpus output) {
        super(context, input, output);
    }

    /**
     * When an object implementing interface <code>Runnable</code> is used to create a thread, starting the thread
     * causes the object's <code>run</code> method to be called in that separately executing thread.
     * <p/>
     * The general contract of the method <code>run</code> is that it may take any action whatsoever.
     *
     * @see Thread#run()
     */
    @Override
    public void run() {
        try {
            Context context = getContext();
            ContextProcessors cp = context.take(); // Take parser, sentence splitter and CRF
            Corpus corpus = getInputCorpus().getCorpus(); // Create corpus to store data
            DefaultPipeline p = new DefaultPipeline(); // Create pipeline
            p.add(new XMLReader(new String[]{"ArticleTitle", "AbstractText"})); // Reader
            p.add(new SentenceTagger(cp.getSentenceSplitter())); // Sentence tagger
            p.add(new NLP(corpus, cp.getParser())); // NLP
            for (Dictionary d : context.getDictionaries()) { // Dictionary matching
                p.add(new DictionaryHybrid(d, corpus));
            }
            for (int i = 0; i < context.getModels().size(); i++) { // Machine learning
                p.add(new MLHybrid(corpus, context.getModels().get(i), cp.getCRF(i)));
            }
            p.add(new IeXMLWriter(corpus)); //Writer
            p.run(getInputCorpus().getInStream(), getOutputCorpus().getOutStream()); //Run pipeline
            context.put(cp); // Return parser, sentence splitter and CRF
        } catch (NejiException | InterruptedException ex) {
            throw new RuntimeException("There was a problem running the pipeline.", ex);
        }
    }

    public static void main(String... args) {
        // Input and output resources
        String inputFolder = "input/";
        String outputFolder = "output/";
        String dictionariesFolder = "resources/dictionaries/";
        String modelsFolder = "resources/models";
        // Create context
        Context context = new Context(modelsFolder, dictionariesFolder);
        // Run batch
        try {
            boolean areFilesCompressed = true;
            int numThreads = 6;
            Batch batch = new FileBatchExecutor(inputFolder, InputFormat.XML, outputFolder, OutputFormat.XML,
                    areFilesCompressed, numThreads);
            Class p = TestProcessor.class;
            batch.run(p, context);
        } catch (Exception ex) {
            logger.error("There was a problem processing the files.", ex);
        }

    }
}
