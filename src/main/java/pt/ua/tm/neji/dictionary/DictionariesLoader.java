package pt.ua.tm.neji.dictionary;

/*
 * To change this template, choose Tools | Templates and open the template in
 * the editor.
 */
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Pattern;
import martin.common.ArgParser;
import martin.common.Loggers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.tm.gimli.config.Constants;
import pt.ua.tm.gimli.exception.GimliException;
import uk.ac.man.entitytagger.EntityTagger;
import uk.ac.man.entitytagger.matching.Matcher;

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
    private List<Dictionary> dictionaries;

    public DictionariesLoader(List<String> priority) {
        assert (priority != null);
        this.dictionaries = new ArrayList<Dictionary>();
        this.priority = priority;
    }

    public DictionariesLoader(InputStream input) throws GimliException {
        assert (input != null);
        this.dictionaries = new ArrayList<Dictionary>();
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
                priority.add(line);
            }
            br.close();
        } catch (IOException ex) {
            throw new GimliException("There was a problem reading the priority file.", ex);
        }

    }

    public void load(File folder, boolean ignoreCase) {
        assert (folder != null);
        Pattern groupPattern = Pattern.compile("([A-Za-z0-9]+?)\\.");
        for (String name : priority) {

//            logger.info("NAME: {}", name );
            
            String group = null;
            java.util.regex.Matcher m = groupPattern.matcher(name);
            while (m.find()) {
                group = m.group(1);
            }

            if (group == null) {
                throw new RuntimeException("The file name of the lexicon does not follow the required format: *GROUP.*");
            }

            String variantMatcher = folder.getAbsolutePath() + File.separator + name;
//            String ignoreCase = "true";

            
            Boolean b = ignoreCase;
            ArgParser ap = new ArgParser(new String[]{"--variantMatcher", variantMatcher, "--ignoreCase", b.toString()});

            java.util.logging.Logger log = Loggers.getDefaultLogger(ap);
            if (!Constants.verbose) {
                log.setLevel(Level.SEVERE);
            }
            Matcher matcher = EntityTagger.getMatcher(ap, log);

            Dictionary d = new Dictionary(matcher, group);
            dictionaries.add(d);
        }
    }

    public List<Dictionary> getDictionaries() {
        return dictionaries;
    }
}
