/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.ua.tm.neji.dictionary;

import uk.ac.man.entitytagger.matching.Matcher;

/**
 *
 * @author david
 */
public class Dictionary {

    public static enum Matching {
      REGEX,
      EXACT
    };
    
    private Matcher matcher;
    private String group;
    private Matching matching;

    public Dictionary(Matcher matcher, Matching matching, String group) {
        this.group = group;
        this.matching = matching;
        this.matcher = matcher;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public Matcher getMatcher() {
        return matcher;
    }

    public void setMatcher(Matcher matcher) {
        this.matcher = matcher;
    }

    public Matching getMatching() {
        return matching;
    }

    public void setMatching(Matching matching) {
        this.matching = matching;
    }
}
