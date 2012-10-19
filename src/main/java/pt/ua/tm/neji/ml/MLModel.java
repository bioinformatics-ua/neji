/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.ua.tm.neji.ml;

import cc.mallet.fst.CRF;
import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.zip.GZIPInputStream;
import pt.ua.tm.gimli.config.Constants.Parsing;
import pt.ua.tm.gimli.config.ModelConfig;
import pt.ua.tm.gimli.exception.GimliException;
import pt.ua.tm.gimli.model.CRFBase;
import pt.ua.tm.gimli.model.CRFModel;
import pt.ua.tm.neji.dictionary.DictionariesLoader;
import pt.ua.tm.neji.dictionary.Dictionary;

/**
 *
 * @author david
 */
public class MLModel {

    private LinkedBlockingQueue<CRFModel> models;
    private ModelConfig config;
    private Parsing parsing;
    private String semanticGroup;
    private List<Dictionary> normalization;
    private boolean hasNormalizationDictionaries;
    private boolean readyForMultiThreading;
    private boolean isInitialized;
    // To initialize
    private String modelFile, configFile, normalizationDictionariesFolder;

    public MLModel(String modelFile, String configFile, Parsing parsing,
            String semanticGroup) {
        this(modelFile, configFile, parsing, semanticGroup, null);
    }

    public MLModel(String modelFile, String configFile, Parsing parsing,
            String semanticGroup, String normalizationDictionariesFolder) {

        this.modelFile = modelFile;
        this.configFile = configFile;
        this.parsing = parsing;
        this.semanticGroup = semanticGroup;
        this.normalizationDictionariesFolder = normalizationDictionariesFolder;
        this.models = new LinkedBlockingQueue<CRFModel>();
        this.readyForMultiThreading = false;
        this.isInitialized = false;
    }

    public void initialize() throws GimliException {
        if (isInitialized) {
            return;
        }

        try {
            this.config = new ModelConfig(configFile);
            CRFModel model = new CRFModel(config, parsing, new GZIPInputStream(new FileInputStream(modelFile)));
            this.models.put(model);

            // Load normalization dictionaries
            this.normalization = null;
            this.hasNormalizationDictionaries = false;
            if (normalizationDictionariesFolder != null) {
                String priorityFileName = normalizationDictionariesFolder + "_priority";
                DictionariesLoader dl = new DictionariesLoader(new FileInputStream(priorityFileName));
                dl.load(new File(normalizationDictionariesFolder), false);
                this.normalization = dl.getDictionaries();
                this.hasNormalizationDictionaries = true;
            }

        } catch (Exception ex) {
            throw new GimliException("There was a problem loading the model files.", ex);
        }
        
        isInitialized = true;
    }

    public void addMultiThreadingSupport(final int numThreads) throws InterruptedException {
        if (readyForMultiThreading) {
            return;
        }

        CRF crf = models.peek().getCRF();
        for (int j = 1; j < numThreads; j++) {
            CRFModel m = new CRFModel(config, parsing);
            m.setCRF(new CRF(crf));
            models.put(m);
        }
        readyForMultiThreading = true;
    }

    public CRFModel take() throws InterruptedException {
        return models.take();
    }

    public void put(final CRFModel model) throws InterruptedException {
        models.put(model);
    }

    public boolean hasNormalizationDictionaries() {
        return hasNormalizationDictionaries;
    }

    public CRFBase getCrf() {
        return models.peek();
    }

    public ModelConfig getConfig() {
        return config;
    }

    public Parsing getParsing() {
        return parsing;
    }

    public String getSemanticGroup() {
        return semanticGroup;
    }

    public List<Dictionary> getNormalizationDictionaries() {
        return normalization;
    }
}
