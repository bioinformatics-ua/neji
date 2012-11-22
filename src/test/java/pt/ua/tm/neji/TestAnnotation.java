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
package pt.ua.tm.neji;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import pt.ua.tm.gimli.config.Constants;
import pt.ua.tm.gimli.corpus.Annotation;
import pt.ua.tm.gimli.corpus.Corpus;
import pt.ua.tm.gimli.corpus.Sentence;

/**
 *
 * @author david
 */
public class TestAnnotation extends TestCase {

    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public TestAnnotation(String testName) {
        super(testName);
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite(TestAnnotation.class);
    }

    public void testNested() {
        Corpus c = new Corpus(Constants.LabelFormat.BIO, Constants.EntityType.protein);
        Sentence s = new Sentence(c);
        Annotation a1 = Annotation.newAnnotationByTokenPositions(s, 7, 8, 1.0);
        Annotation a2 = Annotation.newAnnotationByTokenPositions(s, 8, 8, 1.0);

        assertFalse(a1.nested(a2));
        assertTrue(a2.nested(a1));

        assertFalse(a1.intersection(a2));
        assertFalse(a2.intersection(a1));

        assertFalse(a1.equals(a2));
        assertFalse(a2.equals(a1));
        
        assertTrue(a1.contains(a2));
        assertFalse(a2.contains(a1));
    }
    
    public void testIntersection(){
        Corpus c = new Corpus(Constants.LabelFormat.BIO, Constants.EntityType.protein);
        Sentence s = new Sentence(c);
        Annotation a1 = Annotation.newAnnotationByTokenPositions(s, 9, 12, 1.0);
        Annotation a2 = Annotation.newAnnotationByTokenPositions(s, 11, 13, 1.0);

        assertFalse(a1.nested(a2));
        assertFalse(a2.nested(a1));

        assertTrue(a1.intersection(a2));
        assertTrue(a2.intersection(a1));

        assertFalse(a1.equals(a2));
        assertFalse(a2.equals(a1));
        
        assertFalse(a1.contains(a2));
        assertFalse(a2.contains(a1));
    }
    
    public void testEquals(){
        Corpus c = new Corpus(Constants.LabelFormat.BIO, Constants.EntityType.protein);
        Sentence s = new Sentence(c);
        Annotation a1 = Annotation.newAnnotationByTokenPositions(s, 10, 12, 1.0);
        Annotation a2 = Annotation.newAnnotationByTokenPositions(s, 10, 12, 1.0);

        assertFalse(a1.nested(a2));
        assertFalse(a2.nested(a1));

        assertFalse(a1.intersection(a2));
        assertFalse(a2.intersection(a1));

        assertTrue(a1.equals(a2));
        assertTrue(a2.equals(a1));
        
        assertFalse(a1.contains(a2));
        assertFalse(a2.contains(a1));
    }
}
