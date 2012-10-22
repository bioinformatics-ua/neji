/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.ua.tm.neji.normalization;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.tm.gimli.corpus.*;
import pt.ua.tm.neji.dictionary.Dictionary;
import pt.ua.tm.neji.dictionary.Dictionary.Matching;
import pt.ua.tm.neji.test.LexEBIReader;
import uk.ac.man.entitytagger.Mention;

/**
 *
 * @author david
 */
public class Normalization {

    /**
     * {@link Logger} to be used in the class.
     */
    private static Logger logger = LoggerFactory.getLogger(Normalization.class);
    
    private List<Dictionary> dictionaries;
    private String group;

    public Normalization(final List<Dictionary> dictionaries, final String group) {
        this.dictionaries = dictionaries;
        this.group = group;
    }

    public void normalize(Corpus c) {
        for (Sentence s : c) {
            normalize(s);
        }
    }

    public void normalize(Sentence s) {
        AnnotationID newAnnotation;

        for (Annotation a : s.getAnnotations()) {
            boolean matched = false;
            newAnnotation = AnnotationID.newAnnotationIDByTokenPositions(
                    a.getSentence(), a.getStartIndex(), a.getEndIndex(),
                    a.getScore());


            for (Dictionary d : dictionaries) {
                if (match(d, newAnnotation, group)) {
                    matched = true;
                    break;
                }
            }

//            if (!matched) {
//                String text = newAnnotation.getText();
//                text = text.replaceAll("gene", "");
//                text = text.replaceAll("genes", "");
//                text = text.replaceAll("protein", "");
//                text = text.replaceAll("proteins", "");
//                text = text.replaceAll("promoter", "");
//                text = text.replaceAll("promoters", "");
//                text = text.replaceAll("mutation", "");
//                text = text.replaceAll("mutations", "");
//                text = text.replaceAll("mRNA", "");
//                text = text.replaceAll("complex", "");
//
//                for (Dictionary d : dictionaries) {
//                    if (match(d, newAnnotation, group, text)) {
//                        matched = true;
//                        break;
//                    }
//                }
//            }

            if (matched) {
                s.addAnnotationToTree(newAnnotation);
            }
        }

        // Clean ML annotations since they are already on the bucket
        s.cleanAnnotations();
    }

    private boolean match(Dictionary dictionary, AnnotationID a, String group) {
        Mention m;
        String[] ids;
        String text = a.getText();
        text = text.replaceAll("\\s+", "");

        List<Mention> mentions = dictionary.getMatcher().match(text);
        
        
        
        if (dictionary.getMatching().equals(Matching.REGEX)) {
            logger.info("ANNOTATION: {}", a.getText());
            for (int i=0; i<mentions.size(); i++) {
                m = mentions.get(i);
                logger.info("{} - {}", m.getText(), m.getIdsToString());
            }
            logger.info("");
            return false;
        }

        if (mentions.size() == 1 && (m = mentions.get(0)).getText().equals(text)) {
            ids = m.getIds();
//            if (ids.length == 1) {
//                // Non-ambiguos
//                Identifier id = Identifier.getIdentifierFromText(ids[0]);
//                id.setGroup(group);
//                a.addID(id);
//                return true;
//            }

            for (String textId : ids) {
                Identifier id = Identifier.getIdentifierFromText(textId);
                id.setGroup(group);
                a.addID(id);
            }
            return true;
        }

        return false;
    }
    
    
    

//    private boolean match(Dictionary dictionary, AnnotationID a, String group, String text) {
//        Mention m;
//        String[] ids;
//        //String text = a.getText();
//        text = text.replaceAll("\\s+", "");
//
//        List<Mention> mentions = dictionary.getMatcher().match(text);
//
//        if (mentions.size() == 1 && (m = mentions.get(0)).getText().equals(text)) {
//            ids = m.getIds();
////            if (ids.length == 1) {
////                // Non-ambiguos
////                Identifier id = Identifier.getIdentifierFromText(ids[0]);
////                id.setGroup(group);
////                a.addID(id);
////                return true;
////            }
//
//            for (String textId : ids) {
//                Identifier id = Identifier.getIdentifierFromText(textId);
//                id.setGroup(group);
//                a.addID(id);
//            }
//            return true;
//        }
//
//        return false;
//    }
}
