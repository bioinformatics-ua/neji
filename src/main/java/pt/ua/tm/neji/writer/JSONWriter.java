/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.ua.tm.neji.writer;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;
import monq.jfa.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.tm.gimli.corpus.AnnotationID;
import pt.ua.tm.gimli.corpus.Corpus;
import pt.ua.tm.gimli.corpus.Sentence;
import pt.ua.tm.gimli.exception.GimliException;
import pt.ua.tm.gimli.tree.TreeNode;
import pt.ua.tm.neji.core.Tagger;
import pt.ua.tm.neji.global.Char;
import pt.ua.tm.neji.writer.json.JSONEntry;
import pt.ua.tm.neji.writer.json.JSONSentence;
import pt.ua.tm.neji.writer.json.JSONTerm;

/**
 *
 * @author david
 */
public class JSONWriter extends Tagger {

    /**
     * {@link Logger} to be used in the class.
     */
    private static Logger logger = LoggerFactory.getLogger(JSONWriter.class);
    private Corpus corpus;
    private int sentenceCounter;
    private int offset;
    private List<JSONSentence> json;

    public JSONWriter(final Corpus corpus) throws GimliException {
        super();
        this.corpus = corpus;
        this.sentenceCounter = 0;
        this.offset = 0;
        this.json = new ArrayList<JSONSentence>();

        try {
            Nfa nfa = new Nfa(Nfa.NOTHING);
            nfa.or(Xml.GoofedElement("s"), end_sentence);
            this.dfa = nfa.compile(DfaRun.UNMATCHED_COPY, eof);


        } catch (CompileDfaException ex) {
            throw new GimliException("There was a problem compiling the Dfa to process the document.", ex);
        } catch (ReSyntaxException ex) {
            throw new GimliException("There is a syntax problem with the document.", ex);
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
        getAnnotations(s.getAnnotationsTree().getRoot(), source, js, 0, 0, 1, offset);
    }

    private void getAnnotations(TreeNode<AnnotationID> node, String source, JSONEntry j, int level, int counter, int subcounter, int offset) {

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
