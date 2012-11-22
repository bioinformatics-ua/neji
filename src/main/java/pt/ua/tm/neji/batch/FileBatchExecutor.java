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

package pt.ua.tm.neji.batch;

import org.apache.commons.io.DirectoryWalker;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.AndFileFilter;
import org.apache.commons.io.filefilter.HiddenFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang.reflect.ConstructorUtils;
import org.apache.commons.lang.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.tm.gimli.corpus.Corpus;
import pt.ua.tm.neji.context.Context;
import pt.ua.tm.neji.core.batch.Batch;
import pt.ua.tm.neji.core.corpus.InputCorpus;
import pt.ua.tm.neji.core.corpus.InputCorpus.InputFormat;
import pt.ua.tm.neji.core.corpus.OutputCorpus;
import pt.ua.tm.neji.core.corpus.OutputCorpus.OutputFormat;
import pt.ua.tm.neji.core.processor.Processor;
import pt.ua.tm.neji.exception.NejiException;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Batch pipeline processors executor, with support for concurrent execution of multiple pipeline processors.
 *
 * @author David Campos
 * @author Tiago Nunes
 * @version 1.0
 * @since 1.0
 */
public class FileBatchExecutor implements Batch {

    private static Logger logger = LoggerFactory.getLogger(FileBatchExecutor.class);

    private String inputFolderPath, outputFolderPath, inputWildcardFilter;
    private InputFormat inputFormat;
    private OutputFormat outputFormat;
    private int numThreads;
    private boolean compressed;
    private int filesProcessed;
    private ExecutorService executor;

    private Collection<Corpus> processedCorpora;


    public FileBatchExecutor(final String inputFolderPath, final InputFormat inputFormat,
                             final String outputFolderPath, final OutputFormat outputFormat,
                             final boolean compressed, final int numThreads, final String inputWildcardFilter) {
        this.inputFolderPath = inputFolderPath;
        this.inputFormat = inputFormat;
        this.outputFolderPath = outputFolderPath;
        this.outputFormat = outputFormat;
        this.inputWildcardFilter = inputWildcardFilter;
        this.compressed = compressed;
        this.numThreads = numThreads;
        this.filesProcessed = 0;
        this.processedCorpora = new ArrayList<>();
    }

    public FileBatchExecutor(final String inputFolderPath, final InputFormat inputFormat,
                             final String outputFolderPath, final OutputFormat outputFormat,
                             final boolean compressed, final int numThreads) {
        this(inputFolderPath, inputFormat, outputFolderPath, outputFormat, compressed, numThreads, null);
    }

    @Override
    public void run(Class<Processor> processorCls, Context context, Object... args) throws NejiException {
        logger.info("Initializing context...");
        context.initialize();
        logger.info("Installing multi-threading support...");
        context.addMultiThreadingSupport(numThreads);

        try {
            logger.info("Starting thread pool with support for {} threads...", numThreads);
            executor = Executors.newFixedThreadPool(numThreads);

            StopWatch timer = new StopWatch();
            timer.start();

            CorpusDirWalker walker = new CorpusDirWalker(processorCls, context,
                    inputWildcardFilter, compressed, args);

            // Store processed corpora
            processedCorpora = walker.processFiles();

            executor.shutdown();
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
            logger.info("Stopped thread pool.");

            logger.info("Terminating context...");
            context.terminate();

            timer.stop();
            logger.info("Processed {} files in {}", filesProcessed, timer.toString());
        } catch (IOException | InterruptedException ex) {
            throw new NejiException("Problem processing pipeline.", ex);
        }
    }

    @Override
    public Collection<Corpus> getProcessedCorpora() {
        return processedCorpora;
    }

    private static Processor newProcessor(Class<Processor> processorCls, Context context,
                                          InputCorpus inputCorpus, OutputCorpus outputCorpus, Object... args)
            throws NejiException {
        Validate.notNull(processorCls);
        Validate.notNull(context);
        Validate.notNull(inputCorpus);
        Validate.notNull(outputCorpus);

        int numberArgs = 3 + (args != null ? args.length : 0);
        List<Object> values = new ArrayList<>(numberArgs);
        values.add(context);
        values.add(inputCorpus);
        values.add(outputCorpus);

        List<Class> types = new ArrayList<>(numberArgs);
        types.add(context.getClass());
        types.add(inputCorpus.getClass());
        types.add(outputCorpus.getClass());

        if (args != null) {
            for (Object arg : args) {
                values.add(arg);
                types.add(arg.getClass());
            }
        }

        try {
            return (Processor) ConstructorUtils.invokeConstructor(
                    processorCls, values.toArray(), types.toArray(new Class[0]));
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException ex) {
            logger.error("Error creating new pipeline processor.", ex);
            throw new NejiException("Error creating new pipeline processor.", ex);
        }

    }

    private static FileFilter newFileFilter(String wildcardFilter, boolean compressed) {
        List<String> wildcards = new ArrayList<String>();

        if (StringUtils.isNotBlank(wildcardFilter)) {
            wildcards.add(wildcardFilter);
        }
        if (compressed) {
            wildcards.add("*.gz");
        }
        if (wildcards.isEmpty()) {
            wildcards.add("*");
        }

        return new AndFileFilter(new WildcardFileFilter(wildcards), HiddenFileFilter.VISIBLE);
    }

    /** Walks the input corpus directory and processes files matching filters using a given pipeline processor. */
    private class CorpusDirWalker extends DirectoryWalker<Corpus> {

        private final Class<Processor> processorCls;
        private final Context context;
        private Object[] args;

        public CorpusDirWalker(Class<Processor> processorCls,
                               Context context, String inputWildcardFilter, boolean compressed, Object... args) {
            super(newFileFilter(inputWildcardFilter, compressed), 1);

            this.processorCls = processorCls;
            this.context = context;
            this.args = args;
        }

        /**
         * Walks corpus directory and processes all matched files.
         *
         * @return collection of processed Corpus.
         */
        public Collection<Corpus> processFiles() throws IOException {
            Collection<Corpus> processed = new ArrayList<Corpus>();

            walk(new File(inputFolderPath), processed);

            return processed;
        }

        /** Log walked directory name. */
        @Override
        protected boolean handleDirectory(File directory, int depth, Collection<Corpus> results) throws IOException {
            logger.info("Walking \"{}\"", directory.getAbsolutePath());
            return true;
        }

        /** Process file on pipeline. */
        @Override
        protected void handleFile(File file, int depth, Collection<Corpus> results) throws IOException {
            // Make corpus, output file
            Corpus corpus = new Corpus();

            // By default, the corpus identifier is the file name
            corpus.setIdentifier(FilenameUtils.getBaseName(file.getName()));

            File outFile = OutputCorpus.newOutputFile(
                    outputFolderPath, FilenameUtils.getBaseName(FilenameUtils.getBaseName(file.getName())),
                    outputFormat, compressed);

            // Make in/out corpus wrappers
            InputCorpus inCorpus = new InputCorpus(file, inputFormat, compressed, corpus);
            OutputCorpus outCorpus = new OutputCorpus(outFile, outputFormat, compressed, corpus);

            try {
                Processor processor = newProcessor(processorCls, context, inCorpus, outCorpus, args);

                logger.info("Processing \"{}\"...", file.getAbsolutePath());
                executor.execute(processor);

                results.add(corpus);
                filesProcessed += 1;
            } catch (NejiException ex) {
                logger.error("Error processing file \"" + file.getAbsolutePath() + "\"", ex);
                throw new RuntimeException("Error processing file \"" + file.getAbsolutePath() + "\"", ex);
            }
        }
    }
}
