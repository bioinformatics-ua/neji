/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.ua.tm.neji.global;

/**
 *
 * @author david
 */
public class Char {

    public static int getNumNonWhiteSpaceChars(String s) {
        assert (s != null);
        int count = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) != ' ') {
                count++;
            }
        }
        return count;
    }
}
