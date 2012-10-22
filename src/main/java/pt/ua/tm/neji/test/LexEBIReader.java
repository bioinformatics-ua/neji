/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.ua.tm.neji.test;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import monq.ie.Term2Re;
import monq.jfa.AbstractFaAction;
import monq.jfa.ByteCharSource;
import monq.jfa.CompileDfaException;
import monq.jfa.DfaRun;
import monq.jfa.Nfa;
import monq.jfa.ReSyntaxException;
import monq.jfa.Xml;
import org.apache.commons.lang.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.tm.gimli.exception.GimliException;
import pt.ua.tm.neji.core.Module;

/**
 *
 * @author david
 */
public class LexEBIReader extends Module {

    /**
     * {@link Logger} to be used in the class.
     */
    private static Logger logger = LoggerFactory.getLogger(LexEBIReader.class);
    private Map<String, List<String>> preferred;
    private Map<String, List<String>> synonyms;
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

    public LexEBIReader() throws GimliException {
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

        try {
            Nfa nfa = new Nfa(Nfa.NOTHING);
            nfa.or(Xml.STag("Entry"), start_entry);
            nfa.or(Xml.ETag("Entry"), end_entry);

            nfa.or(Xml.EmptyElemTag("Variant"), variant);


            nfa.or(Xml.EmptyElemTag("SourceDC"), source);

            nfa.or(Xml.EmptyElemTag("DC"), species);

            this.dfa = nfa.compile(DfaRun.UNMATCHED_DROP);
        } catch (CompileDfaException ex) {
            throw new GimliException("There was a problem compiling the Dfa to process the document.", ex);
        } catch (ReSyntaxException ex) {
            throw new GimliException("There is a syntax problem with the document.", ex);
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

    public void process(InputStream in) throws GimliException {
        assert (dfa != null);
        assert (in != null);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            DfaRun run = new DfaRun(dfa);
            run.setIn(new ByteCharSource(in));
            run.filter();
        } catch (IOException ex) {
            throw new GimliException("There was a problem writing the result.", ex);
        }
    }

    public static void main(String[] args) {

        String fileIn = "/Users/david/Downloads/geneProt70.xml.gz";
        String outPreferred = "/Volumes/data/Dropbox/PhD/work/platform/code/neji/resources/lexicons/prge/lexebi_prge_human_preferred.tsv";
        String outSynonyms = "/Volumes/data/Dropbox/PhD/work/platform/code/neji/resources/lexicons/prge/lexebi_prge_human_synonyms.tsv";
        String outPreferredRegex = "/Volumes/data/Dropbox/PhD/work/platform/code/neji/resources/lexicons/prge/lexebi_prge_human_preferred_regex.tsv";
        String outSynonymsRegex = "/Volumes/data/Dropbox/PhD/work/platform/code/neji/resources/lexicons/prge/lexebi_prge_human_synonyms_regex.tsv";

        try {

            FileOutputStream preferred = new FileOutputStream(outPreferred);
            FileOutputStream synonyms = new FileOutputStream(outSynonyms);
            FileOutputStream preferredRegex = new FileOutputStream(outPreferredRegex);
            FileOutputStream synonymsRegex = new FileOutputStream(outSynonymsRegex);
            LexEBIReader reader = new LexEBIReader();

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

        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (GimliException ex) {
            ex.printStackTrace();
        }

    }

    private static void writeToFile(Map<String, List<String>> dict, OutputStream out, boolean useRegex) throws IOException {
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

    private static List<String> getRegexNames(List<String> names) {
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
