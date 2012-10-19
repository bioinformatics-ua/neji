package pt.ua.tm.neji.dictionary;

/*
 * To change this template, choose Tools | Templates and open the template in
 * the editor.
 */
import pt.ua.tm.neji.core.Loader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import monq.jfa.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.tm.gimli.config.Resources;
import pt.ua.tm.gimli.corpus.AnnotationID;
import pt.ua.tm.gimli.corpus.Corpus;
import pt.ua.tm.gimli.corpus.Identifier;
import pt.ua.tm.gimli.corpus.Sentence;
import pt.ua.tm.gimli.exception.GimliException;
import pt.ua.tm.neji.global.Char;
import uk.ac.man.entitytagger.Mention;

/**
 *
 * @author david
 */
public class DictionaryHybrid extends Loader {

    /**
     * {@link Logger} to be used in the class.
     */
    private static Logger logger = LoggerFactory.getLogger(DictionaryHybrid.class);
    private Dictionary dictionary;
    private int startSentence;
    private int sentence;

    public DictionaryHybrid(Dictionary dictionary, Corpus corpus) throws GimliException {
        super(corpus);
        assert (dictionary != null);
        this.dictionary = dictionary;

        try {
            Nfa nfa = new Nfa(Nfa.NOTHING);
            nfa.or(Xml.STag("s"), start_sentence);
            nfa.or(Xml.ETag("s"), end_sentence);
            this.dfa = nfa.compile(DfaRun.UNMATCHED_COPY);
        } catch (CompileDfaException ex) {
            throw new GimliException("There was a problem compiling the Dfa to process the document.", ex);
        } catch (ReSyntaxException ex) {
            throw new GimliException("There is a syntax problem with the document.", ex);
        }
        this.startSentence = 0;
        this.sentence = 0;
    }
    private AbstractFaAction start_sentence = new AbstractFaAction() {

        @Override
        public void invoke(StringBuffer yytext, int start, DfaRun runner) {
            runner.collect = true;
            startSentence = yytext.indexOf(">", start) + 1;
        }
    };
    private AbstractFaAction end_sentence = new AbstractFaAction() {

        @Override
        public void invoke(StringBuffer yytext, int start, DfaRun runner) {

            StringBuilder sb = new StringBuilder(yytext.substring(startSentence, start));
            String sentenceText = sb.toString();

            List<Mention> mentions = dictionary.getMatcher().match(sentenceText);


            // Filter mentions to remove intersections (does not happen frequently)
            List<Mention> toRemove = new ArrayList<Mention>();
            for (int i = 0; i < mentions.size() - 1; i++) {
                for (int j = i + 1; j < mentions.size(); j++) {
                    Mention m1 = mentions.get(i);
                    Mention m2 = mentions.get(j);
                    int size_m1 = m1.getText().length();
                    int size_m2 = m2.getText().length();

                    if (m1.getStart() >= m2.getStart() && m1.getStart() <= m2.getEnd()) {
                        if (size_m1 > size_m2) {
                            if (!toRemove.contains(m2)) {
                                toRemove.add(m2);
                            }
                        } else {
                            if (!toRemove.contains(m1)) {
                                toRemove.add(m1);
                            }
                        }
                    } else if (m1.getEnd() >= m2.getStart() && m1.getEnd() <= m2.getEnd()) {
                        if (size_m1 > size_m2) {
                            if (!toRemove.contains(m2)) {
                                toRemove.add(m2);
                            }
                        } else {
                            if (!toRemove.contains(m1)) {
                                toRemove.add(m1);
                            }
                        }
                    }
                }
            }

            mentions.removeAll(toRemove);

            // Get pattern for stopwords recognition
            Pattern stopwords;
            try {
                stopwords = Resources.getStopwordsPattern();
            } catch (Exception ex) {
                logger.error("There was a problem loading the stopwords pattern matcher.", ex);
                return;
            }

            // Add annotations
            int endLastEntity = 0;
            int previousNumChars = 0;
            for (Mention m : mentions) {

                // Discard stopwords recognition
                java.util.regex.Matcher mather = stopwords.matcher(m.getText());
                if (mather.matches()) {
                    continue;
                }

                String sentenceBeforeEntity = sentenceText.substring(endLastEntity, m.getStart());

                int numChars = Char.getNumNonWhiteSpaceChars(sentenceBeforeEntity);

                int startEntityChars = previousNumChars + numChars;
                int endEntityChars = previousNumChars + numChars + Char.getNumNonWhiteSpaceChars(m.getText()) - 1;

                String id = m.getIdsToString();
                Sentence s = corpus.getSentence(sentence);
                AnnotationID a = AnnotationID.newAnnotationIDByCharPositions(s, startEntityChars, endEntityChars, 1.0);

                if (a != null) {
                    List<Identifier> ids = Identifier.getIdentifiersFromText(id);
                    a.setIDs(ids);
                    s.addAnnotationToTree(a);
                }

                startSentence = m.getEnd() + 1;
                previousNumChars = endEntityChars + 1;
                endLastEntity = m.getEnd();
            }

            runner.collect = false;
            sentence++;
        }
    };
}
