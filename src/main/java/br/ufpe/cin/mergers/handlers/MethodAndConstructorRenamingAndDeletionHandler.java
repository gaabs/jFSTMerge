package br.ufpe.cin.mergers.handlers;

import br.ufpe.cin.app.JFSTMerge;
import br.ufpe.cin.files.FilesManager;
import br.ufpe.cin.mergers.util.MergeConflict;
import br.ufpe.cin.mergers.util.MergeContext;
import br.ufpe.cin.mergers.util.RenamingStrategy;
import de.ovgu.cide.fstgen.ast.FSTNode;
import de.ovgu.cide.fstgen.ast.FSTTerminal;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Renaming or deletions conflicts happen when one developer edits a element renamed or deleted by other.
 * Semistructured merge is unable to detect such cases because it matches elements via its identifier, so
 * if a element is renamed or deleted it cannot match the elements anymore. This class overcomes this issue.
 *
 * @author Guilherme
 */
public final class MethodAndConstructorRenamingAndDeletionHandler implements ConflictHandler{
    private static final double BODY_SIMILARITY_THRESHOLD = 0.7;  //a typical value of 0.7 (up to 1.0) is used, increase it for a more accurate comparison, or decrease for a more relaxed one.

    private enum RenamingSide {LEFT, RIGHT}

    public void handle(MergeContext context) {
        //when both developers rename the same method/constructor
        handleMutualRenamings(context);

        //when one of the developers rename a method/constructor
        handleSingleRenamings(context);
    }

    private static void handleMutualRenamings(MergeContext context) {
        if (context.addedLeftNodes.isEmpty() || context.addedRightNodes.isEmpty()) return;
        if (context.deletedBaseNodes.isEmpty()) return;
        if (JFSTMerge.renamingStrategy == RenamingStrategy.KEEP_BOTH_METHODS) return;

        List<FSTNode> leftNewMethodsOrConstructors = context.addedLeftNodes.stream().filter(m -> isMethodOrConstructorNode(m)).collect(Collectors.toList());
        List<FSTNode> rightNewMethodsOrConstructors = context.addedRightNodes.stream().filter(m -> isMethodOrConstructorNode(m)).collect(Collectors.toList());
        for (FSTNode left : leftNewMethodsOrConstructors) {
            for (FSTNode right : rightNewMethodsOrConstructors) {
                if (!haveSameParent(left, right)) continue;
                if (!left.getName().equals(right.getName())) { //only if the two declarations have different signatures
                    String leftBody = getNodeBodyWithoutSignature(left);
                    String rightBody = getNodeBodyWithoutSignature(right);
                    //TODO: check deletedBaseNodes similarity

                    if (leftBody.equals(rightBody)) {//the methods have the same body, ignoring their signature
                        generateMutualRenamingConflict(context, ((FSTTerminal) left).getBody(), ((FSTTerminal) left).getBody(), ((FSTTerminal) right).getBody());
                    }
                    break;
                }
            }
        }
    }

    private static String removeSignature(String string) {
        string = string.replaceFirst("^.[^{]*(?=(\\{))", "");
        return string;
    }

    private static void handleSingleRenamings(MergeContext context) {
        if (context.possibleRenamedLeftNodes.isEmpty() && context.possibleRenamedRightNodes.isEmpty()) return;

        //possible renamings or deletions in left
        handleSingleRenamings(context, context.possibleRenamedLeftNodes, context.addedLeftNodes, RenamingSide.LEFT);

        //possible renamings or deletions in right
        handleSingleRenamings(context, context.possibleRenamedRightNodes, context.addedRightNodes, RenamingSide.RIGHT);
    }

    private static void handleSingleRenamings(MergeContext context, List<Pair<String, FSTNode>> possibleRenamedNodes, List<FSTNode> addedNodes, RenamingSide renamingSide) {
        for (Pair<String, FSTNode> tuple : possibleRenamedNodes) {
            FSTNode currentNode = tuple.getRight();
            if (!nodeHasConflict(currentNode)) continue;

            String baseContent = tuple.getLeft();
            String currentNodeContent = ((FSTTerminal) currentNode).getBody(); //node content with conflict
            MergeConflict mergeConflict = FilesManager.extractMergeConflicts(currentNodeContent).get(0);
            String oppositeSideNodeContent = getMergeConflictContentOfOppositeSide(mergeConflict, renamingSide);

            if (JFSTMerge.renamingStrategy == RenamingStrategy.KEEP_BOTH_METHODS) {
                ((FSTTerminal) currentNode).setBody(oppositeSideNodeContent);
                continue;
            }

            List<Pair<Double, String>> similarNodes = getSimilarNodes(baseContent, currentNode, addedNodes);
            boolean hasUnstructuredMergeConflict = hasUnstructuredMergeConflict(context, baseContent);

            if (JFSTMerge.renamingStrategy == RenamingStrategy.MERGE_METHODS && !similarNodes.isEmpty()) {
                String possibleRenamingContent = getMostSimilarContent(similarNodes);
                String newSignature = getSignature(possibleRenamingContent);
                String newBody = removeSignature(oppositeSideNodeContent);

                //TODO check method references!

                // replace node with both nodes content
                FilesManager.findAndReplaceASTNodeContent(context.superImposedTree, currentNodeContent, newSignature + newBody);

                // remove other node
                FilesManager.findAndDeleteASTNode(context.superImposedTree, possibleRenamingContent);
            } else if (hasUnstructuredMergeConflict) {
                String possibleRenamingContent = getMostSimilarContent(similarNodes);
                generateRenamingConflict(context, currentNodeContent, possibleRenamingContent, oppositeSideNodeContent, renamingSide);
            } else { //do not report the renaming conflict
                ((FSTTerminal) tuple.getRight()).setBody(oppositeSideNodeContent);
            }
        }
    }

