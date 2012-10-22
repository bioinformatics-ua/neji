/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.ua.tm.neji.context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import martin.common.ArgParser;
import martin.common.Loggers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.tm.gimli.config.ModelConfig;
import pt.ua.tm.gimli.exception.GimliException;
import pt.ua.tm.gimli.external.gdep.GDepParser;
import pt.ua.tm.gimli.external.gdep.GDepParser.ParserLevel;
import pt.ua.tm.gimli.external.wrapper.Parser;
import pt.ua.tm.neji.dictionary.DictionariesLoader;
import pt.ua.tm.neji.dictionary.Dictionary;
import pt.ua.tm.neji.dictionary.Dictionary.Matching;
import pt.ua.tm.neji.ml.MLModel;
import pt.ua.tm.neji.sentencesplitter.SentenceSplitter;
import uk.ac.man.entitytagger.EntityTagger;
import uk.ac.man.entitytagger.matching.Matcher;

/**
 *
 * @author david
 */
public class Context {

    /**
     * {@link Logger} to be used in the class.
     */
    private static Logger logger = LoggerFactory.getLogger(Context.class);
    private MLModel[] models;
    private LinkedBlockingQueue<Parser> parsersTS;
    private LinkedBlockingQueue<SentenceSplitter> sentencesplittersTS;
    private List<Dictionary> dictionariesTS;
    private boolean isInitialized;
    private String dictionariesFolder;
    private ParserLevel parserLevel;
    private boolean useLINNAEUS, doModels, doDictionaries,
            readyForMultiThreading, setParsingLevelAutomatically;

    public Context(final MLModel[] models,
            final String dictionariesFolder,
            final boolean useLINNAEUS) {
        this(models, dictionariesFolder, null, useLINNAEUS);
    }

    public Context(final MLModel[] models,
            final String dictionariesFolder, final ParserLevel parserLevel,
            final boolean useLINNAEUS) {

        this.models = models;
        this.dictionariesFolder = dictionariesFolder;
        this.parserLevel = parserLevel;
        this.useLINNAEUS = useLINNAEUS;

        this.parsersTS = new LinkedBlockingQueue<Parser>();
        this.dictionariesTS = new ArrayList<Dictionary>();
        this.sentencesplittersTS = new LinkedBlockingQueue<SentenceSplitter>();
        this.readyForMultiThreading = false;
        this.isInitialized = false;

        this.doModels = models == null ? false : true;
        this.doDictionaries = dictionariesFolder == null ? false : true;
        this.setParsingLevelAutomatically = parserLevel == null ? true : false;
    }

    // Models
    public MLModel getModel(int i) {
        return models[i];
    }

    public int sizeModels() {
        return models.length;
    }

    // Sentence splitters
    public SentenceSplitter takeSentenceSplitter() throws InterruptedException {
        return sentencesplittersTS.take();
    }

    public void putSenteceSplitter(final SentenceSplitter ss) throws InterruptedException {
        sentencesplittersTS.put(ss);
    }

    // Parsers
    public Parser takeParser() throws InterruptedException {
        return parsersTS.take();
    }

    public void putParser(Parser parser) throws InterruptedException {
        parsersTS.put(parser);
    }

    // Dictionaries
    public Dictionary getDictionary(int i) {
        return dictionariesTS.get(i);
    }

    public int sizeDictionaries() {
        return dictionariesTS.size();
    }

    public List<Dictionary> getDictionaries() {
        return dictionariesTS;
    }

