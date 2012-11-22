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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import monq.jfa.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.tm.gimli.corpus.AnnotationID;
import pt.ua.tm.gimli.corpus.Corpus;
import pt.ua.tm.gimli.corpus.Identifier;
import pt.ua.tm.gimli.corpus.Sentence;
import pt.ua.tm.gimli.corpus.Token;
import pt.ua.tm.gimli.tree.Tree;
import pt.ua.tm.gimli.tree.TreeNode;
import pt.ua.tm.neji.core.module.BaseWriter;
import pt.ua.tm.neji.exception.NejiException;

/**
 * @author David Campos (<a href="mailto:david.campos@ua.pt">david.campos@ua.pt</a>)
 * @version 1.0
 * @since 1.0
 */
public class CoNLLWriter extends BaseWriter {

    /**
     * {@link Logger} to be used in the class.
     */
    private static Logger logger = LoggerFactory.getLogger(CoNLLWriter.class);
    private Corpus corpus;
    private int sentenceCounter;
    private int startSentence;

    public CoNLLWriter(final Corpus corpus) throws NejiException {
        super();
        this.corpus = corpus;
        this.sentenceCounter = 0;
        this.startSentence = 0;

        try {
            Nfa nfa = new Nfa(Nfa.NOTHING);
            nfa.or(Xml.STag("s"), start_sentence);
            nfa.or(Xml.ETag("s"), end_sentence);
            setNFA(nfa, DfaRun.UNMATCHED_DROP, eof);
        } catch (ReSyntaxException ex) {
            throw new NejiException(ex);
        }
    }
    private AbstractFaAction eof = new AbstractFaAction() {
        @Override
        public void invoke(StringBuffer yytext, int start, DfaRun runner) {
            List<TreeNode<AnnotationID>> annotationNodes;
            StringBuilder sb = new StringBuilder();

            for (Sentence s : corpus) {
                annotationNodes = s.getTree().build(Tree.TreeTraversalOrderEnum.PRE_ORDER);
                for (int i = 0; i < s.size(); i++) {
                    Token t = s.getToken(i);

                    // ID
                    sb.append(t.getIndex() + 1);
                    sb.append("\t");

                    // TEXT
                    sb.append(t.getText());
                    sb.append("\t");

                    if (t.getFeature("LEMMA") != null) {
                        sb.append(t.getFeature("LEMMA"));
                    } else {
                        sb.append("_");
                    }
                    sb.append("\t");

                    if (t.getFeature("CHUNK") != null) {
                        sb.append(t.getFeature("CHUNK"));
                    } else {
                        sb.append("_");
                    }
                    sb.append("\t");

                    if (t.getFeature("POS") != null) {
                        sb.append(t.getFeature("POS"));
                    } else {
                        sb.append("_");
                    }
                    sb.append("\t");

                    // FEATS
                    sb.append(getAnnotationsAsFeatures(s.getTree(), annotationNodes, t));
                    sb.append("\t");

                    if (t.getFeature("DEP_TOK") != null) {
                        sb.append(t.getFeature("DEP_TOK"));
                    } else {
                        sb.append("_");
                    }
                    sb.append("\t");

                    if (t.getFeature("DEP_TAG") != null) {
                        sb.append(t.getFeature("DEP_TAG"));
                    } else {
                        sb.append("_");
                    }
                    sb.append("\t");

                    // PHEAD
                    sb.append("_\t");

                    // PDEPREL
                    sb.append("_");

                    //sb.append(t.getLabel());
                    sb.append("\n");
                }
                sb.append("\n");
            }


            yytext.replace(0, yytext.length(), sb.toString());
            runner.collect = false;
        }
    };
    private AbstractFaAction start_sentence = new AbstractFaAction() {
        @Override
        public void invoke(StringBuffer yytext, int start, DfaRun runner) {
            runner.collect = true;
//            startSentence = yytext.indexOf(">", start) + 1;
            startSentence = start;
        }
    };
    private AbstractFaAction end_sentence = new AbstractFaAction() {
        @Override
        public void invoke(StringBuffer yytext, int start, DfaRun runner) {
//            yytext.replace(startSentence, start + 4, "");
        }
    };

    // Get concept annotations
    private static String getAnnotationsAsFeatures(final Tree<AnnotationID> tree,
            final List<TreeNode<AnnotationID>> annotationNodes, final Token token) {
        final Set<String> semGroups = new HashSet<>();

        for (final TreeNode<AnnotationID> node : annotationNodes) {
            // Skip the root node (whole sentence)
            if (node.equals(tree.getRoot())) {
                continue;
            }

            // Check if current node refers to our token
            if (token.getIndex() >= node.getData().getStartIndex()
                    && token.getIndex() <= node.getData().getEndIndex()) {

                for (final Identifier id : node.getData().getIDs()) {
                    semGroups.add(id.getGroup());
                }
            }
        }

        // Build Semantic Groups string (separated by ;)
        final StringBuilder sb = new StringBuilder();
        for (final String group : semGroups) {
            sb.append(group);
            sb.append(";");
        }

        if (sb.length() == 0) {
            sb.append("0");
        } else {
            sb.setLength(sb.length() - 1);
        }

        return sb.toString();
    }
}
