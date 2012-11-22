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

package pt.ua.tm.neji.statistics;

import monq.jfa.*;
import org.apache.commons.lang.StringUtils;
import pt.ua.tm.gimli.corpus.AnnotationID;
import pt.ua.tm.gimli.corpus.Corpus;
import pt.ua.tm.gimli.corpus.Identifier;
import pt.ua.tm.gimli.corpus.Sentence;
import pt.ua.tm.gimli.tree.Tree;
import pt.ua.tm.gimli.tree.TreeNode;
import pt.ua.tm.neji.core.module.BaseLoader;
import pt.ua.tm.neji.exception.NejiException;

import java.util.ArrayList;
import java.util.List;

/**
 * @author David Campos (<a href="mailto:david.campos@ua.pt">david.campos@ua.pt</a>)
 * @version 1.0
 * @since 1.0
 */
public class StatisticsCollector extends BaseLoader {

    private int sentenceCounter;

    public StatisticsCollector(Corpus corpus) throws NejiException {
        super(corpus);
        this.sentenceCounter = 0;

        try {
            Nfa nfa = new Nfa(Nfa.NOTHING);
            nfa.or(Xml.GoofedElement("s"), end_sentence);
            setNFA(nfa, DfaRun.UNMATCHED_COPY);
        } catch (ReSyntaxException ex) {
            throw new NejiException(ex);
        }
    }

    private AbstractFaAction end_sentence = new AbstractFaAction() {

        @Override
        public void invoke(StringBuffer yytext, int start, DfaRun runner) {

            // Get start and end of sentence
            int startSentence = yytext.indexOf("<s id=");
            int endSentence = yytext.lastIndexOf("</s>") + 4;

            int realStart = yytext.indexOf(">", startSentence) + 1;
            int realEnd = endSentence - 4;

            // Get sentence without XML tags
            String sentenceText = yytext.substring(realStart, realEnd);

            Sentence s = getCorpus().getSentence(sentenceCounter);

            for (TreeNode<AnnotationID> node : s.getTree().build(Tree.TreeTraversalOrderEnum.PRE_ORDER)) {

                //Discard if it is root
                if (node.equals(s.getTree().getRoot())) {
                    continue;
                }
                AnnotationID a = node.getData();


                // Get annotation text from source
                int startSource = s.getToken(a.getStartIndex()).getStartSource();
                int endSource = s.getToken(a.getEndIndex()).getEndSource() + 1;
                String annotationText = sentenceText.substring(startSource, endSource).toLowerCase();

                // Discard intersection
                if (a.getType().equals(AnnotationID.AnnotationType.INTERSECTION)) {
                    StatisticsEntry se = new StatisticsEntry(annotationText, "INTERSECTED", 1);

                    OverlappingEntry oe = new OverlappingEntry(se);
                    addChildren(oe, node, sentenceText);

                    addEntry(Statistics.getInstance().getIntersected(), oe);
                    continue;
                }

                if (a.getType().equals(AnnotationID.AnnotationType.NESTED)) {
                    StatisticsEntry se = new StatisticsEntry(annotationText,
                            StringUtils.join(getUniqueGroups(a.getIDs()), "|"), 1);

                    OverlappingEntry oe = new OverlappingEntry(se);
                    addChildren(oe, node, sentenceText);

                    addEntry(Statistics.getInstance().getNested(), oe);
                }

                StatisticsEntry se;
                if (a.areIDsFromTheSameGroup()) {
                    se = new StatisticsEntry(annotationText, a.getID(0).getGroup(), 1);
                    if (a.getIDs().size() > 1) {
                        // IDs ambiguity
                        addEntry(Statistics.getInstance().getAmbiguosID(), se);
                    }
                    addEntry(Statistics.getInstance().getAnnotations(), se);
                } else {
                    // Group ambiguity

                    // Get different groups
                    List<String> groups = getUniqueGroups(a.getIDs());
                    // Add to annotations
                    se = new StatisticsEntry(annotationText, StringUtils.join(groups, "|"), 1);
                    addEntry(Statistics.getInstance().getAmbiguosGroup(), se);
                    addEntry(Statistics.getInstance().getAnnotations(), se);
                }
            }

            sentenceCounter++;
            runner.collect = false;
        }
    };

    private void addEntry(StatisticsVector v, StatisticsEntry se) {
        if (v.contains(se)) {
            se = v.get(v.indexOf(se));
            se.setOccurrences(se.getOccurrences() + 1);
        } else {
            se.setOccurrences(1);
            v.add(se);
        }

    }

    private void addEntry(OverlappingVector v, OverlappingEntry oe) {
        if (v.contains(oe)) {
            oe = v.get(v.indexOf(oe));
            oe.setOccurrences(oe.getOccurrences() + 1);
        } else {
            oe.setOccurrences(1);
            v.add(oe);
        }

    }

    private List<String> getUniqueGroups(List<Identifier> ids) {
        // Get different groups
        List<String> groups = new ArrayList<String>();
        for (Identifier id : ids) {
            if (!groups.contains(id.getGroup())) {
                groups.add(id.getGroup());
            }
        }
        return groups;
    }

    private void addChildren(OverlappingEntry oe, final TreeNode<AnnotationID> parent, final String sentenceText) {
        Sentence s = parent.getData().getSentence();

        for (TreeNode<AnnotationID> child : parent.getChildren()) {
            AnnotationID childData = child.getData();

            int startSourceChild = s.getToken(childData.getStartIndex()).getStartSource();
            int endSourceChild = s.getToken(childData.getEndIndex()).getEndSource() + 1;
            String annotationTextChild = sentenceText.substring(startSourceChild,
                    endSourceChild).toLowerCase();

            StatisticsEntry seChild = new StatisticsEntry(annotationTextChild,
                    StringUtils.join(getUniqueGroups(childData.getIDs()), "|"), 1);

            oe.addChild(seChild);
        }
    }

}
