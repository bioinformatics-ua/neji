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

package pt.ua.tm.neji.disambiguator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.tm.gimli.corpus.AnnotationID;
import pt.ua.tm.gimli.corpus.AnnotationID.AnnotationType;
import pt.ua.tm.gimli.corpus.Corpus;
import pt.ua.tm.gimli.corpus.Identifier;
import pt.ua.tm.gimli.corpus.Sentence;
import pt.ua.tm.gimli.tree.Tree;
import pt.ua.tm.gimli.tree.TreeNode;
import pt.ua.tm.neji.util.Char;

import java.util.ArrayList;
import java.util.List;

/**
 * Remove ambiguity between concepts in the concept tree, following provided rules.
 * @author David Campos (<a href="mailto:david.campos@ua.pt">david.campos@ua.pt</a>)
 * @version 1.0
 * @since 1.0
 */
public class Disambiguator {

    /** {@link Logger} to be used in the class. */
    private static Logger logger = LoggerFactory.getLogger(Disambiguator.class);

    public static void disambiguate(Corpus c, int level, boolean mergeNestedSameGroup,
                                    boolean discardSameGroupByPriority) {
        for (Sentence s : c.getSentences()) {
            disambiguate(s, level, mergeNestedSameGroup, discardSameGroupByPriority);
        }
    }

    public static void disambiguate(Sentence s, int depth, boolean mergeNestedSameGroup,
                                    boolean discardSameIdDifferentSubGroup) {
        assert (depth > 0);

        List<TreeNode<AnnotationID>> nodes = s.getTree().build(depth);

        for (TreeNode<AnnotationID> node : nodes) {
            AnnotationID data = node.getData();

            if (data.getType().equals(AnnotationType.INTERSECTION)) { // Intersection
                int maxSize = 0;
                AnnotationID maxAnnotation = null;

                // Get larger
                for (TreeNode<AnnotationID> child : node.getChildren()) {
                    AnnotationID childData = child.getData();
                    int size = Char.getNumNonWhiteSpaceChars(childData.getText());
                    if (size > maxSize) {
                        maxSize = size;
                        maxAnnotation = childData;
                    }
                }

                // Remove children
                node.removeChildren();
                node.getData().setType(AnnotationType.LEAF);

                // Set data as the largest one
                node.setData(maxAnnotation);

            } else if (data.getType().equals(AnnotationType.NESTED)) { // Nested
                node.removeChildren();
                node.getData().setType(AnnotationType.LEAF);
            }

            // Merge Nested
            TreeNode<AnnotationID> parentNode = node.getParent();
            if (mergeNestedSameGroup && parentNode != null) {
                AnnotationID parentAnnotation = parentNode.getData();
                if (parentAnnotation.getType().equals(AnnotationType.NESTED)) {
                    if (parentAnnotation.areIDsFromTheSameGroup() && !parentAnnotation.getIDs().isEmpty()) {
                        String group = parentAnnotation.getID(0).getGroup();
                        boolean sameGroup = true;
                        for (TreeNode<AnnotationID> child : parentNode.getChildren()) {
                            if (!child.getData().areIDsFromTheSameGroup(group)) {
                                sameGroup = false;
                                break;
                            }
                        }

                        if (sameGroup) {
                            parentNode.removeChildren();
                            parentNode.getData().setType(AnnotationType.LEAF);
                        }

                    }
                }
            }
        }

        // Final cleaning
        nodes = s.getTree().build(Tree.TreeTraversalOrderEnum.PRE_ORDER);
        for (TreeNode<AnnotationID> node : nodes) {
            AnnotationID data = node.getData();
            // Remove equal IDs from the same subgroup by priority
            if (discardSameIdDifferentSubGroup) {
                List<Identifier> ids = data.getIDs();
                List<Identifier> toRemove = new ArrayList<>();

                for (int i = ids.size() - 1; i >= 0; i--) {

                    Identifier i1 = ids.get(i);
                    String id1 = i1.getID();
                    String group1 = i1.getGroup();
                    String subgroup1 = i1.getSubGroup();

                    for (int j = i - 1; j >= 0; j--) {
                        Identifier i2 = ids.get(j);
                        String id2 = i2.getID();
                        String group2 = i2.getGroup();
                        String subgroup2 = i2.getSubGroup();

                        if (group1.equals(group2) && !subgroup1.equals(subgroup2)
                                && id1.equals(id2)) {
                            toRemove.add(i1);
                        }
                    }
                }

                // Remove
                ids.removeAll(toRemove);
            }
        }


    }
}
