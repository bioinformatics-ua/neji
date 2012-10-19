package pt.ua.tm.neji.core;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.ArrayList;
import java.util.List;
import monq.jfa.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.tm.gimli.exception.GimliException;

/**
 *
 * @author david
 */
public class Pipeline{

    /**
     * {@link Logger} to be used in the class.
     */
    private static Logger logger = LoggerFactory.getLogger(Pipeline.class);
    private List<Module> pipeline;

//    private List<Object> memory;
    
    public Pipeline() {
        this.pipeline = new ArrayList<Module>();
//        this.memory = new ArrayList<Object>();
    }
    
//    public void addMemoryObject(Object o){
//        memory.add(o);
//    }
//    
//    public Object getMemoryObject(int i){
//        return memory.get(i);
//    }
//    
//    public List<Object> getMemory(){
//        return memory;
//    }
//    
//    public void eraseMemory(){
//        memory = new ArrayList<Object>();
//    }
    
    public void addModule(Module pa) {
        pipeline.add(pa);
    }

    public void run(InputStream input) throws GimliException {
        run(input, null);
    }

    public void run(InputStream input, OutputStream out) throws GimliException {
        try {
            Nfa nfa = new Nfa(Nfa.NOTHING);
            Dfa dfa = nfa.compile(DfaRun.UNMATCHED_COPY);
            DfaRun previous = dfa.createRun();
            
            CharsetDecoder decoder = Charset.forName("UTF-8").newDecoder();
            ByteCharSource cs = new ByteCharSource(input);
            cs.setDecoder(decoder);
            
            previous.setIn(cs);

            for (Module a : pipeline) {
                DfaRun inside = a.getDfaRun();
                inside.setIn(previous);
                previous = inside;
            }
            if (out == null) {
                previous.filter();
            } else {
                //previous.filter(new PrintStream(out));
                PrintStream ps = new PrintStream(out, true,"UTF-8");
                previous.filter(ps);
                
                ps.close();
            }
        }
        catch (IOException ex) {
            throw new GimliException("There was a problem reading the input.", ex);
        }
        catch (CompileDfaException ex) {
            throw new GimliException("There was a problem compiling the automato.", ex);
        }
    }
}
