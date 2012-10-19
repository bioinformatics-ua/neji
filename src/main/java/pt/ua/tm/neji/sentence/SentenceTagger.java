package pt.ua.tm.neji.sentence;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
import com.aliasi.util.Pair;
import java.util.List;
import pt.ua.tm.neji.core.Tagger;
import monq.jfa.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.tm.gimli.exception.GimliException;
import pt.ua.tm.neji.sentencesplitter.SentenceSplitter;

/**
 *
 * @author david
 */
public class SentenceTagger extends Tagger {

    /**
     * {@link Logger} to be used in the class.
     */
    private Logger logger = LoggerFactory.getLogger(SentenceTagger.class);
    private int startText;
    private int startTag;
    private boolean inText;
    private int sentenceCounter;
    private SentenceSplitter sentencesplitter;
    private List<Pair> sentencesPositions;
    private boolean isToProvidePositions;

    /**
     * Tag sentences parsing XML content of the specified tags.
     *
     * @param xmlTags Tags to extract content to be used.
     * @throws GimliException Problem loading the sentence tagger.
     */
    public SentenceTagger(final SentenceSplitter sentencesplitter) throws GimliException {
        super();

        this.sentencesplitter = sentencesplitter;
        this.isToProvidePositions = false;

        // Initialise XML parser
        try {
            Nfa nfa = new Nfa(Nfa.NOTHING);
            nfa.or(Xml.STag("roi"), start_text);
            nfa.or(Xml.ETag("roi"), end_text);
            this.dfa = nfa.compile(DfaRun.UNMATCHED_COPY);
        } catch (CompileDfaException ex) {
            throw new GimliException("There was a problem compiling the Dfa to process the document.", ex);
        } catch (ReSyntaxException ex) {
            throw new GimliException("There is a syntax problem with the document.", ex);
        }
        startText = 0;
        startTag = 0;
        sentenceCounter = 0;
        inText = false;
    }

    /**
     * Tag sentences parsing XML content of the specified tags.
     *
     * @param xmlTags Tags to extract content to be used.
     * @throws GimliException Problem loading the sentence tagger.
     */
    public SentenceTagger(final SentenceSplitter sentencesplitter, List<Pair> sentencesPositions) throws GimliException {
        super();

        this.sentencesplitter = sentencesplitter;
        this.sentencesPositions = sentencesPositions;
        this.isToProvidePositions = true;

        // Initialise XML parser
        try {
            Nfa nfa = new Nfa(Nfa.NOTHING);
            nfa.or(Xml.STag("roi"), start_text);
            nfa.or(Xml.ETag("roi"), end_text);
            this.dfa = nfa.compile(DfaRun.UNMATCHED_COPY);
        } catch (CompileDfaException ex) {
            throw new GimliException("There was a problem compiling the Dfa to process the document.", ex);
        } catch (ReSyntaxException ex) {
            throw new GimliException("There is a syntax problem with the document.", ex);
        }
        startText = 0;
        startTag = 0;
        sentenceCounter = 0;
        inText = false;
    }
    private AbstractFaAction start_text = new AbstractFaAction() {
        @Override
        public void invoke(StringBuffer yytext, int start, DfaRun runner) {
            inText = true;
            runner.collect = true;
            startText = yytext.indexOf(">", start) + 1;
            startTag = start;
        }
    };
    private AbstractFaAction end_text = new AbstractFaAction() {
        @Override
        public void invoke(StringBuffer yytext, int start, DfaRun runner) {
            if (inText) {
                StringBuffer sb = new StringBuffer(yytext.substring(startText, start));
                
                int[][] indices = sentencesplitter.split(sb.toString());

                int offset = 0;
                for (int i = 0; i < indices.length; i++) {
                    int s = offset + indices[i][0];
                    int e = offset + indices[i][1];

                    if (isToProvidePositions) {
                        Pair p = new Pair(indices[i][0], indices[i][1] - 1);
                        sentencesPositions.add(p);
                    }


//                    String prefix = "<s id=\"" + sentenceCounter++ + "\">";

                    String prefix = "<s";
                    prefix += " id=\"" + sentenceCounter++ + "\"";
//                    prefix += " start=\"" + (s - offset) + "\"";
//                    prefix += " end=\"" + (e - offset - 1) + "\"";
                    prefix += ">";

                    String suffix = "</s>";

                    String taggedSentence = prefix + sb.substring(s, e) + suffix;

                    sb.replace(s, e, taggedSentence);

                    offset += prefix.length() + suffix.length();
                }

                yytext.replace(startTag, yytext.indexOf(">", start) + 1, sb.toString());

                inText = false;
                runner.collect = false;
            }
        }
    };
}
