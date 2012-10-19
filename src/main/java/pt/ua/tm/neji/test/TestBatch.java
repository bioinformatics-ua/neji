/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.ua.tm.neji.test;

import pt.ua.tm.gimli.config.Constants;
import pt.ua.tm.gimli.config.Constants.Parsing;
import pt.ua.tm.gimli.external.gdep.GDepParser;
import pt.ua.tm.neji.Main;
import pt.ua.tm.neji.Main.InputFormat;
import pt.ua.tm.neji.Main.OutputFormat;
import pt.ua.tm.neji.batch.Batch;
import pt.ua.tm.neji.context.Context;
import pt.ua.tm.neji.main.Processor;
import pt.ua.tm.neji.ml.MLModel;

/**
 *
 * @author david
 */
public class TestBatch {

    public static void main(String[] args) {
        Parsing parsing = Constants.Parsing.BW;
        String modelFile = "resources/models/all_bc2_bw_o2_windows_ndp.gz";
        String group = "PRGE";
        String config = "config/bc.config";
        String lexicons = "resources/lexicons/UMLS_GO/";
        String normalization = "resources/lexicons/prge/";

        String folderIn = "/Volumes/data/Dropbox/corpora/test2/";
        String folderOut = "/Volumes/data/Dropbox/corpora/test2/out/";
        
        InputFormat inputFormat = InputFormat.XML;
        OutputFormat outputFormat = Main.OutputFormat.XML;

        int numThreads = 2;

        MLModel[] mlmodels = new MLModel[1];
        mlmodels[0] = new MLModel(modelFile, config, parsing, group, normalization);
        
        Context context = new Context(
                mlmodels, // Models
                lexicons, // Dictionaries folder
                GDepParser.ParserLevel.CHUNKING, // Parser Level
                true); // Use LINNAEUS

        
        
        try {
            context.initialize();

            Batch batch = new Batch(folderIn, folderOut, true, numThreads);
            batch.run(Processor.class, context, new Integer(2), false, false);
            
            batch.run(Processor.class, context, inputFormat, outputFormat, new String[]{"AbstractText","ArticleTitle"});
            

            context.terminate();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        
//        System.exit(0);
    }
}
