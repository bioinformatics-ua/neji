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

import martin.common.ArgParser;
import martin.common.Loggers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.tm.gimli.config.ModelConfig;
import pt.ua.tm.gimli.external.gdep.GDepParser;
import pt.ua.tm.gimli.external.gdep.GDepParser.ParserLevel;
import pt.ua.tm.gimli.external.wrapper.Parser;
import pt.ua.tm.gimli.model.CRFBase;
import pt.ua.tm.neji.dictionary.DictionariesLoader;
import pt.ua.tm.neji.dictionary.Dictionary;
import pt.ua.tm.neji.exception.NejiException;
import pt.ua.tm.neji.ml.MLModel;
import pt.ua.tm.neji.ml.MLModelsLoader;
import pt.ua.tm.neji.sentencesplitter.SentenceSplitter;
import uk.ac.man.entitytagger.EntityTagger;
import uk.ac.man.entitytagger.matching.Matcher;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Context provider that manages sentence splitters, parsers, dictionaries and ML models..
 * @author David Campos (<a href="mailto:david.campos@ua.pt">david.campos@ua.pt</a>)
 * @version 1.0
 * @since 1.0
 */
public class Context {

    /** {@link Logger} to be used in the class. */
    private static Logger logger = LoggerFactory.getLogger(Context.class);
    private LinkedBlockingQueue<Parser> parsersTS;
    private LinkedBlockingQueue<SentenceSplitter> sentencesplittersTS;
    private List<Dictionary> dictionariesTS;
    private List<MLModel> modelsTS;
    private boolean isInitialized;
    private String dictionariesFolder;
    private String modelsFolder;
    private ParserLevel parserLevel;
    private boolean useLINNAEUS, doModels, doDictionaries,
            readyForMultiThreading, setParsingLevelAutomatically;

    public Context(final String modelsFolder, final String dictionariesFolder) {
        this(modelsFolder, dictionariesFolder, false);
    }

    public Context(final String modelsFolder,
                   final String dictionariesFolder,
                   final boolean useLINNAEUS) {
        this(modelsFolder, dictionariesFolder, null, useLINNAEUS);
    }

    public Context(final String modelsFolder, final String dictionariesFolder, final ParserLevel parserLevel,
                   final boolean useLINNAEUS) {

        this.dictionariesFolder = dictionariesFolder;
        this.modelsFolder = modelsFolder;
        this.parserLevel = parserLevel;
        this.useLINNAEUS = useLINNAEUS;

        this.parsersTS = new LinkedBlockingQueue<>();
        this.dictionariesTS = new ArrayList<>();
        this.modelsTS = new ArrayList<>();
        this.sentencesplittersTS = new LinkedBlockingQueue<>();

        this.readyForMultiThreading = false;
        this.isInitialized = false;

        this.doModels = modelsFolder != null;
        this.doDictionaries = dictionariesFolder != null;
        this.setParsingLevelAutomatically = parserLevel == null;
    }

    public ContextProcessors take() throws InterruptedException {
        Parser parser = parsersTS.take();
        SentenceSplitter splitter = sentencesplittersTS.take();
        List<CRFBase> contextModels = new ArrayList<>();

        if (doModels) {
            for (MLModel model : modelsTS) {
                contextModels.add(model.take());
            }
        }

        return new ContextProcessors(parser, splitter, contextModels);
    }

    public void put(ContextProcessors contextProcessors) throws InterruptedException {
        sentencesplittersTS.put(contextProcessors.getSentenceSplitter());
        parsersTS.put(contextProcessors.getParser());

        for (int i = 0; i < modelsTS.size(); i++) {
            MLModel model = modelsTS.get(i);
            model.put(contextProcessors.getCRF(i));
        }
    }


    // Models
    public List<MLModel> getModels() {
        return modelsTS;
    }

    // Dictionaries
    public List<Dictionary> getDictionaries() {
        return dictionariesTS;
    }

