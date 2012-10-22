/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.ua.tm.neji.dictionary;

/**
 *
 * @author david
 */
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import uk.ac.man.documentparser.dataholders.Document;
import uk.ac.man.entitytagger.Mention;
import uk.ac.man.entitytagger.matching.Matcher;

public class RegexMatcher extends Matcher {

    private Map<String, Pattern> hashmap;

    public RegexMatcher(Map<String, Pattern> hashmap) {
        this.hashmap = hashmap;
    }

    @Override
    public List<Mention> match(String string) {
        List<Mention> matches = new ArrayList<Mention>();
        Iterator<String> keys = hashmap.keySet().iterator();

        while (keys.hasNext()) {
            String key = keys.next();
            java.util.regex.Matcher m = hashmap.get(key).matcher(string);

            while (m.find()) {
                Mention match = new Mention(new String[]{key}, m.start(), m.end(), string.substring(m.start(), m.end()));
                matches.add(match);
            }
        }

        return matches;
    }
    
    @Override
    public List<Mention> match(String string, Document dcmnt) {
        return match(string);
    }

    @Override
    public int size() {
        return hashmap.size();
    }

    
}
