/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.ua.tm.neji.sentencesplitter;

import com.aliasi.chunk.Chunk;
import com.aliasi.chunk.Chunking;
import com.aliasi.sentences.MedlineSentenceModel;
import com.aliasi.sentences.SentenceChunker;
import com.aliasi.sentences.SentenceModel;
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;
import com.aliasi.tokenizer.TokenizerFactory;
import java.util.Iterator;
import java.util.Set;

/**
 *
 * @author david
 */
public class SentenceSplitter {

    private TokenizerFactory TOKENIZER_FACTORY;
    private SentenceModel SENTENCE_MODEL;
    private SentenceChunker SENTENCE_CHUNKER;
    
    
    
    public SentenceSplitter(){
        this.TOKENIZER_FACTORY = IndoEuropeanTokenizerFactory.INSTANCE;
        this.SENTENCE_MODEL = new MedlineSentenceModel();
        this.SENTENCE_CHUNKER = new SentenceChunker(TOKENIZER_FACTORY, SENTENCE_MODEL);
    }
    
    
    
    public int[][] split(String text) {

        Chunking chunking = SENTENCE_CHUNKER.chunk(text.toCharArray(), 0, text.length());
        Set<Chunk> sentences = chunking.chunkSet();

        int size = sentences.size();
        int[][] indices = new int[size][2];

        int i = 0;
        for (Iterator<Chunk> it = sentences.iterator(); it.hasNext();) {

            Chunk sentence = it.next();
            int start = sentence.start();
            int end = sentence.end();

            indices[i][0] = start;
            indices[i][1] = end;

            i++;
        }

        return indices;
    }
}
