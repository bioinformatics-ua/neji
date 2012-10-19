/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.ua.tm.neji;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import pt.ua.tm.gimli.config.Constants;
import pt.ua.tm.gimli.corpus.AnnotationID;
import pt.ua.tm.gimli.corpus.Corpus;
import pt.ua.tm.gimli.corpus.Sentence;

/**
 *
 * @author david
 */
public class TestTree extends TestCase {

    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public TestTree(String testName) {
        super(testName);
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite(TestTree.class);
    }

    public void testTree() {
        
        Corpus c = new Corpus(Constants.LabelFormat.BIO, Constants.EntityType.protein);
        Sentence s = new Sentence(c);
        
        //AnnotationID a1 = AnnotationID.newAnnotationIDByTokenPositions(s, 0, 10, 1.0);
        AnnotationID a2 = AnnotationID.newAnnotationIDByTokenPositions(s, 7, 8, 1.0);
        AnnotationID a3 = AnnotationID.newAnnotationIDByTokenPositions(s, 8, 8, 1.0);
        
        s.addAnnotationToTree(a2);
        s.addAnnotationToTree(a3);
        
        s.printAnnotationTree();
        
        assertTrue(true);
    }
    
}
