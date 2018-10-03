package br.ufpe.cin.mergers.handlers.singlerenaming;

import br.ufpe.cin.files.FilesManager;
import br.ufpe.cin.mergers.util.MergeConflict;
import br.ufpe.cin.mergers.util.MergeContext;
import br.ufpe.cin.mergers.util.RenamingUtils;
import br.ufpe.cin.mergers.util.Side;
import de.ovgu.cide.fstgen.ast.FSTNode;
import de.ovgu.cide.fstgen.ast.FSTTerminal;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.util.List;

public class KeepBothMethodsSingleRenamingHandler implements SingleRenamingHandler {
    public void handle(MergeContext context, String baseContent, FSTNode conflictNode,
                       List<Pair<Double, String>> similarNodes, Side renamingSide) {

        String conflictNodeContent = ((FSTTerminal) conflictNode).getBody();
        MergeConflict mergeConflict = FilesManager.extractMergeConflicts(conflictNodeContent).get(0);
        String oppositeSideNodeContent = RenamingUtils.getMergeConflictContentOfOppositeSide(mergeConflict, renamingSide);

        String possibleRenamingContent = RenamingUtils.getMostSimilarContent(similarNodes);
        String oldMethodName = RenamingUtils.getMethodName(baseContent);

        File fileWithRenaming = RenamingUtils.getFileOfSide(context, renamingSide);

        if (RenamingUtils.forgotToRenameMethodCall(fileWithRenaming, oldMethodName)) {
            RenamingUtils.generateRenamingConflict(context, conflictNodeContent, oppositeSideNodeContent,
                    possibleRenamingContent, renamingSide);
            FilesManager.findAndDeleteASTNode(context.superImposedTree, possibleRenamingContent);
        } else {
            ((FSTTerminal) conflictNode).setBody(oppositeSideNodeContent);
        }
    }
}
