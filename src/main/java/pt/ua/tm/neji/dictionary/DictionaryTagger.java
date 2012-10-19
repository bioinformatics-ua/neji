package pt.ua.tm.neji.dictionary;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


import pt.ua.tm.neji.core.Tagger;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import monq.jfa.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.tm.gimli.config.Constants;
import pt.ua.tm.gimli.config.Resources;
import pt.ua.tm.gimli.exception.GimliException;
import uk.ac.man.entitytagger.Mention;
import uk.ac.man.entitytagger.matching.Matcher;

/**
 *
 * @author david
 */
public class DictionaryTagger extends Tagger {

    /**
     * {@link Logger} to be used in the class.
     */
    private static Logger logger = LoggerFactory.getLogger(DictionaryTagger.class);
    private Matcher matcher;
    private int startSentence;
    private String start_e = "<e ";
    private String end_start_e = ">";
    private String end_e = "</e>";
    private String start_id = "id=\"";
    private String end_id = "\"";
    private int offset = start_e.length() + end_start_e.length()
            + end_e.length() + start_id.length() + end_id.length();

    public DictionaryTagger(Matcher matcher) throws GimliException {
        super();
        assert ( matcher != null );
        this.matcher = matcher;

        try {
            Nfa nfa = new Nfa(Nfa.NOTHING);
            nfa.or(Xml.STag("s"), start_sentence);
            nfa.or(Xml.ETag("s"), end_sentence);
            this.dfa = nfa.compile(DfaRun.UNMATCHED_COPY);
        }
        catch (CompileDfaException ex) {
            throw new GimliException("There was a problem compiling the Dfa to process the document.", ex);
        }
        catch (ReSyntaxException ex) {
            throw new GimliException("There is a syntax problem with the document.", ex);
        }
        this.startSentence = 0;
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
            StringBuffer sb = new StringBuffer(yytext.substring(startSentence, start));

            List<Mention> mentions = matcher.match(sb.toString());
            int sum_offset = 0;


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

            if (!toRemove.isEmpty() && Constants.verbose) {
                for (Mention m : toRemove) {
                    logger.info("INTERSECTION REMOVED: {}-{} {}", new String[]{
                                new Integer(m.getStart()).toString(),
                                new Integer(m.getEnd()).toString(),
                                m.getText()
                            });
                }
            }
            mentions.removeAll(toRemove);

            // Get pattern for stopwords recognition
            Pattern stopwords;
            try {
                stopwords = Resources.getStopwordsPattern();
            }
            catch (Exception ex) {
                logger.error("There was a problem loading the stopwords pattern matcher.", ex);
                return;
            }

            // Add annotations
            for (Mention m : mentions) {
                
                // Discard stopwords recognition
                java.util.regex.Matcher mather = stopwords.matcher(m.getText());
                if (mather.matches()) {
                    continue;
                }

                StringBuilder s = new StringBuilder();
                s.append(start_e);
                s.append(start_id);

                // Solve problem with IDs that contain scores
                String ids = m.getIdsToString();
                ids = ids.replaceAll("[\\\\?][\\d]+[\\\\.,][\\d]+", "");

                s.append(ids);
                //s.append(m.getIdsToString());
                s.append(end_id);
                s.append(end_start_e);
                s.append(m.getText());
                s.append(end_e);

                sb = sb.replace(sum_offset + m.getStart(), sum_offset + m.getEnd(), s.toString());

                //sum_offset += offset + m.getIdsToString().length();
                sum_offset += offset + ids.length();
            }

            // Replace sentence with species annotations
            yytext.replace(startSentence, start, sb.toString());
            runner.collect = false;
        }
    };
}
