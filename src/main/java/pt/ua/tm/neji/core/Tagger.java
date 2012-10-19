package pt.ua.tm.neji.core;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


import java.io.*;
import monq.jfa.ByteCharSource;
import monq.jfa.DfaRun;
import pt.ua.tm.gimli.exception.GimliException;

/**
 *
 * @author david
 */
public abstract class Tagger extends Module {

    public Tagger() {
        super();
    }

    public void process(InputStream in, OutputStream out) throws GimliException {
        assert ( dfa != null );
        assert ( in != null );
        assert ( out != null );

        try {
            DfaRun run = new DfaRun(dfa);
            run.setIn(new ByteCharSource(in));
            run.filter(new PrintStream(out));
        }
        catch (IOException ex) {
            throw new GimliException("There was a problem writing the result.", ex);
        }
    }

    public InputStream process(InputStream in) throws GimliException {
        assert ( dfa != null );
        assert ( in != null );

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            DfaRun run = new DfaRun(dfa);
            run.setIn(new ByteCharSource(in));
            run.filter();
        }
        catch (IOException ex) {
            throw new GimliException("There was a problem writing the result.", ex);
        }
        return new ByteArrayInputStream(out.toByteArray());
    }
}