    private static boolean hasUnstructuredMergeConflict(MergeContext context, String baseContent) {
        String signature = getTrimmedSignature(baseContent);

        return FilesManager.extractMergeConflicts(context.unstructuredOutput).stream()
                .map(conflict -> FilesManager.getStringContentIntoSingleLineNoSpacing(conflict.body))
                .anyMatch(conflict -> conflict.contains(signature));
    }

    private static List<Pair<Double, String>> getSimilarNodes(String baseContent, FSTNode currentNode, List<FSTNode> addedNodes) {
        //list of possible nodes renaming a previous one
        List<Pair<Double, String>> similarNodes = new ArrayList<>();

        //1. getting similar nodes to fulfill renaming conflicts
        for (FSTNode newNode : addedNodes) { // a possible renamed node is seem as "new" node due to superimposition
            if (!isMethodOrConstructorNode(newNode)) continue;
            if (!haveSameParent(newNode, currentNode)) continue;

            String possibleRenamingContent = ((FSTTerminal) newNode).getBody();
            double bodySimilarity = FilesManager.computeStringSimilarity(baseContent, possibleRenamingContent);
            if (bodySimilarity >= BODY_SIMILARITY_THRESHOLD) {
                Pair<Double, String> tp = Pair.of(bodySimilarity, possibleRenamingContent);
                similarNodes.add(tp);
            }
        }

        return similarNodes;
    }

    private static String getTrimmedSignature(String source) {
        String trimmedSource = FilesManager.getStringContentIntoSingleLineNoSpacing(source);
        return getSignature(trimmedSource);
    }

    private static String getSignature(String source) {
        return source.substring(0, (/*is interface?*/(source.contains("{")) ? source.indexOf("{") : source.indexOf(";")));
    }

    private static String getNodeBodyWithoutSignature(FSTNode node) {
        return Optional.of(node)
                .map(FSTTerminal.class::cast)
                .map(FSTTerminal::getBody)
                .map(FilesManager::getStringContentIntoSingleLineNoSpacing)
                .map(MethodAndConstructorRenamingAndDeletionHandler::removeSignature)
                .orElse(null);
    }

    private static String getMostSimilarContent(List<Pair<Double, String>> similarNodes) {
        return similarNodes.stream()
                .max(Comparator.comparing(Pair::getLeft))
                .map(Pair::getRight)
                .orElse("");
    }

    private static boolean nodeHasConflict(FSTNode node) {
        if (isMethodOrConstructorNode(node)) {
            String body = ((FSTTerminal) node).getBody();
            return body.contains("<<<<<<< MINE");
        }

        return false;
    }

    private static boolean isMethodOrConstructorNode(FSTNode node) {
        if (node instanceof FSTTerminal) {
            String nodeType = node.getType();
            return nodeType.equals("MethodDecl") || nodeType.equals("ConstructorDecl");
        }

        return false;
    }

    private static boolean haveSameParent(FSTNode left, FSTNode right) {
        return left.getParent().equals(right.getParent());
    }

    private static void generateRenamingConflict(MergeContext context, String currentNodeContent, String firstContent, String secondContent, RenamingSide renamingSide) {
        if (renamingSide == RenamingSide.LEFT) {//managing the origin of the changes in the conflict
            String aux = secondContent;
            secondContent = firstContent;
            firstContent = aux;
        }

        //statistics
        if (firstContent.isEmpty() || secondContent.isEmpty()) {
            context.deletionConflicts++;
        } else {
            context.renamingConflicts++;
        }

        //first creates a conflict
        MergeConflict newConflict = new MergeConflict(firstContent + '\n', secondContent + '\n');
        //second put the conflict in one of the nodes containing the previous conflict, and deletes the other node containing the possible renamed version
        FilesManager.findAndReplaceASTNodeContent(context.superImposedTree, currentNodeContent, newConflict.body);
        if (renamingSide == RenamingSide.RIGHT) {
            FilesManager.findAndDeleteASTNode(context.superImposedTree, firstContent);
        } else {
            FilesManager.findAndDeleteASTNode(context.superImposedTree, secondContent);
        }
    }

    private static void generateMutualRenamingConflict(MergeContext context, String currentNodeContent, String firstContent, String secondContent) {
        //statistics
        context.renamingConflicts++;

        //first creates a conflict
        MergeConflict newConflict = new MergeConflict(firstContent + '\n', secondContent + '\n');

        //second put the conflict in one of the nodes containing the previous conflict, and deletes the other node containing the possible renamed version
        FilesManager.findAndReplaceASTNodeContent(context.superImposedTree, currentNodeContent, newConflict.body);
        FilesManager.findAndDeleteASTNode(context.superImposedTree, secondContent);
    }

    private static String getMergeConflictContentOfOppositeSide(MergeConflict mergeConflict, RenamingSide side) {
        if (side == RenamingSide.LEFT) return mergeConflict.right;
        if (side == RenamingSide.RIGHT) return mergeConflict.left;

        return null;
    }
}
