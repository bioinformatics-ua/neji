package pt.ua.tm.neji.misc;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
import pt.ua.tm.neji.core.Tagger;
import monq.jfa.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.tm.gimli.exception.GimliException;

/**
 *
 * @author david
 */
public class TagCleaner extends Tagger {

    private static Logger logger = LoggerFactory.getLogger(TagCleaner.class);
    private int startTag;
    private int startContent;
    boolean inTag;

    public TagCleaner(String tag) throws GimliException {
        super();

        try {
            Nfa nfa = new Nfa(Nfa.NOTHING);

            nfa.or(Xml.STag(tag), start_tag);
            nfa.or(Xml.ETag(tag), end_tag);

            this.dfa = nfa.compile(DfaRun.UNMATCHED_COPY);
        } catch (CompileDfaException ex) {
            throw new GimliException("There was a problem compiling the Dfa to process the document.", ex);
        } catch (ReSyntaxException ex) {
            throw new GimliException("There is a syntax problem with the document.", ex);
        }

        this.startTag = 0;
        this.startContent = 0;
    }
    private AbstractFaAction start_tag = new AbstractFaAction() {
        @Override
        public void invoke(StringBuffer yytext, int start, DfaRun runner) {
            inTag = true;
            startContent = yytext.indexOf(">", start) + 1;
            startTag = start;
            runner.collect = true;
        }
    };
    private AbstractFaAction end_tag = new AbstractFaAction() {
        @Override
        public void invoke(StringBuffer yytext, int start, DfaRun runner) {

            String content = yytext.substring(startContent, start);

            yytext.replace(startTag, yytext.indexOf(">", start) + 1, content);

            inTag = false;
            runner.collect = false;
        }
    };
}
