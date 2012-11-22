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
package pt.ua.tm.neji.normalization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.tm.gimli.corpus.*;
import pt.ua.tm.neji.dictionary.Dictionary;
import uk.ac.man.entitytagger.Mention;

import java.util.List;

/**
 * Perform normalization of chunks of text provided by machine learning.
 *
 * @author David Campos (<a href="mailto:david.campos@ua.pt">david.campos@ua.pt</a>)
 * @version 1.0
 * @since 1.0
 */
public class Normalization {

    /**
     * {@link Logger} to be used in the class.
     */
    private static Logger logger = LoggerFactory.getLogger(Normalization.class);
    private List<Dictionary> dictionaries;
    private String group;
    private String sourceText;

    public Normalization(final List<Dictionary> dictionaries, final String group, final String sourceText) {
        this.dictionaries = dictionaries;
        this.group = group;
        this.sourceText = sourceText;
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
                if (match(d, newAnnotation, group, sourceText)) {
                    matched = true;
                    break;
                }
            }

            if (matched) {
                s.addAnnotationToTree(newAnnotation);
            }
        }

        // Clean ML annotations since they are already on the bucket
        s.cleanAnnotations();
    }

    private boolean match(Dictionary dictionary, AnnotationID a, String group, String sourceText) {
        String[] ids;

        Sentence s = a.getSentence();


        int startChar = s.getToken(a.getStartIndex()).getStartSource();
        int endChar = s.getToken(a.getEndIndex()).getEndSource() + 1;

        String text = sourceText.substring(startChar, endChar);

        List<Mention> mentions = dictionary.getMatcher().match(text);

        // Alternate
        if (mentions.size() >= 1) {
            ids = mentions.get(0).getIds();
            for (String textId : ids) {
                Identifier id = Identifier.getIdentifierFromText(textId);
                id.setGroup(group);
                a.addID(id);
            }
            return true;
        }
        return false;
    }
}
