package pt.ua.tm.neji.dictionary;

/*
 * To change this template, choose Tools | Templates and open the template in
 * the editor.
 */
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Pattern;
import martin.common.ArgParser;
import martin.common.Loggers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.tm.gimli.config.Constants;
import pt.ua.tm.gimli.exception.GimliException;
import pt.ua.tm.neji.dictionary.Dictionary.Matching;
import uk.ac.man.entitytagger.EntityTagger;
import uk.ac.man.entitytagger.matching.Matcher;
import uk.ac.man.entitytagger.matching.matchers.ACIDMatcher;

/**
 *
 * @author david
 */
public class DictionariesLoader {

    /**
     * {@link Logger} to be used in the class.
     */
    private static Logger logger = LoggerFactory.getLogger(DictionariesLoader.class);
    private List<String> priority;
    private List<Matching> matchings;
    private List<Dictionary> dictionaries;

    public DictionariesLoader(List<String> priority, List<Matching> matchings) {
        assert (priority != null);
        this.dictionaries = new ArrayList<Dictionary>();
        this.priority = priority;
        this.matchings = matchings;
    }

    public DictionariesLoader(InputStream input) throws GimliException {
        assert (input != null);
        this.dictionaries = new ArrayList<Dictionary>();
        this.matchings = new ArrayList<Matching>();
        loadPriority(input);
    }

    private void loadPriority(InputStream input) throws GimliException {
        priority = new ArrayList<String>();
        BufferedReader br = new BufferedReader(new InputStreamReader(input));
        String line;

        try {
            while ((line = br.readLine()) != null) {
                if (line.equals("") || line.equals(" ") || line.equals("\n")) {
                    continue;
                }
                line = line.replace(".txt", ".tsv");

                String[] parts = line.split("\t");

                if (parts.length != 2) {
                    throw new RuntimeException("The dictionaries priority file "
                            + "does not follow the required format: <file>\t[EXACT|REGEX]");
                }

                Matching matching = null;
                
                try {
                    matching = Matching.valueOf(parts[1]);
                } catch (Exception ex) {
                    throw new RuntimeException("The dictionaries priority file "
                            + "does not follow the required format: <file>\t[EXACT|REGEX]");
                }
                
                priority.add(parts[0]);
                matchings.add(matching);
            }
            br.close();
        } catch (IOException ex) {
            throw new GimliException("There was a problem reading the priority file.", ex);
        }

    }

    public void load(File folder, boolean ignoreCase) {
        assert (folder != null);
        Pattern groupPattern = Pattern.compile("([A-Za-z0-9]+?)\\.");

        for (int i = 0; i < priority.size(); i++) {
            String name = priority.get(i);
            Matching matching = matchings.get(i);

            String group = null;
            java.util.regex.Matcher m = groupPattern.matcher(name);
            while (m.find()) {
                group = m.group(1);
            }
            if (group == null) {
                throw new RuntimeException("The file name of the lexicon does not follow the required format: *GROUP.*");
            }

            Matcher matcher = null;
            String dictionaryFileName = folder.getAbsolutePath() + File.separator + name;
            if (matching.equals(Matching.EXACT)) {
                matcher = getExactMatcher(dictionaryFileName, ignoreCase);
            } else if (matching.equals(Matching.REGEX)) {
                matcher = getRegexMatcher(dictionaryFileName);
            }

            Dictionary d = new Dictionary(matcher, matching, group);
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

    private Matcher getRegexMatcher(String fileName) {
        HashMap<String, Pattern> patterns = ACIDMatcher.loadPatterns(new File(fileName)).getA();
        return new RegexMatcher(patterns);
    }

    public List<Dictionary> getDictionaries() {
        return dictionaries;
    }
}
