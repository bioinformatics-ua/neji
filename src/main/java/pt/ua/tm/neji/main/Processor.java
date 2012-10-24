/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.ua.tm.neji.main;

import java.io.InputStream;
import java.io.OutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.tm.gimli.config.Constants;
import pt.ua.tm.gimli.corpus.Corpus;
import pt.ua.tm.gimli.exception.GimliException;
import pt.ua.tm.gimli.external.gdep.GDepParser;
import pt.ua.tm.gimli.model.CRFModel;
import pt.ua.tm.neji.Main.InputFormat;
import pt.ua.tm.neji.Main.OutputFormat;
import pt.ua.tm.neji.context.Context;
import pt.ua.tm.neji.core.Pipeline;
import pt.ua.tm.neji.core.Tagger;
import pt.ua.tm.neji.dictionary.Dictionary;
import pt.ua.tm.neji.dictionary.DictionaryHybrid;
import pt.ua.tm.neji.misc.DTDTagger;
import pt.ua.tm.neji.ml.MLHybrid;
import pt.ua.tm.neji.ml.MLModel;
import pt.ua.tm.neji.nlp.NLP;
import pt.ua.tm.neji.reader.RawReader;
import pt.ua.tm.neji.reader.XMLReader;
import pt.ua.tm.neji.sentence.SentenceTagger;
import pt.ua.tm.neji.sentencesplitter.SentenceSplitter;
import pt.ua.tm.neji.writer.A1Writer;
import pt.ua.tm.neji.writer.Base64Writer;
import pt.ua.tm.neji.writer.CoNLLWriter;
import pt.ua.tm.neji.writer.IeXMLWriter;
import pt.ua.tm.neji.writer.JSONWriter;
import pt.ua.tm.neji.writer.NejiWriter;

/**
 *
 * @author david
 */
public class Processor implements Runnable {

    /**
     * {@link Logger} to be used in the class.
     */
    private static Logger logger = LoggerFactory.getLogger(Processor.class);
    private String identifier;
    private InputStream input;
    private OutputStream output;
    private Context context;
    private InputFormat inputFormat;
    private OutputFormat outputFormat;
    private String[] xmlTags;

    public Processor(InputStream input, OutputStream output, String identifier, Context context, InputFormat inputFormat, OutputFormat outputFormat, String[] xmlTags) {
        this.input = input;
        this.output = output;
        this.context = context;
        this.identifier = identifier;
        this.inputFormat = inputFormat;
        this.outputFormat = outputFormat;
        this.xmlTags = xmlTags;
    }

    public Processor(InputStream input, OutputStream output, String identifier, Context context, InputFormat inputFormat, OutputFormat outputFormat) {
        this(input, output, identifier, context, inputFormat, outputFormat, null);
    }

    @Override
    public void run() {
        try {
            // Create corpus
            Corpus c = new Corpus(Constants.LabelFormat.BIO, Constants.EntityType.protein);

            // Obtain mandatory resources from context
            GDepParser parser = (GDepParser) context.takeParser();
            SentenceSplitter ss = context.takeSentenceSplitter();

            // Create Pipeline
            Pipeline p = new Pipeline();

            // Change DocType of the document (DTD)
            if (inputFormat.equals(InputFormat.XML) && outputFormat.equals(OutputFormat.XML)) {
                DTDTagger doc = new DTDTagger();
                p.addModule(doc);
            }

            // Reader
            Tagger reader = null;
            if (inputFormat.equals(InputFormat.XML)) {
                reader = new XMLReader(xmlTags);
            } else if (inputFormat.equals(InputFormat.RAW)) {
                reader = new RawReader();
            } else {
                throw new RuntimeException("Invalid input format.");
            }
            p.addModule(reader);

            // Sentence tagger
            SentenceTagger stl = new SentenceTagger(ss);
            p.addModule(stl);

            // NLP
            NLP nlp = new NLP(c, parser);
            p.addModule(nlp);


            // Dictionary matching
            for (Dictionary d : context.getDictionaries()) {
                DictionaryHybrid dtl = new DictionaryHybrid(d, c);
                p.addModule(dtl);
            }

            // Machine learning
            CRFModel[] usedModels = new CRFModel[context.sizeModels()];
            for (int i = 0; i < context.sizeModels(); i++) {
                // Take model
                CRFModel crf = context.getModel(i).take();
                usedModels[i] = crf;

                // Add ML recognizer to pipeline
                MLHybrid ml;
                MLModel model = context.getModel(i);
                if (model.hasNormalizationDictionaries()) {
                    ml = new MLHybrid(c, crf, model.getSemanticGroup(), model.getNormalizationDictionaries());
                } else {
                    ml = new MLHybrid(c, crf, model.getSemanticGroup());
                }
                p.addModule(ml);
            }

            Tagger writer = null;
            if (outputFormat.equals(OutputFormat.XML)) {
                writer = new IeXMLWriter(c, 2, true, false);
            } else if (outputFormat.equals(OutputFormat.A1)) {
                writer = new A1Writer(c);
            } else if (outputFormat.equals(OutputFormat.NEJI)) {
                writer = new NejiWriter(c);
            } else if (outputFormat.equals(OutputFormat.JSON)) {
                writer = new JSONWriter(c);
            } else if (outputFormat.equals(OutputFormat.CONLL)) {
                writer = new CoNLLWriter(c);
            } else if (outputFormat.equals(OutputFormat.B64)) {
                writer = new Base64Writer(c);
            } else {
                throw new RuntimeException("Invalid output format.");
            }
            p.addModule(writer);

            // Run
            p.run(input, output);

            // Return Sentence Splitter
            context.putSenteceSplitter(ss);
            // Return parser
            context.putParser(parser);

            // Return models
            for (int i = 0; i < usedModels.length; i++) {
                context.getModel(i).put(usedModels[i]);
            }


        } catch (GimliException ex) {
            logger.error("ERROR:", ex);
            throw new RuntimeException("There was a problem annotating the stream with the identifier " + identifier, ex);
        } catch (InterruptedException ex) {
            logger.error("ERROR:", ex);
            throw new RuntimeException("There was a problem annotating the stream with the identifier " + identifier, ex);
        }

        logger.info("Done processing: {}", identifier);

        // Clean memory
        System.gc();
    }
}
