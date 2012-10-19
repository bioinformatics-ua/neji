/*
 * Gimli - High-performance and multi-corpus recognition of biomedical
 * entity names
 *
 * Copyright (C) 2011 David Campos, Universidade de Aveiro, Instituto de
 * Engenharia Electrónica e Telemática de Aveiro
 *
 * Gimli is licensed under the Creative Commons
 * Attribution-NonCommercial-ShareAlike 3.0 Unported License. To view a copy of
 * this license, visit http://creativecommons.org/licenses/by-nc-sa/3.0/.
 *
 * Gimli is a free software, you are free to copy, distribute, change and
 * transmit it. However, you may not use Gimli for commercial purposes.
 *
 * Gimli is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.
 *
 */
package pt.ua.tm.neji.annotator;

import cc.mallet.fst.CRF;
import cc.mallet.fst.NoopTransducerTrainer;
import cc.mallet.fst.SumLatticeDefault;
import cc.mallet.fst.Transducer;
import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.Sequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.tm.gimli.corpus.Corpus;
import pt.ua.tm.gimli.config.Constants.LabelTag;
import pt.ua.tm.gimli.config.Constants.Parsing;
import pt.ua.tm.gimli.corpus.Sentence;
import pt.ua.tm.gimli.corpus.Token;
import pt.ua.tm.gimli.exception.GimliException;
import pt.ua.tm.gimli.model.CRFBase;
import pt.ua.tm.gimli.model.CRFModel;

/**
 * Class used to annotate any {@link Corpus} using one or several
 * {@link CRFModel} trained by Gimli.
 *
 * @author David Campos (<a
 * href="mailto:david.campos@ua.pt">david.campos@ua.pt</a>)
 * @version 1.0
 * @since 1.0
 */
public class Annotator {

    /**
     * {@link Logger} to be used in the class.
     */
    private static Logger logger = LoggerFactory.getLogger(Annotator.class);

    /**
     * The {@link Corpus} to be annotated by this {@link Annotator}.
     */
    public static void annotate(Sentence s, CRFBase modelCRF) throws GimliException {
        CRF crf = modelCRF.getCRF();

        // Check parsing direction
        boolean sentenceReversed = false;
        if (!modelCRF.getParsing().equals(s.getCorpus().getParsing())) {
            s.reverse();
            sentenceReversed = true;
        }

        // Get pipe
        crf.getInputPipe().getDataAlphabet().stopGrowth();
        Pipe pipe = crf.getInputPipe();

        // Get instance
        Instance i = new Instance(s.toExportFormat(), null, 0, null);
        i = pipe.instanceFrom(i);

        // Get predictions
        NoopTransducerTrainer crfTrainer = modelCRF.getTransducer();

        Sequence input = (Sequence) i.getData();
        Transducer tran = crfTrainer.getTransducer();
        Sequence pred = tran.transduce(input);

        // Get score
        double logScore = new SumLatticeDefault(crf, input, pred).getTotalWeight();
        double logZ = new SumLatticeDefault(crf, input).getTotalWeight();
        double prob = Math.exp(logScore - logZ);

        // Add tags
        LabelTag p;
        for (int j = 0; j < pred.size(); j++) {
            p = LabelTag.valueOf(pred.get(j).toString());
            s.getToken(j).setLabel(p);
        }
        
        // Add annotations from tags
        if (modelCRF.getParsing().equals(Parsing.FW)) {
            s.addAnnotationsFromTagsForward(prob);
        } else {
            s.addAnnotationsFromTagsBackward(prob);
        }

        // Get sentence back to its original parsing direction
        if (sentenceReversed) {
            s.reverse();
        }
    }

    public static void annotate(Corpus c, CRFBase crf) throws GimliException {
        for (Sentence s : c.getSentences()) {
            annotate(s, crf);
        }
    }
}
