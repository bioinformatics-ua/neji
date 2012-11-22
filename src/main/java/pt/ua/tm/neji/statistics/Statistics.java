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

package pt.ua.tm.neji.statistics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David Campos (<a href="mailto:david.campos@ua.pt">david.campos@ua.pt</a>)
 * @version 1.0
 * @since 1.0
 */
public class Statistics {
    /** {@link org.slf4j.Logger} to be used in the class. */
    private static Logger logger = LoggerFactory.getLogger(Statistics.class);
    private StatisticsVector ambiguosGroup;
    private StatisticsVector ambiguosID;
    private StatisticsVector annotations;

    private OverlappingVector nested;
    private OverlappingVector intersected;

    private static Statistics instance;

    private Statistics() {
        this.ambiguosGroup = new StatisticsVector();
        this.ambiguosID = new StatisticsVector();
        this.annotations = new StatisticsVector();

        this.nested = new OverlappingVector();
        this.intersected = new OverlappingVector();
    }

    public synchronized static Statistics getInstance() {
        if (instance == null) {
            instance = new Statistics();
        }
        return instance;
    }

    /**
     * Gets annotations.
     *
     * @return Value of annotations.
     */
    public synchronized StatisticsVector getAnnotations() {
        return annotations;
    }

    /**
     * Gets ambiguosID.
     *
     * @return Value of ambiguosID.
     */
    public synchronized StatisticsVector getAmbiguosID() {
        return ambiguosID;
    }

    /**
     * Gets ambiguosGroup.
     *
     * @return Value of ambiguosGroup.
     */
    public synchronized StatisticsVector getAmbiguosGroup() {
        return ambiguosGroup;
    }

    /**
     * Gets intersected.
     *
     * @return Value of intersected.
     */
    public OverlappingVector getIntersected() {
        return intersected;
    }

    /**
     * Gets nested.
     *
     * @return Value of nested.
     */
    public OverlappingVector getNested() {
        return nested;
    }


    public void print() {
        logger.info("ANNOTATIONS");
        logger.info("\tNum. Unique: {}", annotations.getUnique());
        logger.info("\tNum. Occurrences: {}", annotations.getOccurrences());
        logger.info("");

        logger.info("AMBIGUOS GROUP");
        logger.info("\tNum. Unique: {}", ambiguosGroup.getUnique());
        logger.info("\tNum. Occurrences: {}", ambiguosGroup.getOccurrences());
        double p = ((double) ambiguosGroup.getOccurrences() / (double) annotations.getOccurrences()) * 100.0;
        logger.info("Percentage: {}%", p);
        logger.info("");

        logger.info("AMBIGUOS ID");
        logger.info("\tNum. Unique: {}", ambiguosID.getUnique());
        logger.info("\tNum. Occurrences: {}", ambiguosID.getOccurrences());
        p = ((double) ambiguosID.getOccurrences() / (double) annotations.getOccurrences()) * 100.0;
        logger.info("Percentage: {}%", p);
        logger.info("");

        printPerGroup("DISO");
        printPerGroup("PRGE");
        printPerGroup("CHED");
        printPerGroup("FUNC");
        printPerGroup("PROC");


        annotations.sort();
        logger.info("TOP 10 ANNOTATIONS");
        for (int i = 0; i < 10; i++) {
            StatisticsEntry se = annotations.get(i);
            logger.info("{}:\t{} ({}) - {}", new Object[]{i + 1, se.getName(), se.getGroup(), se.getOccurrences()});
        }
        logger.info("");

        ambiguosGroup.sort();
        logger.info("TOP 10 AMBIGUOS GROUP");
        for (int i = 0; i < 10; i++) {
            StatisticsEntry se = ambiguosGroup.get(i);
            logger.info("{}:\t{} ({}) - {}", new Object[]{i + 1, se.getName(), se.getGroup(), se.getOccurrences()});
        }
        logger.info("");

        ambiguosID.sort();
        logger.info("TOP 10 AMBIGUOS ID");
        for (int i = 0; i < 10; i++) {
            StatisticsEntry se = ambiguosID.get(i);
            logger.info("{}:\t{} ({}) - {}", new Object[]{i + 1, se.getName(), se.getGroup(), se.getOccurrences()});
        }
        logger.info("");

        nested.sort();
        logger.info("TOP 10 NESTED");
        for (int i = 0; i < 10 && i < nested.size(); i++) {
            OverlappingEntry oe = nested.get(i);
            logger.info("{}:\t{}", new Object[]{i + 1, oe.toString()});
        }
        logger.info("");

        intersected.sort();
        logger.info("TOP 10 INTERSECTED");
        for (int i = 0; i < 10 && i < intersected.size(); i++) {
            OverlappingEntry oe = intersected.get(i);
            logger.info("{}:\t{}", new Object[]{i + 1, oe.toString()});
        }
        logger.info("");

    }

    private void printPerGroup(final String group) {
        int num = getUniqueAnnotationsGroup(group);
        double p = ((double) num / (double) annotations.getUnique()) * 100.0;
        logger.info("Num. Unique {}: {} ({}%)", new Object[]{group, num, p});
        num = getOccurrencesAnnotationsGroup(group);
        p = ((double) num / (double) annotations.getOccurrences()) * 100.0;
        logger.info("Num. Occurrences {}: {} ({}%)", new Object[]{group, num, p});
        logger.info("");
    }

    private int getUniqueAnnotationsGroup(final String group) {
        int sum = 0;
        for (StatisticsEntry se : annotations) {
            if (se.getGroup().contains(group)) {
                sum++;
            }
        }
        return sum;
    }

    private int getOccurrencesAnnotationsGroup(final String group) {
        int sum = 0;
        for (StatisticsEntry se : annotations) {
            if (se.getGroup().contains(group)) {
                sum += se.getOccurrences();
            }
        }
        return sum;
    }
}
