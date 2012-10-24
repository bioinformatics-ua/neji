package pt.ua.tm.neji.writer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import monq.jfa.*;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.tm.gimli.corpus.Corpus;
import pt.ua.tm.gimli.exception.GimliException;
import pt.ua.tm.neji.core.Tagger;

/**
 *
 * @author Tiago Nunes
 */
public class Base64Writer extends Tagger {

    /**
     * {@link Logger} to be used in the class.
     */
    private static Logger logger = LoggerFactory.getLogger(Base64Writer.class);
    private CorpusDumper dumper;

    public Base64Writer(final Corpus corpus) throws GimliException {
        super();
        this.dumper = new CorpusDumper(corpus);

        try {
            Nfa nfa = new Nfa(Nfa.NOTHING);
            this.dfa = nfa.compile(DfaRun.UNMATCHED_COPY, this.dumper.newFaAction());
        } catch (CompileDfaException ex) {
            throw new GimliException("There was a problem compiling the Dfa to process the document.", ex);
        }
    }
    
    private static final class CorpusDumper {
        private final Corpus corpus;
        
        public CorpusDumper(Corpus corpus) {
            this.corpus = corpus;
        }
        
        public AbstractFaAction newFaAction() {
            return new AbstractFaAction() {
                @Override
                public void invoke(StringBuffer yytext, int start, DfaRun runner) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ObjectOutputStream oos;
                    try {
                        oos = new ObjectOutputStream(baos);
                        oos.writeObject(CorpusDumper.this.corpus);
                        oos.close();
                    } catch (IOException ex) {
                        logger.error("Error serializing corpus object", ex);
                        throw new RuntimeException("Error serializing corpus object", ex);
                    }

                    yytext.replace(0, yytext.length(), Base64.encodeBase64String(baos.toByteArray()));
                    runner.collect = false;
                }
            };
        }
    }    
}
