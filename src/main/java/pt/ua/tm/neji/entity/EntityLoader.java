package pt.ua.tm.neji.entity;

/*
 * To change this template, choose Tools | Templates and open the template in
 * the editor.
 */
import java.util.List;
import pt.ua.tm.neji.core.Loader;
import monq.jfa.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.tm.gimli.corpus.*;
import pt.ua.tm.gimli.exception.GimliException;
import pt.ua.tm.neji.global.Char;

/**
 *
 * @author david
 */
public class EntityLoader extends Loader {

    /**
     * {@link Logger} to be used in the class.
     */
    private static Logger logger = LoggerFactory.getLogger(EntityLoader.class);
    private int sentence;
    private boolean inSentence;
    private boolean inEntity;
    private int startEntity;
    private int startSentence;
    private int previousNumChars;

    public EntityLoader(Corpus corpus) throws GimliException {
        super(corpus);

        try {
            Nfa nfa = new Nfa(Nfa.NOTHING);
            nfa.or(Xml.STag("s"), start_sentence);
            nfa.or(Xml.ETag("s"), end_sentence);
            nfa.or(Xml.STag("e"), start_entity);
            nfa.or(Xml.ETag("e"), end_entity);
            this.dfa = nfa.compile(DfaRun.UNMATCHED_COPY);
        } catch (CompileDfaException ex) {
            throw new GimliException("There was a problem compiling the Dfa to process the document.", ex);
        } catch (ReSyntaxException ex) {
            throw new GimliException("There is a syntax problem with the document.", ex);
        }

        this.sentence = 0;
        this.inSentence = false;
        this.inEntity = false;
        this.startEntity = 0;
        this.startSentence = 0;
        this.previousNumChars = 0;
    }
    private AbstractFaAction start_sentence = new AbstractFaAction() {

        @Override
        public void invoke(StringBuffer yytext, int start, DfaRun runner) {
            inSentence = true;
            startSentence = yytext.indexOf(">", start) + 1;
            runner.collect = true;
            previousNumChars = 0;
        }
    };
    private AbstractFaAction end_sentence = new AbstractFaAction() {

        @Override
        public void invoke(StringBuffer yytext, int start, DfaRun runner) {
            inSentence = false;
            sentence++;
            runner.collect = false;
        }
    };
    private AbstractFaAction start_entity = new AbstractFaAction() {

        @Override
        public void invoke(StringBuffer yytext, int start, DfaRun runner) {
            if (inSentence) {
                //logger.info("ENTITY: {}", yytext);

                /*
                 * map.clear(); Xml.splitElement(map, yytext, start); String
                 * entity = map.get(">");
                 *
                 * logger.info("ENTITY: {}", entity);
                 */

                inEntity = true;
                startEntity = start;
            }
        }
    };
    private AbstractFaAction end_entity = new AbstractFaAction() {

        @Override
        public void invoke(StringBuffer yytext, int start, DfaRun runner) {
            if (inEntity) {

                String entity = yytext.substring(startEntity, start);

                //logger.info("ENTITY: {}", entity);

                int start_id = entity.indexOf("id=\"") + 4;
                int end_id = entity.indexOf("\">", start_id);
                String id = entity.substring(start_id, end_id);

                //logger.info("ID: {}", id);

                String text = entity.substring(end_id + 2, entity.length());
                //logger.info("TEXT: {}", text);

                String sentenceBeforeEntity = yytext.substring(startSentence, startEntity);
                //logger.info("BEFORE ENTITY: {}", sentenceBeforeEntity);

                int numChars = Char.getNumNonWhiteSpaceChars(sentenceBeforeEntity);

                int startEntityChars = previousNumChars + numChars;
                int endEntityChars = previousNumChars + numChars + Char.getNumNonWhiteSpaceChars(text) - 1;

                //logger.info("[{} - {}]", startEntityChars, endEntityChars);

                //String entityType = id.substring(id.lastIndexOf(":") + 1);
                //addAnnotation(corpus.getSentence(sentence), startEntityChars, endEntityChars, entityType, id);

                Sentence s = corpus.getSentence(sentence);
                AnnotationID a = AnnotationID.newAnnotationIDByCharPositions(s, startEntityChars, endEntityChars, 1.0);

                if (a != null) {
                    List<Identifier> ids = Identifier.getIdentifiersFromText(id);
                    a.setIDs(ids);
                    s.addAnnotationToTree(a);
                }

                //logger.info("");

                inEntity = false;
                startSentence = start + 4;
                previousNumChars = endEntityChars + 1;
            }
        }
    };
}
