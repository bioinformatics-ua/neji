package pt.ua.tm.neji.core;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


import pt.ua.tm.gimli.corpus.Corpus;
import pt.ua.tm.gimli.exception.GimliException;

/**
 *
 * @author david
 */
public abstract class Loader extends Module {

    protected Corpus corpus;

    public Loader(Corpus corpus) throws GimliException {
        super();
        assert ( corpus != null );
        this.corpus = corpus;
    }
    
    public Corpus getCorpus(){
        return corpus;
    }
}
