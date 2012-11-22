/*
 * Copyright (c) 2012 David Campos, University of Aveiro.
 *
 * Neji is a framework for modular biomedical concept recognition made easy, fast and accessible.
 *
 * This project is licensed under the Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported License.
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-sa/3.0/.
 *
 * This project is a free software, you are free to copy, distribute, change and transmit it. However, you may not use
 * it for commercial purposes.
 *
 * It is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */
package pt.ua.tm.neji.dictionary;

import monq.jfa.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.tm.gimli.config.Constants;
import pt.ua.tm.gimli.config.Resources;
import pt.ua.tm.neji.core.module.BaseTagger;
import pt.ua.tm.neji.exception.NejiException;
import uk.ac.man.entitytagger.Mention;
import uk.ac.man.entitytagger.matching.Matcher;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Tagger module to perform dictionary matching and provide the concepts to the stream.
 * @author David Campos (<a href="mailto:david.campos@ua.pt">david.campos@ua.pt</a>)
 * @version 1.0
 * @since 1.0
 */
public class DictionaryTagger extends BaseTagger {

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

    public DictionaryTagger(Matcher matcher) throws NejiException {
        super();
        assert (matcher != null);
        this.matcher = matcher;

        try {
            Nfa nfa = new Nfa(Nfa.NOTHING);
            nfa.or(Xml.STag("s"), start_sentence);
            nfa.or(Xml.ETag("s"), end_sentence);
            setNFA(nfa, DfaRun.UNMATCHED_COPY);
        } catch (ReSyntaxException ex) {
            throw new NejiException(ex);
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
            } catch (Exception ex) {
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
