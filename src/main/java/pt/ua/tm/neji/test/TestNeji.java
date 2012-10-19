/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.ua.tm.neji.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.List;
import java.util.zip.GZIPInputStream;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.tm.gimli.config.Constants;
import pt.ua.tm.gimli.config.Constants.Parsing;
import pt.ua.tm.gimli.config.ModelConfig;
import pt.ua.tm.gimli.config.Resources;
import pt.ua.tm.gimli.corpus.Corpus;
import pt.ua.tm.gimli.external.gdep.GDepParser;
import pt.ua.tm.gimli.model.CRFModel;
import pt.ua.tm.neji.context.Context;
import pt.ua.tm.neji.core.Pipeline;
import pt.ua.tm.neji.dictionary.DictionariesLoader;
import pt.ua.tm.neji.dictionary.Dictionary;
import pt.ua.tm.neji.dictionary.DictionaryHybrid;
import pt.ua.tm.neji.ml.MLHybrid;
import pt.ua.tm.neji.ml.MLModel;
import pt.ua.tm.neji.nlp.NLP;
import pt.ua.tm.neji.reader.RawReader;
import pt.ua.tm.neji.sentence.SentenceTagger;
import pt.ua.tm.neji.sentencesplitter.SentenceSplitter;
import pt.ua.tm.neji.writer.A1Writer;
import pt.ua.tm.neji.writer.NejiWriter;

/**
 *
 * @author david
 */
public class TestNeji {
    
    /**
     * {@link Logger} to be used in the class.
     */
    private static Logger logger = LoggerFactory.getLogger(TestNeji.class);

    public static void main(String[] args) {
Parsing parsing = Parsing.BW;
        String modelFile = "resources/models/all_bc2_bw_o2_windows_ndp.gz";
        String group = "PRGE";
        String config = "config/bc.config";
        String lexicons = "resources/lexicons/UMLS_GO/";
//        String lexicons = "resources/lexicons/UMLS_neji/";
        String normalization = "resources/lexicons/prge/";

        String fileIn = "/Volumes/data/Dropbox/corpora/test/bug.txt";
        String fileOut = "/Volumes/data/Dropbox/corpora/test/out/bug2.neji";
        
        
        MLModel[] mlmodels = new MLModel[1];
        mlmodels[0] = new MLModel(modelFile, config, parsing, group, normalization);
        
        int numThreads = 1;
        
        Context context = new Context(
                mlmodels, // Models
                lexicons, // Dictionaries folder
                GDepParser.ParserLevel.CHUNKING, // Parser Level
                true); // Use LINNAEUS
        
        try {
            context.initialize();
            
            Corpus c = new Corpus(Constants.LabelFormat.BIO, Constants.EntityType.protein);

            Pipeline p = new Pipeline();

            RawReader raw = new RawReader();
            p.addModule(raw);

//            XMLReader xml = new XMLReader(new String[]{"AbstractText", "ArticleTitle"});
//            p.addModule(xml);

            SentenceTagger stl = new SentenceTagger(new SentenceSplitter());
            p.addModule(stl);

            NLP nlp = new NLP(c, context.takeParser());
            p.addModule(nlp);

            // Dictionary matching
            for (Dictionary d : context.getDictionaries()) {
                DictionaryHybrid dtl = new DictionaryHybrid(d, c);
                p.addModule(dtl);
            }

            // Machine learning
            MLModel model = context.getModel(0);
            MLHybrid ml = new MLHybrid(c, model.take(), model.getSemanticGroup(), model.getNormalizationDictionaries());
            p.addModule(ml);

            // Writer
            NejiWriter writer = new NejiWriter(c);
            p.addModule(writer);

            p.run(new FileInputStream(fileIn), new FileOutputStream(fileOut));

            context.terminate();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        
        System.exit(0);
    }
}
