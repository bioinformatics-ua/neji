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

package pt.ua.tm.neji.writer;

/*
 * To change this template, choose Tools | Templates and open the template in
 * the editor.
 */

import monq.jfa.*;
import org.apache.commons.lang.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.tm.gimli.corpus.AnnotationID;
import pt.ua.tm.gimli.corpus.Corpus;
import pt.ua.tm.gimli.corpus.Sentence;
import pt.ua.tm.gimli.corpus.Token;
import pt.ua.tm.gimli.tree.TreeNode;
import pt.ua.tm.neji.core.module.BaseWriter;
import pt.ua.tm.neji.disambiguator.Disambiguator;
import pt.ua.tm.neji.exception.NejiException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Writer that provides information following the IeXML inline format.
 * @author David Campos (<a href="mailto:david.campos@ua.pt">david.campos@ua.pt</a>)
 * @version 1.0
 * @since 1.0
 */
public class IeXMLWriter extends BaseWriter {

    /** {@link Logger} to be used in the class. */
    private static Logger logger = LoggerFactory.getLogger(IeXMLWriter.class);
    private Corpus corpus;
    private int sentenceCounter;
    private int startSentence;
    private boolean mergeNestedSameGroup;
    private boolean discardSameIdDifferentSubGroup;
    private int detail;

    public IeXMLWriter(Corpus corpus) throws NejiException {
        this(corpus, 2, false, false);
    }

    public IeXMLWriter(Corpus corpus, int detail, boolean mergeNestedSameGroup, boolean discardSameSubGroupByPriority)
            throws NejiException {
        super();

        if (detail != 1 && detail != 2) {
            throw new RuntimeException("Nested annotations detail must be 1 or 2.");
        }

        try {
            Nfa nfa = new Nfa(Nfa.NOTHING);
            nfa.or(Xml.STag("s"), start_sentence);
            nfa.or(Xml.ETag("s"), end_sentence);
            setNFA(nfa, DfaRun.UNMATCHED_COPY);
        } catch (ReSyntaxException ex) {
            throw new NejiException(ex);
        }
        this.corpus = corpus;
        this.mergeNestedSameGroup = mergeNestedSameGroup;
        this.discardSameIdDifferentSubGroup = discardSameSubGroupByPriority;
        this.detail = detail;
        this.sentenceCounter = 0;
        this.startSentence = 0;
    }

    private AbstractFaAction start_sentence = new AbstractFaAction() {
        @Override
        public void invoke(StringBuffer yytext, int start, DfaRun runner) {
            startSentence = start;
            runner.collect = true;
        }
    };
    private AbstractFaAction end_sentence = new AbstractFaAction() {

        @Override
        public void invoke(StringBuffer yytext, int start, DfaRun runner) {
            // Get sentence from document
            String sentence = yytext.substring(startSentence, start);
            int offset_id = sentence.indexOf('>') + 1;
            sentence = sentence.substring(offset_id);

            // Get respective sentence from corpus
            Sentence s = corpus.getSentence(sentenceCounter);

            // Disambiguate annotations
            Disambiguator.disambiguate(s, detail, mergeNestedSameGroup, discardSameIdDifferentSubGroup);

            // Get new sentence with annotations
            String newSentence = convertSentenceToXML(s, sentence);

            // Replace annotation in the XML document
            yytext.replace(startSentence + offset_id, start, newSentence);

            // Change state variables
            sentenceCounter++;
            runner.collect = false;
        }
    };

    private String convertSentenceToXML(final Sentence s, final String source) {
        StringBuilder sb = new StringBuilder();
        AnnotationID a;

        int countChars = 0;
        int startSource = 0;
        int endSource = 0;

        int lastEndSource = 0;

        List<TreeNode<AnnotationID>> nodes = s.getTree().build(1);

        for (TreeNode<AnnotationID> node : nodes) {

            a = node.getData();

            int startChar = s.getToken(a.getStartIndex()).getStart();
            int endChar = s.getToken(a.getEndIndex()).getEnd();

            // Get Start and End Char with Spaces
            for (int j = lastEndSource; (j < source.length()) && (countChars <= endChar); j++) {
                if (countChars == startChar) {
                    startSource = j;
                }
                if (countChars == endChar) {
                    endSource = j;
                }
                if (source.charAt(j) != ' ') {
                    countChars++;
                }
            }

            // Copiar o que vai do último End Source, até a este Start Source            
            try {
                String prev = source.substring(lastEndSource, startSource);
                if (prev.length() > 0) {
                    // Escape XML Tags
                    prev = StringEscapeUtils.escapeXml(prev);
                    sb.append(prev);
                }

                String annotation = source.substring(startSource, endSource + 1);
                boolean isMergedAnnotation = node.hasChildren();

                String ids;
                if (isMergedAnnotation) {
                    //annotation = getMergedAnnotation(s, a, annotation);
                    annotation = getMergedAnnotation(node, annotation);
                    ids = getMergedIDs(node);
                } else {
                    annotation = StringEscapeUtils.escapeXml(annotation);
                    ids = a.getStringIDs();
                }

                sb.append("<e id=\"");
                sb.append(ids);
                sb.append("\">");
                sb.append(annotation);
                sb.append("</e>");

                lastEndSource = endSource + 1;
            } catch (Exception ex) {
                logger.error("ERROR:", ex);
                logger.error("Problem writing sentence to output: {}", s.toString());
                for (TreeNode<AnnotationID> n : nodes) {
                    AnnotationID an = n.getData();
                    logger.error("{} - {}", an.getStartIndex(), an.getEndIndex());
                    logger.error("ANNOTATION: {} --- {}", an.getText(), an.getIDs());
                }
            }
        }

        if (lastEndSource < source.length()) {
            String prev = source.substring(lastEndSource, source.length());
            // Escape XML Tags
            prev = StringEscapeUtils.escapeXml(prev);
            sb.append(prev);
        }
        return sb.toString();
    }

