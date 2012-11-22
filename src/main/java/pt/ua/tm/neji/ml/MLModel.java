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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.ua.tm.neji.ml;

import cc.mallet.fst.CRF;
import org.apache.commons.io.FilenameUtils;
import pt.ua.tm.gimli.config.Constants.Parsing;
import pt.ua.tm.gimli.config.ModelConfig;
import pt.ua.tm.gimli.exception.GimliException;
import pt.ua.tm.gimli.model.CRFBase;
import pt.ua.tm.gimli.model.CRFModel;
import pt.ua.tm.neji.dictionary.DictionariesLoader;
import pt.ua.tm.neji.dictionary.Dictionary;
import pt.ua.tm.neji.exception.NejiException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.zip.GZIPInputStream;

/**
 * Machine Learning model representation.
 * @author David Campos (<a href="mailto:david.campos@ua.pt">david.campos@ua.pt</a>)
 * @version 1.0
 * @since 1.0
 */
public class MLModel {

    private LinkedBlockingQueue<CRFBase> models;
    private ModelConfig config;
    private Parsing parsing;
    private String semanticGroup;
    private List<Dictionary> normalization;
    private boolean hasNormalizationDictionaries;
    private boolean readyForMultiThreading;
    private boolean isInitialized;
    // To initialize
    private String modelFile, configFile, normalizationDictionariesFolder;

    public MLModel(final String modelFile, final String configFile, final Parsing parsing,
                   final String semanticGroup) {
        this(modelFile, configFile, parsing, semanticGroup, null);
    }

    public MLModel(final File propertiesFile) {
        Properties prop = new Properties();
        try {
            prop.load(new FileReader(propertiesFile));
        } catch (IOException e) {
            throw new RuntimeException("There was a problem loading the model properties file.", e);
        }

        String folderPath = propertiesFile.getParent() + File.separator;


        this.modelFile = FilenameUtils.normalize(folderPath + prop.getProperty("file"));
        this.configFile = FilenameUtils.normalize(folderPath + prop.getProperty("config"));
        this.parsing = Parsing.valueOf(prop.getProperty("parsing"));
        this.semanticGroup = prop.getProperty("group");
        this.normalizationDictionariesFolder = FilenameUtils.normalize(folderPath + prop.getProperty("dictionaries"));
        this.models = new LinkedBlockingQueue<>();
        this.readyForMultiThreading = false;
        this.isInitialized = false;
    }

    public MLModel(final String modelFile, final String configFile, final Parsing parsing,
                   final String semanticGroup, final String normalizationDictionariesFolder) {
        this.modelFile = modelFile;
        this.configFile = configFile;
        this.parsing = parsing;
        this.semanticGroup = semanticGroup;
        this.normalizationDictionariesFolder = normalizationDictionariesFolder;
        this.models = new LinkedBlockingQueue<>();
        this.readyForMultiThreading = false;
        this.isInitialized = false;
    }

    public void initialize() throws NejiException {
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
        } catch (IOException | InterruptedException | GimliException ex) {
            throw new NejiException("There was a problem loading the model files.", ex);
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

    public CRFBase take() throws InterruptedException {
        return models.take();
    }

    public void put(final CRFBase model) throws InterruptedException {
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

    /**
     * Gets isInitialized.
     *
     * @return Value of isInitialized.
     */
    public boolean isInitialized() {
        return isInitialized;
    }
}
