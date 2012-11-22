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
package pt.ua.tm.neji.test;

import monq.ie.Term2Re;
import monq.jfa.*;
import org.apache.commons.lang.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.tm.gimli.corpus.Corpus;
import pt.ua.tm.neji.core.module.BaseLoader;
import pt.ua.tm.neji.exception.NejiException;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

/** @author david */
public class LexEBIReader extends BaseLoader {

    /** {@link Logger} to be used in the class. */
    private static Logger logger = LoggerFactory.getLogger(LexEBIReader.class);
    private Map<String, List<String>> preferred;
    private Map<String, List<String>> synonyms;
    private Map<String, String> map;
    private List<String> preferredNames, synonymsNames;
    private boolean inEntry;
    private long numEntries, numNames, numVariants, numUP;
    String prgeID;

    public long getNumEntries() {
        return numEntries;
    }

    public long getNumNames() {
        return numNames;
    }

    public long getNumVariants() {
        return numVariants;
    }

    public long getNumUP() {
        return numUP;
    }

    public Map<String, List<String>> getPreferred() {
        return preferred;
    }

    public Map<String, List<String>> getSynonyms() {
        return synonyms;
    }

    public LexEBIReader(final Corpus corpus) throws NejiException {
        super(corpus);
        this.preferred = new HashMap<String, List<String>>();
        this.synonyms = new HashMap<String, List<String>>();
        this.preferredNames = new ArrayList<String>();
        this.synonymsNames = new ArrayList<String>();

        this.inEntry = false;
        this.prgeID = "";
        numEntries = 0;
        numNames = 0;
        numVariants = 0;
        numUP = 0;
        this.map = new HashMap<>();

        try {
            Nfa nfa = new Nfa(Nfa.NOTHING);
            nfa.or(Xml.STag("Entry"), start_entry);
            nfa.or(Xml.ETag("Entry"), end_entry);

            nfa.or(Xml.EmptyElemTag("Variant"), variant);


            nfa.or(Xml.EmptyElemTag("SourceDC"), source);

            nfa.or(Xml.EmptyElemTag("DC"), species);

            setNFA(nfa, DfaRun.UNMATCHED_DROP);
        } catch (ReSyntaxException ex) {
            throw new NejiException(ex);
        }
    }

    private AbstractFaAction start_entry = new AbstractFaAction() {
        @Override
        public void invoke(StringBuffer yytext, int start, DfaRun runner) {
            inEntry = true;

            // Get preferred term
//            logger.info("PREFERRED: {}", yytext);

            map = Xml.splitElement(yytext, start);
            String preferred = map.get("baseForm");
            preferred = preferred.replaceAll("&amp;gt", "&gt;");
            preferred = StringEscapeUtils.unescapeXml(preferred);
            preferred = preferred.trim();
//            preferred = preferred.replaceAll("\\s+", "");

            if (preferred.equals("")) {
                return;
            }

            preferredNames.add(preferred);

            numEntries++;
            numNames++;

//            logger.info("PREFERRED: {}", preferred);
        }
    };
    private AbstractFaAction end_entry = new AbstractFaAction() {
        @Override
        public void invoke(StringBuffer yytext, int start, DfaRun runner) {
            inEntry = false;
            preferredNames = new ArrayList<String>();
            synonymsNames = new ArrayList<String>();
        }
    };
    private AbstractFaAction variant = new AbstractFaAction() {
        @Override
        public void invoke(StringBuffer yytext, int start, DfaRun runner) {
            // Get Variant
//            logger.info("VARIANT: {}", yytext);

            map = Xml.splitElement(yytext, start);
            String variant = map.get("writtenForm");
            variant = variant.trim();
//            variant = variant.replaceAll("\\s+", "");

            if (variant.equals("")) {
                return;
            }

            variant = variant.replaceAll("&amp;gt", "&gt;");
            variant = StringEscapeUtils.unescapeXml(variant);

            if (variant.contains("<up>")) {
                numUP++;
                return;
            }

            synonymsNames.add(variant);

            numNames++;
            numVariants++;
        }
    };
    private AbstractFaAction species = new AbstractFaAction() {
        @Override
        public void invoke(StringBuffer yytext, int start, DfaRun runner) {

            map = Xml.splitElement(yytext, start);
            String att = map.get("att");
            String id = map.get("val");

            boolean isHuman = false;
            if (att != null && id != null) {
                if (att.equals("speciesNameNCBI")
                        && id.equals("9606")) {
                    isHuman = true;
                }
            } else {
                return;
            }


            if (isHuman) {
                if (preferred.containsKey(prgeID)) {
                    preferred.get(prgeID).addAll(preferredNames);
                } else {
                    preferred.put(prgeID, preferredNames);
                }

                if (synonyms.containsKey(prgeID)) {
                    synonyms.get(prgeID).addAll(synonymsNames);
                } else {
                    synonyms.put(prgeID, synonymsNames);
                }
            }

        }
    };
    private AbstractFaAction source = new AbstractFaAction() {
        @Override
        public void invoke(StringBuffer yytext, int start, DfaRun runner) {
            map = Xml.splitElement(yytext, start);
            prgeID = map.get("sourceId");
        }
    };