    private String getMergedIDs(TreeNode<AnnotationID> node) {

        int start, end;
        String range;

        HashMap<String, List<String>> idsMap = new HashMap<String, List<String>>();

        AnnotationID parentAnnotation = node.getData();
        if (node.hasChildren() && !parentAnnotation.getIDs().isEmpty()) {
            start = 1;
            end = parentAnnotation.getEndIndex() - parentAnnotation.getStartIndex() + 1;
            range = getRange(start, end);

            addIDtoMap(idsMap, range, parentAnnotation.getStringIDs());
        }

        // Children
        for (TreeNode<AnnotationID> child : node.getChildren()) {
            AnnotationID childAnnotation = child.getData();

            start = childAnnotation.getStartIndex() - parentAnnotation.getStartIndex() + 1;
            end = childAnnotation.getEndIndex() - parentAnnotation.getStartIndex() + 1;
            range = getRange(start, end);

            addIDtoMap(idsMap, range, childAnnotation.getStringIDs());
        }

        // Generate text
        StringBuilder sb = new StringBuilder();

        Iterator<String> it = idsMap.keySet().iterator();
        while (it.hasNext()) {
            range = it.next();
            List<String> ids = idsMap.get(range);
            sb.append("(");
            for (String id : ids) {
                sb.append(id);
                sb.append("|");
            }
            sb.setLength(sb.length() - 1);
            sb.append(")");
            sb.append(":");
            sb.append(range);
            sb.append("|");
        }
        sb.setLength(sb.length() - 1);

        return sb.toString();
    }

    private String getRange(int start, int end) {
        StringBuilder range = new StringBuilder();
        range.append(start);
        if ((end - start) > 0) {
            range.append(",");
            range.append(end);
        }
        return range.toString();
    }

    private void addIDtoMap(HashMap<String, List<String>> map, String range, String ids) {
        if (map.containsKey(range)) {
            map.get(range).add(ids);
        } else {
            List<String> listIDs = new ArrayList<String>();
            listIDs.add(ids);
            map.put(range, listIDs);
        }
    }

    private String getMergedAnnotation(TreeNode<AnnotationID> node, String annotationSourceText) {

        StringBuilder sb = new StringBuilder();

        AnnotationID data = node.getData();
        Sentence s = data.getSentence();

        // Tokens
        int lastEndSource = 0, startChar, endChar, startSource = 0, endSource = 0, countChars = 0, wordCounter = 1;
        int startOffset = s.getToken(data.getStartIndex()).getStart();

        for (int i = data.getStartIndex(); i <= data.getEndIndex(); i++, wordCounter++) {

            Token t = s.getToken(i);

            startChar = t.getStart() - startOffset;
            endChar = t.getEnd() - startOffset;

            for (int j = lastEndSource; (j < annotationSourceText.length()) && (countChars <= endChar); j++) {
                if (countChars == startChar) {
                    startSource = j;
                }
                if (countChars == endChar) {
                    endSource = j;
                }
                if (annotationSourceText.charAt(j) != ' ') {
                    countChars++;
                } else {
                    sb.append(" ");
                }
            }

            StringBuilder word = new StringBuilder();
            word.append("<w id=\"");
            word.append(wordCounter);
            word.append("\">");
            String text = annotationSourceText.substring(startSource, endSource + 1);
            String escapeXML = StringEscapeUtils.escapeXml(text);
            word.append(escapeXML);
            word.append("</w>");
            sb.append(word);

            lastEndSource = endSource + 1;
        }

        return sb.toString();
    }
}
