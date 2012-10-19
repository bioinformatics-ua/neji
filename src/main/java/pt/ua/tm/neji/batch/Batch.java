/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.ua.tm.neji.batch;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.apache.commons.lang.reflect.ConstructorUtils;
import org.apache.commons.lang.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.tm.gimli.exception.GimliException;
import pt.ua.tm.gimli.util.FileUtil;
import pt.ua.tm.neji.context.Context;

/**
 *
 * @author david
 */
public class Batch {

    /**
     * {@link Logger} to be used in the class.
     */
    private static Logger logger = LoggerFactory.getLogger(Batch.class);
    private String inputFolderPath, outputFolderPath;
    private int numThreads;
    private boolean compressed;

    public Batch(final String inputFolderPath, final String outputFolderPath,
            boolean compressed, final int numThreads) {
        this.inputFolderPath = inputFolderPath;
        this.outputFolderPath = outputFolderPath;
        this.compressed = compressed;
        this.numThreads = numThreads;
    }

    public void run(Class c, Context context, Object... args) throws GimliException {

        logger.info("Loading context...");
        context.initialize();
        context.addMultiThreadingSupport(numThreads);

        // Set types
        Class[] types = new Class[args.length + 4];
        Object[] newArgs = new Object[args.length + 4];

        types[0] = InputStream.class;
        types[1] = OutputStream.class;
        types[2] = String.class;
        types[3] = Context.class;

        newArgs[3] = context;

        for (int i = 0; i < args.length; i++) {
            types[i + 4] = args[i].getClass();
            newArgs[i + 4] = args[i];
        }

        try {

            ExecutorService exec = Executors.newFixedThreadPool(numThreads);
            File inputFolder = new File(inputFolderPath);
            
            File[] ff;
            if (compressed) {
                ff = inputFolder.listFiles(new FileUtil.Filter(new String[]{"gz"}));
            } else {
                ff = inputFolder.listFiles();
            }
            
            
            File f;


            logger.info("Started processing...");

            // Start timer
            StopWatch sw = new StopWatch();
            sw.start();


            for (int i = 0; i < ff.length; i++) {
                f = ff[i];
                if (f.isDirectory()) {
                    continue;
                }
                
                if(f.getName().charAt(0) == '.') {
                    continue;
                }
                
                String name = f.getName();
                String path = f.getAbsolutePath();

                if (compressed) {
                    newArgs[0] = new GZIPInputStream(new FileInputStream(path));
                    newArgs[1] = new GZIPOutputStream(new FileOutputStream(outputFolderPath + name));
                } else {
                    newArgs[0] = new FileInputStream(path);
                    newArgs[1] = new FileOutputStream(outputFolderPath + name);
                }
                newArgs[2] = name;

                exec.execute((Runnable) ConstructorUtils.invokeExactConstructor(c, newArgs, types));
            }

            exec.shutdown();

            exec.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);

            // Terminate context
            context.terminate();

            // Stop timer
            sw.stop();
            logger.info("Finished!");
            logger.info("Files processed in: {}", sw.toString());

        } catch (Exception ex) {
            throw new GimliException("There was a problem creating the Class object based on the provided arguments and respecitve types.", ex);
        }

    }
}