    public static void main(String[] args) {

        String fileIn = "/Users/david/Downloads/geneProt70.xml.gz";
        String outPreferred = "/Volumes/data/Dropbox/PhD/work/platform/code/neji/resources/lexicons/prge/lexebi_prge_human_preferred.tsv";
        String outSynonyms = "/Volumes/data/Dropbox/PhD/work/platform/code/neji/resources/lexicons/prge/lexebi_prge_human_synonyms.tsv";
        String outPreferredRegex = "/Volumes/data/Dropbox/PhD/work/platform/code/neji/resources/lexicons/prge/lexebi_prge_human_preferred_regex.tsv";
        String outSynonymsRegex = "/Volumes/data/Dropbox/PhD/work/platform/code/neji/resources/lexicons/prge/lexebi_prge_human_synonyms_regex.tsv";

        try {

            Corpus corpus = new Corpus();

            FileOutputStream preferred = new FileOutputStream(outPreferred);
            FileOutputStream synonyms = new FileOutputStream(outSynonyms);
            FileOutputStream preferredRegex = new FileOutputStream(outPreferredRegex);
            FileOutputStream synonymsRegex = new FileOutputStream(outSynonymsRegex);
            LexEBIReader reader = new LexEBIReader(corpus);

            logger.info("Collecting dictionary data from file...");
            reader.process(new GZIPInputStream(new FileInputStream(fileIn)));

            logger.info("NUM ENTRIES: {}", reader.getNumEntries());
            logger.info("NUM NAMES: {}", reader.getNumNames());
            logger.info("NUM VARIANTS: {}", reader.getNumVariants());
            logger.info("NUM UP: {}", reader.getNumUP());


            writeToFile(reader.getPreferred(), preferred, false);
            writeToFile(reader.getSynonyms(), synonyms, false);
            writeToFile(reader.getPreferred(), preferredRegex, true);
            writeToFile(reader.getSynonyms(), synonymsRegex, true);

        } catch (IOException | NejiException ex) {
            ex.printStackTrace();
        }

    }

    private static void writeToFile(Map<String, List<String>> dict, OutputStream out, boolean useRegex)
            throws IOException {
        Iterator<String> it = dict.keySet().iterator();
        while (it.hasNext()) {
            String id = it.next();

            List<String> names = dict.get(id);

            if (useRegex) {
                names = getRegexNames(names);
            }

            String toWrite = entryToTSV(id, names);

            if (toWrite != null) {
                out.write(toWrite.getBytes());
            }
        }
        out.close();
    }

    public static List<String> getRegexNames(List<String> names) {
        List<String> regexNames = new ArrayList<String>();

        for (String name : names) {

            String regex = Term2Re.convert(name);
            regex = "(" + regex + ")";

            regex = regex.replaceAll("\\{", "\\\\{");
            regex = regex.replaceAll("\\}", "\\\\}");

            if (!regexNames.contains(regex)) {
                regexNames.add(regex);
            }
        }

        return regexNames;
    }

    private static String entryToTSV(String id, List<String> names) {
        StringBuilder sb = new StringBuilder();

        if (names.isEmpty()) {
            return null;
        }

        sb.append("UNIPROT:");
        sb.append(id);
        sb.append(":T116:PRGE");
        sb.append("\t");
        for (String name : names) {
            sb.append(name);
            sb.append("|");
        }
        sb.setLength(sb.length() - 1);


        // Temporary
//        sb.append("\t");
//        sb.append("UNIPROT:");
//        sb.append(id);

        sb.append("\n");
        return sb.toString();
    }
}
