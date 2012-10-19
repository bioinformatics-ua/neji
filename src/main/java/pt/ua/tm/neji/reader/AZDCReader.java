/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.ua.tm.neji.reader;

import com.sun.jndi.toolkit.ctx.Continuation;
import monq.jfa.AbstractFaAction;
import monq.jfa.CompileDfaException;
import monq.jfa.DfaRun;
import monq.jfa.Nfa;
import monq.jfa.ReSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.tm.gimli.corpus.Annotation;
import pt.ua.tm.gimli.corpus.Corpus;
import pt.ua.tm.gimli.corpus.Sentence;
import pt.ua.tm.gimli.corpus.Token;
import pt.ua.tm.gimli.exception.GimliException;
import pt.ua.tm.gimli.external.gdep.GDepCorpus;
import pt.ua.tm.gimli.external.gdep.GDepSentence;
import pt.ua.tm.gimli.external.wrapper.Parser;
import pt.ua.tm.neji.core.Loader;

/**
 *
 * @author david
 */
public class AZDCReader extends Loader {

    /**
     * {@link Logger} to be used in the class.
     */
    private static Logger logger = LoggerFactory.getLogger(AZDCReader.class);
    private int sentenceCounter;
    private int docid;
    private Sentence previousSentence;
    private Parser parser;

    public AZDCReader(Corpus corpus, Parser parser) throws GimliException {
        super(corpus);
        this.parser = parser;
        try {
            Nfa nfa = new Nfa(Nfa.NOTHING);
            nfa.or("\n", line);
            this.dfa = nfa.compile(DfaRun.UNMATCHED_COPY);
        } catch (CompileDfaException ex) {
            throw new GimliException("There was a problem compiling the Dfa to process the document.", ex);
        } catch (ReSyntaxException ex) {
            throw new GimliException("There is a syntax problem with the document.", ex);
        }

        sentenceCounter = 0;
    }
    private AbstractFaAction line = new AbstractFaAction() {
        @Override
        public void invoke(StringBuffer yytext, int start, DfaRun runner) {

            if (sentenceCounter++ == 0) { //Header
                return;
            }

            String input = yytext.toString();
            String[] parts = input.split("\t");
            String sentenceText = parts[3];

            boolean usePreviousSentence = false;
            Sentence s;

            if (Integer.parseInt(parts[0]) == docid) {
                s = previousSentence;
                usePreviousSentence = true;
            } else {
                s = new Sentence(corpus);

                try {
                    GDepSentence gs = new GDepSentence(new GDepCorpus(), parser.parse(sentenceText));
                    s = new Sentence(corpus, gs);
                    corpus.addSentence(s);
                } catch (GimliException ex) {
                    throw new RuntimeException("There was a problem parsing the sentence.", ex);
                }

            }

            if (parts[8].contains("No annotation") || parts[8].equals("*")) {
                return;
            }

            int counter = 0;
            for (String part : parts) {
                logger.info("{} - {}", counter++, part);
            }
            logger.info("");
            logger.info("");
            logger.info("");

            logger.info("SENTENCE COUNTER: {}", corpus.size());
            logger.info("YYTEXT BEFORE: {}", yytext);

            docid = Integer.parseInt(parts[0]);

            int startCharWithWhite = Integer.parseInt(parts[4]);
            int endCharWithWhite = Integer.parseInt(parts[5]);

            logger.info("START WHITE: {}, END WHITE: {}", startCharWithWhite, endCharWithWhite);

            int startChar = getCharPositionWithoutWhiteSpaces(sentenceText, startCharWithWhite - 1);
            int endChar = getCharPositionWithoutWhiteSpaces(sentenceText, endCharWithWhite - 1);

            int startToken = getTokenPosFromCharPos(s, startChar);
            int endToken = getTokenPosFromCharPos(s, endChar);

            logger.info("SENTENCE: {}", s);

            logger.info("START: {}, END: {}", startChar, endChar);
            logger.info("1st: {}, last: {}", s.getToken(startToken).getText(), s.getToken(endToken).getText());

//            s.addAnnotation(Annotation.newAnnotationByCharPositions(s, startToken, endToken, 1.0));
            s.addAnnotation(Annotation.newAnnotationByCharPositions(s, startChar, endChar, 1.0));


            for (int i = 0; i < s.size(); i++) {
                Token t = s.getToken(i);
                logger.info("{}\t{}", t.getText(), t.getLabel());
            }


            // For previous
            previousSentence = s;

            if (usePreviousSentence) {
                yytext.replace(0, start, "");
            } else {
                yytext.replace(0, start, "<s>" + sentenceText + "</s>");
            }

            logger.info("YYTEXT AFTER: {}", yytext);

        }
    };

    private int getCharPositionWithoutWhiteSpaces(String source, int pos) {
        int nonWhiteSpaceCounter = 0;
        int i;
        for (i = 0; i < source.length(); i++) {

            if (i == pos) {
                return nonWhiteSpaceCounter;
            }

            if (source.charAt(i) != ' ') {
                nonWhiteSpaceCounter++;
            }
        }

        if (i == pos) {
            return nonWhiteSpaceCounter;
        }

        return -1;
    }

    private int getTokenPosFromCharPos(Sentence s, int pos) {
        for (int i = 0; i < s.size(); i++) {
            Token t = s.getToken(i);
            if (pos >= t.getStart() && pos <= t.getEnd()) {
                return i;
            }
        }
        return -1;
    }
}
