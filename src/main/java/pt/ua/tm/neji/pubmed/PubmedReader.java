package pt.ua.tm.neji.pubmed;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


import java.io.*;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import monq.jfa.*;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.tm.gimli.config.Constants;
import pt.ua.tm.gimli.config.Constants.LabelFormat;
import pt.ua.tm.gimli.config.Resources;
import pt.ua.tm.gimli.corpus.Corpus;
import pt.ua.tm.gimli.corpus.Sentence;
import pt.ua.tm.gimli.corpus.Token;
import pt.ua.tm.gimli.exception.GimliException;
import pt.ua.tm.gimli.external.gdep.GDepCorpus;
import pt.ua.tm.gimli.external.gdep.GDepParser;
import pt.ua.tm.gimli.external.gdep.GDepSentence;
import pt.ua.tm.gimli.external.gdep.GDepToken;
import pt.ua.tm.gimli.reader.ICorpusReader;
import pt.ua.tm.gimli.util.UnclosableInputStream;

/**
 *
 * @author david
 */
public class PubmedReader implements ICorpusReader {

    /**
     * {@link Logger} to be used in the class.
     */
    private static Logger logger = LoggerFactory.getLogger(PubmedReader.class);
    private HashMap<String, String> map;
    private SentenceDetectorME sd;
    private GDepParser parser;
    private GDepCorpus gdepCorpus;
    private UnclosableInputStream inputCorpus;
    private InputStream inputGDep;
    private OutputStream outputGDep;
    private boolean launchParser;

    public PubmedReader(final InputStream inputCorpus) {
        assert ( inputCorpus != null );
        this.inputCorpus = new UnclosableInputStream(inputCorpus);
        this.inputGDep = null;
        this.outputGDep = null;
        this.parser = null;
        this.launchParser = true;
    }

    public PubmedReader(final InputStream inputCorpus, GDepParser parser) {
        this(inputCorpus);
        assert ( parser != null );
        this.parser = parser;
        this.launchParser = false;
    }

    public PubmedReader(final InputStream inputCorpus, final Object GDep) {
        this(inputCorpus);

        assert ( GDep != null );
        if (GDep instanceof InputStream) {
            inputGDep = (InputStream) GDep;
        } else if (GDep instanceof OutputStream) {
            outputGDep = (OutputStream) GDep;
        } else {
            throw new RuntimeException("GDep should be InputStream or OutputStream.");
        }
    }
    /*
     * public void setGDepParser (GDepParser parser){ logger.info("SET PARSER!
     * {}", parser); this.parser = parser; }
     */
    private AbstractFaAction get_article_title = new AbstractFaAction() {

        @Override
        public void invoke(StringBuffer yytext, int start, DfaRun runner) {
            map.clear();
            Xml.splitElement(map, yytext, start);
            String sentence = map.get(">");
            try {
                gdepCorpus.addSentence(new GDepSentence(gdepCorpus, parser.parse(sentence)));
            }
            catch (GimliException ex) {
                throw new RuntimeException("There was a problem parsing the sentence using GDep.", ex);
            }
        }
    };
    private AbstractFaAction get_abstract_text = new AbstractFaAction() {

        @Override
        public void invoke(StringBuffer yytext, int start, DfaRun runner) {
            map.clear();
            Xml.splitElement(map, yytext, start);
            String text = map.get(">");

            try {
                String[] sets = sd.sentDetect(text);
                for (String s : sets) {
                    logger.info(s);
                    gdepCorpus.addSentence(new GDepSentence(gdepCorpus, parser.parse(s)));
                }
            }
            catch (GimliException ex) {
                throw new RuntimeException("There was a problem parsing the sentence using GDep.", ex);
            }
        }
    };

    @Override
    public Corpus read(LabelFormat format) throws GimliException {
        if (inputGDep == null) {
            logger.info("Running GDep parser");
            gdepCorpus = getGDepCorpus();

            if (outputGDep != null) {
                logger.info("Saving GDep parsing result into file...");
                gdepCorpus.write(outputGDep);
            }
        } else {
            logger.info("Loading GDep parsing from file...");
            gdepCorpus.load(inputGDep);
        }

        Corpus c = loadCorpus(format, gdepCorpus);
        return c;
    }

    private Corpus loadCorpus(final LabelFormat format, final GDepCorpus gdepOutput) {
        Corpus c = new Corpus(format, Constants.EntityType.protein);

        Sentence s;
        GDepSentence gs;
        GDepToken gt;
        Token t;
        int start = 0;

        // Parser GDep Output
        for (int i = 0; i < gdepOutput.size(); i++) {
            gs = gdepOutput.getSentence(i);
            s = new Sentence(c);
            s.setId(new Integer(i).toString());
            start = 0;
            for (int k = 0; k < gs.size(); k++) {
                gt = gs.getToken(k);
                t = new Token(s, start, k, gs);
                start = t.getEnd() + 1;
                s.addToken(t);
            }
            c.addSentence(s);
        }

        return c;
    }

    @Override
    public GDepCorpus getGDepCorpus() throws GimliException {
        gdepCorpus = new GDepCorpus();
        try {
            map = new HashMap<String, String>();
            //ss = new SentenceSplitter(new GZIPInputStream(Resources.getResource("sentencesplitter")));

            sd = new SentenceDetectorME(new SentenceModel(Resources.getResource("sentence_detector")));

            if (launchParser) {
//                parser = new GDepParser(true, false);
                parser = new GDepParser(GDepParser.ParserLevel.CHUNKING, false);
                parser.launch();
            }
        }
        catch (IOException ex) {
            throw new GimliException("There was a problem lauching GDep Parser.", ex);
        }

        try {

            Nfa nfa = new Nfa(Nfa.NOTHING);
            nfa.or(Xml.GoofedElement("ArticleTitle"), get_article_title);
            nfa.or(Xml.GoofedElement("AbstractText"), get_abstract_text);
            Dfa dfa = nfa.compile(DfaRun.UNMATCHED_DROP);

            DfaRun dfaRun = new DfaRun(dfa);
            dfaRun.setIn(new ByteCharSource(inputCorpus));
            dfaRun.filter();
        }
        catch (ReSyntaxException ex) {
            throw new GimliException("There was a problem parsing the XML file.", ex);
        }
        catch (IOException ex) {
            throw new GimliException("There was a problem parsing the XML file.", ex);
        }
        catch (CompileDfaException ex) {
            throw new GimliException("There was a problem parsing the XML file.", ex);
        }

        if (launchParser) {
            parser.terminate();
        }

        return gdepCorpus;
    }

    public static void main(String[] args) {
        String fileIn = "/Users/david/Desktop/corpus/zzz/publist_1.xml.gz";
        try {
            PubmedReader reader = new PubmedReader(new GZIPInputStream(new FileInputStream(fileIn)));
            Corpus c = reader.read(LabelFormat.BIO);
            c.write(new GZIPOutputStream(new FileOutputStream("/Users/david/Desktop/corpus/corpus_tmp.gz")));
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
