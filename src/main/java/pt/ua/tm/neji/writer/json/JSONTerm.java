/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.ua.tm.neji.writer.json;

/**
 *
 * @author david
 */
public class JSONTerm extends JSONEntry {

    private String ids;

    public JSONTerm(int id, int start, int end, String text, String ids) {
        super(id, start, end, text);
        this.ids = ids;
    }

    public String getIds() {
        return ids;
    }

    public void setIds(String ids) {
        this.ids = ids;
    }
}
