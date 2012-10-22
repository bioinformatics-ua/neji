package pt.ua.tm.neji.core;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import monq.jfa.Dfa;
import monq.jfa.DfaRun;
import pt.ua.tm.gimli.exception.GimliException;

/**
 *
 * @author david
 */
public abstract class Module {

    protected Dfa dfa;
    protected Map<String, String> map;

    public Module(){
        this.map = new HashMap<String, String>();
        this.dfa = null;
    }
    
    public DfaRun getDfaRun() {
        return dfa.createRun();
    }
}