    public void initialize() throws NejiException {
        if (isInitialized) {
            return;
        }


        if (doModels) {
            String priorityFileName = modelsFolder + "_priority";
            MLModelsLoader ml;
            try {
                ml = new MLModelsLoader(Files.newInputStream(Paths.get(priorityFileName)));
            } catch (IOException ex) {
                throw new NejiException("There was a problem reading the dictionaries.", ex);
            }
            ml.load(new File(modelsFolder));

            // Get models
            modelsTS = ml.getModels();

            // Initialize models
            for (MLModel model : modelsTS) {
                model.initialize();
            }

            if (setParsingLevelAutomatically) {
                this.parserLevel = getParserLevel(modelsTS);
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
            throw new NejiException("There was a problem loading the parser.", ex);
        }


        // Load dictionaries matchers
        if (doDictionaries) {
            String priorityFileName = dictionariesFolder + "_priority";
            DictionariesLoader dl;
            try {
                dl = new DictionariesLoader(new FileInputStream(priorityFileName));
            } catch (FileNotFoundException ex) {
                throw new NejiException("There was a problem reading the dictionaries.", ex);
            }
            dl.load(new File(dictionariesFolder), true);
            dictionariesTS = dl.getDictionaries();
        }


        // Load linnaeus if required
        if (useLINNAEUS) {
            ArgParser ap = new ArgParser(new String[]{""});
            ap.addProperties("resources/lexicons/species/properties.conf");
            Matcher m = EntityTagger.getMatcher(ap, Loggers.getDefaultLogger(ap));
            dictionariesTS.add(new Dictionary(m, "SPEC"));
        }

        // Initialize sentence splitters
        try {
            SentenceSplitter ss = new SentenceSplitter();
            sentencesplittersTS.put(ss);
        } catch (Exception ex) {
            throw new NejiException("There was a problem loading the Sentence Splitters.", ex);
        }

        // Set initialized
        isInitialized = true;
    }


    private ParserLevel getParserLevel(final List<MLModel> models) {
        int[] counters = new int[4];
        for (int i = 0; i < counters.length; i++) {
            counters[i] = 0;
        }

        for (MLModel model : models) {
            ModelConfig mc = model.getConfig();
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
            return ParserLevel.DEPENDENCY;
        } else if (counters[2] > 0) {
            return ParserLevel.CHUNKING;
        } else if (counters[1] > 0) {
            return ParserLevel.POS;
        } else if (counters[0] > 0) {
            return ParserLevel.LEMMATIZATION;
        } else {
            return ParserLevel.TOKENIZATION;
        }
    }

    public void addMultiThreadingSupport(final int numThreads) throws NejiException {
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
                throw new NejiException("There was a problem loading the parsers.", ex);
            }
        }

        // Models
        if (doModels) {
            for (MLModel model : modelsTS) {
                try {
                    model.addMultiThreadingSupport(numThreads);
                } catch (Exception ex) {
                    throw new NejiException("There was a problem loading the CRF models.", ex);
                }
            }
        }

        // Sentence Splitters
        for (int i = 1; i < numThreads; i++) {
            try {
                SentenceSplitter ss = new SentenceSplitter();
                sentencesplittersTS.put(ss);
            } catch (Exception ex) {
                throw new NejiException("There was a problem loading the Sentence Splitters.", ex);
            }
        }


        readyForMultiThreading = true;
    }

    public void terminate() throws NejiException {
        // Finalize parsers
        while (!parsersTS.isEmpty()) {
            try {
                parsersTS.take().terminate();
            } catch (InterruptedException ex) {
                throw new NejiException("There was a problem terminating the parsers.", ex);
            }
        }

        this.parsersTS = new LinkedBlockingQueue<>();
        this.dictionariesTS = new ArrayList<>();
        this.modelsTS = new ArrayList<>();


        System.gc();
        isInitialized = false;
        readyForMultiThreading = false;
    }
}
