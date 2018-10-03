package br.ufpe.cin.mergers.handlers.singlerenaming;

import br.ufpe.cin.files.FilesManager;
import br.ufpe.cin.mergers.util.MergeConflict;
import br.ufpe.cin.mergers.util.MergeContext;
import br.ufpe.cin.mergers.util.RenamingUtils;
import br.ufpe.cin.mergers.util.Side;
import de.ovgu.cide.fstgen.ast.FSTNode;
import de.ovgu.cide.fstgen.ast.FSTTerminal;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public class MergeMethodsSingleRenamingHandler implements SingleRenamingHandler {
    public void handle(MergeContext context, String baseContent, FSTNode conflictNode,
                       List<Pair<Double, String>> similarNodes, Side renamingSide) {

        String conflictNodeContent = ((FSTTerminal) conflictNode).getBody();
        MergeConflict mergeConflict = FilesManager.extractMergeConflicts(conflictNodeContent).get(0);
        String oppositeSideNodeContent = RenamingUtils.getMergeConflictContentOfOppositeSide(mergeConflict, renamingSide);

        String possibleRenamingContent = RenamingUtils.getMostSimilarContent(similarNodes);
        String newSignature = RenamingUtils.getSignature(possibleRenamingContent);
        String newBody = RenamingUtils.removeSignature(oppositeSideNodeContent);

        String oldMethodName = RenamingUtils.getMethodName(baseContent);
        String newMethodName = RenamingUtils.getMethodName(possibleRenamingContent);

        if (RenamingUtils.hasNewMethodCall(context, oldMethodName, newMethodName, renamingSide)) {
            RenamingUtils.generateRenamingConflict(context, conflictNodeContent, oppositeSideNodeContent,
                    possibleRenamingContent, renamingSide);
            FilesManager.findAndDeleteASTNode(context.superImposedTree, possibleRenamingContent);
        } else {
            // replace node with both nodes content
            FilesManager.findAndReplaceASTNodeContent(context.superImposedTree, conflictNodeContent, newSignature + newBody);

            // remove other node
            FilesManager.findAndDeleteASTNode(context.superImposedTree, possibleRenamingContent);
        }
    }
}
