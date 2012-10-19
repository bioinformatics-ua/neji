/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.ua.tm.neji.writer.json;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author david
 */
public class JSONEntry {

    private int id;
    private int start;
    private int end;
    private String text;
    private List<JSONTerm> terms;

    public JSONEntry(int id, int start, int end, String text) {
        this.id = id;
        this.start = start;
        this.end = end;
        this.text = text;
        this.terms = new ArrayList<JSONTerm>();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public List<JSONTerm> getTerms() {
        return terms;
    }

    public void setTerms(List<JSONTerm> terms) {
        this.terms = terms;
    }
    
    public void addTerm(JSONTerm term) {
        terms.add(term);
    }
    
}
