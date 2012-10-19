package pt.ua.tm.neji.core;

/*
 * To change this template, choose Tools | Templates and open the template in
 * the editor.
 */
import pt.ua.tm.gimli.corpus.Corpus;
import pt.ua.tm.gimli.exception.GimliException;

/**
 *
 * @author david
 */
public class Hybrid extends Tagger {

    protected Corpus corpus;

    public Hybrid(Corpus c) throws GimliException {
        super();
        assert (c != null);
        this.corpus = c;
    }

    public Corpus getCorpus() {
        return corpus;
    }
}
