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
import pt.ua.tm.neji.core.Pipeline;
import pt.ua.tm.neji.dictionary.DictionariesLoader;
import pt.ua.tm.neji.dictionary.Dictionary;
import pt.ua.tm.neji.dictionary.DictionaryHybrid;
import pt.ua.tm.neji.ml.MLHybrid;
import pt.ua.tm.neji.reader.AZDCReader;
import pt.ua.tm.neji.writer.CoNLLWriter;

/**
 *
 * @author david
 */
public class TestReader {
    
    /**
     * {@link Logger} to be used in the class.
     */
    private static Logger logger = LoggerFactory.getLogger(TestReader.class);

    public static void main(String[] args) {
        Parsing parsing = Parsing.BW;
        String modelFile = "resources/models/all_bc2_bw_o2_windows_ndp.gz";
        String group = "PRGE";
        String config = "config/bc.config";
        String lexicons = "resources/lexicons/UMLS_neji/";

        
        
//        String fileIn = "/Users/david/Downloads/AZDC_2009_09_10.txt";
        String fileIn = "/Users/david/Downloads/AZDC2.txt";
//        String fileIn = "/Users/david/Downloads/AZDC_test.txt";
        String fileOut = "/Volumes/data/Dropbox/corpora/test/out/test2.conll";

        ModelConfig mc = new ModelConfig(config);


        try {
            // Load model
            CRFModel crf = new CRFModel(mc, parsing, new GZIPInputStream(new FileInputStream(modelFile)));

            // Load lexicons
            String priorityFileName = lexicons + "_priority";
            DictionariesLoader dl = new DictionariesLoader(new FileInputStream(priorityFileName));
            dl.load(new File(lexicons), true);
            List<Dictionary> dictionaries = dl.getDictionaries();

            GDepParser parser = new GDepParser(GDepParser.ParserLevel.CHUNKING, false);
            parser.launch();

            SentenceDetectorME sd = new SentenceDetectorME(new SentenceModel(Resources.getResource("sentence_detector")));



            Corpus c = new Corpus(Constants.LabelFormat.BIO, Constants.EntityType.protein);

            Pipeline p = new Pipeline();

            // Change DocType of the document (DTD)
//            DTDTagger doc = new DTDTagger();
//            p.addModule(doc);

            // Sentence splitter
//            SentenceHybrid stl = new SentenceHybrid(c, sd, parser, new String[]{"AbstractText", "ArticleTitle"});
//            p.addModule(stl);
            
            AZDCReader reader = new AZDCReader(c, parser);
            p.addModule(reader);

            // Dictionary matching
//            for (Dictionary d : dictionaries) {
//                DictionaryHybrid dtl = new DictionaryHybrid(d, c);
//                p.addModule(dtl);
//            }
//
//            // Machine learning
//            MLHybrid ml = new MLHybrid(c, crf, group);
//            p.addModule(ml);

            // Writer
            CoNLLWriter writer = new CoNLLWriter(c);
            p.addModule(writer);

            p.run(new FileInputStream(fileIn), new FileOutputStream(fileOut));



            parser.terminate();
            
            logger.info("Number of sentences: {}", c.size());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        
        
    }
}
