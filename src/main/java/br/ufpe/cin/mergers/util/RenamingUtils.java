package br.ufpe.cin.mergers.util;

import br.ufpe.cin.app.JFSTMerge;
import br.ufpe.cin.exceptions.TextualMergeException;
import br.ufpe.cin.app.JFSTMerge;
import br.ufpe.cin.files.FilesManager;
import br.ufpe.cin.mergers.TextualMerge;
import de.ovgu.cide.fstgen.ast.FSTNode;
import de.ovgu.cide.fstgen.ast.FSTTerminal;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class RenamingUtils {
    public static boolean hasUnstructuredMergeConflict(MergeContext context, String baseContent) {
        String signature = getTrimmedSignature(baseContent);

        return FilesManager.extractMergeConflicts(context.unstructuredOutput).stream()
                .map(conflict -> FilesManager.getStringContentIntoSingleLineNoSpacing(conflict.body))
                .anyMatch(conflict -> conflict.contains(signature));
    }

    public static String getMostSimilarNodeContent(String baseContent, FSTNode currentNode, List<FSTNode> addedNodes) {
        List<Pair<Double, String>> similarNodes = getSimilarNodes(baseContent, currentNode, addedNodes);

        return getMostSimilarContent(similarNodes);
    }

    public static List<Pair<Double, String>> getSimilarNodes(String baseContent, FSTNode currentNode, List<FSTNode> addedNodes) {
        //list of possible nodes renaming a previous one
        List<Pair<Double, String>> similarNodes = new ArrayList<>();

        //1. getting similar nodes to fulfill renaming conflicts
        for (FSTNode newNode : addedNodes) { // a possible renamed node is seem as "new" node due to superimposition
            if (!isMethodOrConstructorNode(newNode)) continue;
            if (!haveSameParent(newNode, currentNode)) continue;

            String possibleRenamingContent = ((FSTTerminal) newNode).getBody();
            double bodySimilarity = FilesManager.computeStringSimilarity(baseContent, possibleRenamingContent);
            if (bodySimilarity >= JFSTMerge.RENAMING_SIMILARITY_THRESHOLD) {
                Pair<Double, String> tp = Pair.of(bodySimilarity, possibleRenamingContent);
                similarNodes.add(tp);
            }
        }

        return similarNodes;
    }

    public static String getMostSimilarContent(List<Pair<Double, String>> similarNodes) {
        return similarNodes.stream()
                .max(Comparator.comparing(Pair::getLeft))
                .map(Pair::getRight)
                .orElse(StringUtils.EMPTY);
    }

    public static String getTrimmedSignature(String source) {
        String trimmedSource = FilesManager.getStringContentIntoSingleLineNoSpacing(source);
        return getSignature(trimmedSource);
    }

    public static String getSignature(String source) {
        return source.substring(0, hasDefinedBody(source) ? source.indexOf("{") : source.indexOf(";"));
    }

    public static boolean hasDefinedBody(String methodSource) {
        return methodSource.contains("{");
    }

    public static String getNodeBodyWithoutSignature(FSTNode node) {
        return Optional.of(node)
                .map(FSTTerminal.class::cast)
                .map(FSTTerminal::getBody)
                .map(FilesManager::getStringContentIntoSingleLineNoSpacing)
                .map(RenamingUtils::removeSignature)
                .orElse(StringUtils.EMPTY);
    }

    public static String removeSignature(String string) {
        if (!hasDefinedBody(string)) return StringUtils.EMPTY;

        return string.replaceFirst("^.[^{]*(?=(\\{))", "");
    }

    public static boolean nodeHasConflict(FSTNode node) {
        if (isMethodOrConstructorNode(node)) {
            String body = ((FSTTerminal) node).getBody();
            return body.contains("<<<<<<< MINE");
        }

        return false;
    }

    public static boolean isMethodOrConstructorNode(FSTNode node) {
        if (node instanceof FSTTerminal) {
            String nodeType = node.getType();
            return nodeType.equals("MethodDecl") || nodeType.equals("ConstructorDecl");
        }

        return false;
    }

    public static boolean haveSameParent(FSTNode left, FSTNode right) {
        return left.getParent().equals(right.getParent());
    }

    public static void generateRenamingConflict(MergeContext context, String currentNodeContent, String baseContent, String firstContent,
                                                String secondContent, Side renamingSide) {
        if (renamingSide == Side.LEFT) {//managing the origin of the changes in the conflict
            String aux = secondContent;
            secondContent = firstContent;
            firstContent = aux;
        }

        //first creates a conflict
        String newConflictBody;
        try {
            newConflictBody = TextualMerge.merge(firstContent, baseContent, secondContent, JFSTMerge.isWhitespaceIgnored);
        } catch (TextualMergeException e) {
            e.printStackTrace();
            MergeConflict newConflict = new MergeConflict(firstContent + '\n', secondContent + '\n');
            newConflictBody = newConflict.body;
        }

        //second put the conflict in one of the nodes containing the previous conflict, and deletes the other node containing the possible renamed version
        FilesManager.findAndReplaceASTNodeContent(context.superImposedTree, currentNodeContent, newConflictBody);
        if (renamingSide == Side.RIGHT) {
            FilesManager.findAndDeleteASTNode(context.superImposedTree, firstContent);
        } else {
            FilesManager.findAndDeleteASTNode(context.superImposedTree, secondContent);
        }

        //statistics
        if (firstContent.isEmpty() || secondContent.isEmpty()) {
            context.deletionConflicts++;
        } else {
            context.renamingConflicts += StringUtils.countMatches(newConflictBody, "<<<<<<<");
        }
    }

    public static String mergeBodies(String leftContent, String baseContent, String rightContent) {
        String bodyResult;

        try {
            String leftBody = RenamingUtils.removeSignature(leftContent);
            String baseBody = RenamingUtils.removeSignature(baseContent);
            String rightBody = RenamingUtils.removeSignature(rightContent);
            bodyResult = TextualMerge.merge(leftBody, baseBody, rightBody, JFSTMerge.isWhitespaceIgnored);
        } catch (TextualMergeException e) {
            e.printStackTrace();
            bodyResult = new MergeConflict(leftContent + '\n', rightContent + '\n').body;
        }

        return bodyResult;
    }

    public static Pair<String, String> getRenamingContentOnCorrectOrder(String possibleRenamingContent, String oppositeSideNodeContent, Side renamingSide) {
        String leftContent, rightContent;
        if (renamingSide == Side.LEFT) {
            leftContent = possibleRenamingContent;
            rightContent = oppositeSideNodeContent;
        } else {
            rightContent = possibleRenamingContent;
            leftContent = oppositeSideNodeContent;
        }

        return Pair.of(leftContent, rightContent);
    }

    public static void generateMutualRenamingConflict(MergeContext context, FSTNode left, FSTNode right) {
        String leftContent = ((FSTTerminal) left).getBody();
        String rightContent = ((FSTTerminal) right).getBody();

        //statistics
        context.renamingConflicts++;

        //first creates a conflict
        MergeConflict newConflict = new MergeConflict(leftContent + '\n', rightContent + '\n');

        //second put the conflict in one of the nodes containing the previous conflict, and deletes the other node containing the possible renamed version
        FilesManager.findAndReplaceASTNodeContent(context.superImposedTree, leftContent, newConflict.body);
        FilesManager.findAndDeleteASTNode(context.superImposedTree, rightContent);
    }

    public static String getMergeConflictContentOfOppositeSide(MergeConflict mergeConflict, Side side) {
        if (side == Side.LEFT) return mergeConflict.right;
        if (side == Side.RIGHT) return mergeConflict.left;

        return null;
    }

    public static List<FSTNode> getMethodsOrConstructors(List<FSTNode> nodes) {
        return nodes.stream()
                .filter(RenamingUtils::isMethodOrConstructorNode)
                .collect(Collectors.toList());
    }

    public static boolean haveDifferentSignature(FSTNode left, FSTNode right) {
        return !haveEqualSignature(left, right);
    }

    public static boolean haveEqualSignature(FSTNode left, FSTNode right) {
        return left.getName().equals(right.getName());
    }

    public static boolean haveDifferentBody(FSTNode left, FSTNode right) {
        return !haveEqualBody(left, right);
    }

    public static boolean haveEqualBody(FSTNode left, FSTNode right) {
        String leftBody = RenamingUtils.getNodeBodyWithoutSignature(left);
        String rightBody = RenamingUtils.getNodeBodyWithoutSignature(right);

        return leftBody.equals(rightBody);
    }

    public static boolean haveSimilarBody(FSTNode left, FSTNode right) {
        String leftBody = RenamingUtils.getNodeBodyWithoutSignature(left);
        String rightBody = RenamingUtils.getNodeBodyWithoutSignature(right);

        double bodySimilarity = FilesManager.computeStringSimilarity(leftBody, rightBody);

        return bodySimilarity >= JFSTMerge.RENAMING_SIMILARITY_THRESHOLD;
    }
}
