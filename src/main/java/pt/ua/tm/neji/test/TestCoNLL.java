/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.ua.tm.neji.test;

import java.io.FileInputStream;
import java.io.FileOutputStream;
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
import pt.ua.tm.gimli.external.gdep.GDepParser.ParserLevel;
import pt.ua.tm.neji.core.Pipeline;
import pt.ua.tm.neji.nlp.NLP;
import pt.ua.tm.neji.reader.RawReader;
import pt.ua.tm.neji.sentence.SentenceTagger;
import pt.ua.tm.neji.sentencesplitter.SentenceSplitter;
import pt.ua.tm.neji.writer.CoNLLWriter;

/**
 *
 * @author david
 */
public class TestCoNLL {

    /**
     * {@link Logger} to be used in the class.
     */
    private static Logger logger = LoggerFactory.getLogger(TestCoNLL.class);

    public static void main(String[] args) {
        Parsing parsing = Parsing.BW;
        String modelFile = "resources/models/all_bc2_bw_o2_windows_ndp.gz";
        String group = "PRGE";
        String config = "config/bc.config";
        String lexicons = "resources/lexicons/UMLS_neji_casper/";
//        String lexicons = "resources/lexicons/UMLS_neji/";
        String normalization = "resources/lexicons/prge/";

        String fileIn = "/Volumes/data/Dropbox/corpora/test/species.txt";
        String fileOut = "/Volumes/data/Dropbox/corpora/test/out/species.conll";

        ModelConfig mc = new ModelConfig(config);


        try {
            // Load model
//            CRFModel crf = new CRFModel(mc, parsing, new GZIPInputStream(new FileInputStream(modelFile)));
//
//            // Load lexicons
//            String priorityFileName = lexicons + "_priority";
//            DictionariesLoader dl = new DictionariesLoader(new FileInputStream(priorityFileName));
//            dl.load(new File(lexicons), true);
//            List<Dictionary> dictionaries = dl.getDictionaries();
//
//            // Load species lexicon
//            String variantMatcher = lexicons + "../species/dict-species.tsv";
//            String ignoreCase = "false";
//            String ppStopTerms = lexicons + "../species/stoplist.tsv";
//            String ppAcrProbs = lexicons + "../species/synonyms-acronyms.tsv";
//            String ppSpeciesFreqs = lexicons + "../species/species-frequency.tsv";
//
//            ArgParser ap = new ArgParser(new String[]{"--variantMatcher", variantMatcher,
//                        "--ignoreCase", ignoreCase,
//                        "--ppStopTerms", ppStopTerms,
//                        "--ppAcrProbs", ppAcrProbs,
//                        "--ppSpeciesFreqs", ppSpeciesFreqs,
//                        "--postProcessing"});
//
//            java.util.logging.Logger log = Loggers.getDefaultLogger(ap);
//            if (!Constants.verbose) {
//                log.setLevel(Level.SEVERE);
//            }
//            Matcher speciesDict = EntityTagger.getMatcher(ap, log);
//            dictionaries.add(new Dictionary(speciesDict, "SPEC"));

            GDepParser parser = new GDepParser(ParserLevel.CHUNKING, false);
            parser.launch();

            SentenceDetectorME sd = new SentenceDetectorME(new SentenceModel(Resources.getResource("sentence_detector")));



            Corpus c = new Corpus(Constants.LabelFormat.BIO, Constants.EntityType.protein);

            Pipeline p = new Pipeline();

            // Change DocType of the document (DTD)
            //DTDTagger doc = new DTDTagger();
            //p.addModule(doc);

            // Sentence splitter
//            SentenceHybrid stl = new SentenceHybrid(c, sd, parser, new String[]{"AbstractText", "ArticleTitle"});
//            SentenceHybrid stl = new SentenceHybrid(c, sd, parser);
            
            RawReader raw = new RawReader();
            p.addModule(raw);
            
            SentenceTagger stl = new SentenceTagger(new SentenceSplitter());
            p.addModule(stl);

            
            NLP nlp = new NLP(c, parser);
            p.addModule(nlp);
            // Dictionary matching
//            for (Dictionary d : dictionaries) {
//                DictionaryHybrid dtl = new DictionaryHybrid(d, c);
//                p.addModule(dtl);
//            }


//            // Get normalization lexicons
//            priorityFileName = normalization + "_priority";
//            try {
//                dl = new DictionariesLoader(new FileInputStream(priorityFileName));
//            } catch (GimliException ex) {
//                logger.error("There was a problem loading the dictionaries.", ex);
//                return;
//            } catch (FileNotFoundException ex) {
//                logger.error("There was a problem reading the dictionaries.", ex);
//                return;
//            }
//
//            // Load lexicons from folder
//            dl.load(new File(normalization), false);
//            List<Dictionary> normalizationDictionaries = dl.getDictionaries();
//
//
//            // Machine learning
////            MLHybrid ml = new MLHybrid(c, crf, group);
//            MLHybrid ml = new MLHybrid(c, crf, group, normalizationDictionaries);
//            p.addModule(ml);

            // Writer
            CoNLLWriter writer = new CoNLLWriter(c);
            p.addModule(writer);

//            p.run(FileUtil.getFile(new FileInputStream(fileIn)), new FileOutputStream(fileOut));

            p.run(new FileInputStream(fileIn), new FileOutputStream(fileOut));



            parser.terminate();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
