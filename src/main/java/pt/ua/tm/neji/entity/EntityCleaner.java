package pt.ua.tm.neji.entity;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


import pt.ua.tm.neji.core.Tagger;
import monq.jfa.*;
import pt.ua.tm.gimli.exception.GimliException;

/**
 *
 * @author david
 */
public class EntityCleaner extends Tagger {

    private boolean inSentence;
    private boolean inEntity;
    private int startEntity;

    public EntityCleaner() throws GimliException {
        super();

        try {
            Nfa nfa = new Nfa(Nfa.NOTHING);
            nfa.or(Xml.STag("s"), start_sentence);
            nfa.or(Xml.ETag("s"), end_sentence);
            nfa.or(Xml.STag("e"), start_entity);
            nfa.or(Xml.ETag("e"), end_entity);
            this.dfa = nfa.compile(DfaRun.UNMATCHED_COPY);
        }
        catch (CompileDfaException ex) {
            throw new GimliException("There was a problem compiling the Dfa to process the document.", ex);
        }
        catch (ReSyntaxException ex) {
            throw new GimliException("There is a syntax problem with the document.", ex);
        }

        this.inSentence = false;
        this.inEntity = false;
        this.startEntity = 0;
    }
    private AbstractFaAction start_sentence = new AbstractFaAction() {

        @Override
        public void invoke(StringBuffer yytext, int start, DfaRun runner) {
            inSentence = true;
            runner.collect = true;
        }
    };
    private AbstractFaAction end_sentence = new AbstractFaAction() {

        @Override
        public void invoke(StringBuffer yytext, int start, DfaRun runner) {
            inSentence = false;
            runner.collect = false;
        }
    };
    private AbstractFaAction start_entity = new AbstractFaAction() {

        @Override
        public void invoke(StringBuffer yytext, int start, DfaRun runner) {
            if (inSentence) {
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

                int start_id = entity.indexOf("id=\"") + 4;
                int end_id = entity.indexOf("\">", start_id);
                String text = entity.substring(end_id + 2, entity.length());


                inEntity = false;
                yytext.replace(startEntity, start + 5, text);
            }
        }
    };
}
