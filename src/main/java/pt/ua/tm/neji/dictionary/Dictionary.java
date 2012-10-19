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

    private Matcher matcher;
    private String group;

    public Dictionary(Matcher matcher, String group) {
        this.group = group;
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

}
