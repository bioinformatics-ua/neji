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
package pt.ua.tm.neji.dictionary;

import martin.common.ArgParser;
import martin.common.Loggers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.tm.gimli.config.Constants;
import pt.ua.tm.neji.exception.NejiException;
import uk.ac.man.entitytagger.EntityTagger;
import uk.ac.man.entitytagger.matching.Matcher;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Pattern;

/**
 * Helper to load dictionaries.
 *
 * @author David Campos (<a href="mailto:david.campos@ua.pt">david.campos@ua.pt</a>)
 * @version 1.0
 * @since 1.0
 */
public class DictionariesLoader {

    /**
     * {@link Logger} to be used in the class.
     */
    private static Logger logger = LoggerFactory.getLogger(DictionariesLoader.class);
    private List<String> priority;
    private List<Dictionary> dictionaries;

    public DictionariesLoader(List<String> priority) {
        assert (priority != null);
        this.dictionaries = new ArrayList<Dictionary>();
        this.priority = priority;
    }

    public DictionariesLoader(InputStream input) throws NejiException {
        assert (input != null);
        this.dictionaries = new ArrayList<Dictionary>();
        loadPriority(input);
    }

    private void loadPriority(InputStream input) throws NejiException {
        priority = new ArrayList<String>();
        BufferedReader br = new BufferedReader(new InputStreamReader(input));
        String line;

        try {
            while ((line = br.readLine()) != null) {
                if (line.equals("") || line.equals(" ") || line.equals("\n")) {
                    continue;
                }
                line = line.replace(".txt", ".tsv");
                priority.add(line);
            }
            br.close();
        } catch (IOException ex) {
            throw new NejiException("There was a problem reading the priority file.", ex);
        }
    }

    public void load(File folder, boolean ignoreCase) {
        assert (folder != null);
        Pattern groupPattern = Pattern.compile("([A-Za-z0-9]+?)\\.");

        for (String name : priority) {
            String group = null;
            java.util.regex.Matcher m = groupPattern.matcher(name);
            while (m.find()) {
                group = m.group(1);
            }
            if (group == null) {
                throw new RuntimeException(
                        "The file name of the lexicon does not follow the required format: *GROUP.*");
            }

            String dictionaryFileName = folder.getAbsolutePath() + File.separator + name;
            Matcher matcher = getExactMatcher(dictionaryFileName, ignoreCase);

            Dictionary d = new Dictionary(matcher, group);
            dictionaries.add(d);
        }
    }

    private Matcher getExactMatcher(String fileName, boolean ignoreCase) {
        Boolean b = ignoreCase;
        ArgParser ap = new ArgParser(new String[]{"--variantMatcher", fileName, "--ignoreCase", b.toString()});

        java.util.logging.Logger log = Loggers.getDefaultLogger(ap);
        if (!Constants.verbose) {
            log.setLevel(Level.SEVERE);
        }
        return EntityTagger.getMatcher(ap, log);
    }

    public List<Dictionary> getDictionaries() {
        return dictionaries;
    }
}