    public void initialize() throws GimliException {
        if (isInitialized) {
            return;
        }

        // Load models and Find appropriate Parsing level
        if (doModels) {
            for (int i = 0; i < models.length; i++) {
                try {
                    models[i].initialize();
                } catch (Exception ex) {
                    throw new GimliException("There was a problem loading the CRF models.", ex);
                }
            }



            if (setParsingLevelAutomatically) {
                int[] counters = new int[4];
                for (int i = 0; i < counters.length; i++) {
                    counters[i] = 0;
                }

                for (int i = 0; i < models.length; i++) {
                    ModelConfig mc = models[i].getConfig();
                    if (mc.isLemma()) {
                        counters[0]++;
                    }
                    if (mc.isPos()) {
                        counters[1]++;
                    }
                    if (mc.isChunk()) {
                        counters[2]++;
                    }
                    if (mc.isNLP()) {
                        counters[3]++;
                    }
                }

                if (counters[3] > 0) {
                    parserLevel = ParserLevel.DEPENDENCY;
                } else if (counters[2] > 0) {
                    parserLevel = ParserLevel.CHUNKING;
                } else if (counters[1] > 0) {
                    parserLevel = ParserLevel.POS;
                } else if (counters[0] > 0) {
                    parserLevel = ParserLevel.LEMMATIZATION;
                }
            }
        }

        if (parserLevel == null) {
            parserLevel = ParserLevel.TOKENIZATION;
        }

        // Initialize Parser
        try {
            // GDepParser
            GDepParser gp = new GDepParser(parserLevel, false);
            gp.launch();
            parsersTS.put(gp);
        } catch (Exception ex) {
            throw new GimliException("There was a problem loading the parser.", ex);
        }




        // Load dictionaries matchers
        if (doDictionaries) {
            String priorityFileName = dictionariesFolder + "_priority";
            DictionariesLoader dl;
            try {
                dl = new DictionariesLoader(new FileInputStream(priorityFileName));
            } catch (FileNotFoundException ex) {
                throw new GimliException("There was a problem reading the dictionaries.", ex);
            }
            dl.load(new File(dictionariesFolder), true);
            dictionariesTS = dl.getDictionaries();
        }


        // Load linnaeus if required
        if (useLINNAEUS) {
            ArgParser ap = new ArgParser(new String[]{""});
            ap.addProperties("resources/lexicons/species/properties.conf");
            Matcher m = EntityTagger.getMatcher(ap, Loggers.getDefaultLogger(ap));
            dictionariesTS.add(new Dictionary(m, Matching.EXACT, "SPEC"));
        }

        // Initilizase sentence splitters
        try {
            SentenceSplitter ss = new SentenceSplitter();
            sentencesplittersTS.put(ss);
        } catch (Exception ex) {
            throw new GimliException("There was a problem loading the Sentence Splitters.", ex);
        }

        // Set initialized
        isInitialized = true;
    }

    public void addMultiThreadingSupport(final int numThreads)
            throws GimliException {
        if (!isInitialized) {
            throw new RuntimeException("Context must be initialized before "
                    + "adding multi-threading support.");
        }
        if (readyForMultiThreading) {
            return;
        }

        // Parsers
        for (int i = 1; i < numThreads; i++) {
            try {
                // GDepParser
                GDepParser gp = new GDepParser(parserLevel, false);
                gp.launch();
                parsersTS.put(gp);
            } catch (Exception ex) {
                throw new GimliException("There was a problem loading the parsers.", ex);
            }
        }

        // Models
        if (doModels) {
            for (int i = 0; i < models.length; i++) {
                try {
                    models[i].addMultiThreadingSupport(numThreads);
                } catch (Exception ex) {
                    throw new GimliException("There was a problem loading the CRF models.", ex);
                }
            }
        }

        // Sentence Splitters
        for (int i = 1; i < numThreads; i++) {
            try {
                SentenceSplitter ss = new SentenceSplitter();
                sentencesplittersTS.put(ss);
            } catch (Exception ex) {
                throw new GimliException("There was a problem loading the Sentence Splitters.", ex);
            }
        }


        readyForMultiThreading = true;
    }

    public void terminate() throws GimliException {
        // Finalize parsers
        while (!parsersTS.isEmpty()) {
            try {
                parsersTS.take().terminate();
            } catch (InterruptedException ex) {
                throw new GimliException("There was a problem terminating the parsers.", ex);
            }
        }

        this.parsersTS = new LinkedBlockingQueue<Parser>();
        this.dictionariesTS = new ArrayList<Dictionary>();
        this.models = null;


        System.gc();
        isInitialized = false;
    }
}
