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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.ua.tm.neji.writer;

import com.google.gson.Gson;
import monq.jfa.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.tm.gimli.corpus.AnnotationID;
import pt.ua.tm.gimli.corpus.Corpus;
import pt.ua.tm.gimli.corpus.Sentence;
import pt.ua.tm.gimli.tree.TreeNode;
import pt.ua.tm.neji.core.module.BaseWriter;
import pt.ua.tm.neji.exception.NejiException;
import pt.ua.tm.neji.writer.json.JSONEntry;
import pt.ua.tm.neji.writer.json.JSONSentence;
import pt.ua.tm.neji.writer.json.JSONTerm;

import java.util.ArrayList;
import java.util.List;

/**
 * Writer that provides information in JSON.
 * @author David Campos (<a href="mailto:david.campos@ua.pt">david.campos@ua.pt</a>)
 * @version 1.0
 * @since 1.0
 */
public class JSONWriter extends BaseWriter {

    /** {@link Logger} to be used in the class. */
    private static Logger logger = LoggerFactory.getLogger(JSONWriter.class);
    private Corpus corpus;
    private int sentenceCounter;
    private int offset;
    private List<JSONSentence> json;

    public JSONWriter(final Corpus corpus) throws NejiException {
        super();
        this.corpus = corpus;
        this.sentenceCounter = 0;
        this.offset = 0;
        this.json = new ArrayList<JSONSentence>();

        try {
            Nfa nfa = new Nfa(Nfa.NOTHING);
            nfa.or(Xml.GoofedElement("s"), end_sentence);
            setNFA(nfa, DfaRun.UNMATCHED_COPY, eof);
        } catch (ReSyntaxException ex) {
            throw new NejiException(ex);
        }
    }

    private AbstractFaAction eof = new AbstractFaAction() {
        @Override
        public void invoke(StringBuffer yytext, int start, DfaRun runner) {


//            Gson gson = new GsonBuilder().setPrettyPrinting().create();
//            String jsonText = gson.toJson(json);
            String jsonText = new Gson().toJson(json);

            yytext.replace(0, yytext.length(), jsonText);
            runner.collect = false;
        }
    };
    private AbstractFaAction end_sentence = new AbstractFaAction() {
        @Override
        public void invoke(StringBuffer yytext, int start, DfaRun runner) {

            // Get start and end of sentence
            int startSentence = yytext.indexOf("<s id=");
            int endSentence = yytext.lastIndexOf("</s>") + 4;

            int realStart = yytext.indexOf(">", startSentence) + 1;
            int realEnd = endSentence - 4;

            // Get sentence with XML tags
            String sentence = yytext.substring(realStart, realEnd);

            // Get respective sentence from corpus
            Sentence s = corpus.getSentence(sentenceCounter);

            //Remove sentence tags and escape XML
//            sentence = sentence.replaceAll("\\<.*?>", "");
            //sentence = StringEscapeUtils.escapeXml(sentence);
            yytext.replace(startSentence, endSentence, sentence);

            // Get final start and end of sentence
            int startChar = offset + yytext.indexOf(sentence);
            int endChar = startChar + sentence.length();


            // Generate sentence on stand-off format
            JSONSentence js = new JSONSentence(sentenceCounter + 1,
                    startChar, endChar - 1,
                    sentence);

            getAnnotations(s, sentence, js, startChar);

            json.add(js);

            // Remove processed input from input
            yytext.replace(0, endSentence, "");

            // Change state variables
            sentenceCounter++;

            offset = endChar;
            runner.collect = true;
        }
    };

    private void getAnnotations(Sentence s, String source, JSONSentence js, int offset) {
        getAnnotations(s.getTree().getRoot(), source, js, 0, 0, 1, offset);
    }

    private void getAnnotations(TreeNode<AnnotationID> node, String source, JSONEntry j, int level, int counter,
                                int subcounter, int offset) {

        JSONEntry je;

        if (level != 0) {
            // Add result to StringBuilder

            int id;

            if (level <= 1) {
                id = counter;
            } else {
                id = subcounter;
            }

            AnnotationID data = node.getData();
            Sentence s = data.getSentence();

            int startAnnotationInSentence = s.getToken(data.getStartIndex()).getStartSource();
            int endAnnotationInSentence = s.getToken(data.getEndIndex()).getEndSource() + 1;

            int startChar = offset + startAnnotationInSentence;
            int endChar = offset + endAnnotationInSentence;


            JSONTerm jt = new JSONTerm(id, startChar,
                    endChar, source.substring(startAnnotationInSentence, endAnnotationInSentence).trim(),
                    data.getStringIDs());
            j.addTerm(jt);

            je = jt;
        } else {
            je = j;
        }


        int i = 0;
        for (TreeNode<AnnotationID> child : node.getChildren()) {
            getAnnotations(child, source, je, level + 1, ++counter, ++i, offset);
        }
    }
}
