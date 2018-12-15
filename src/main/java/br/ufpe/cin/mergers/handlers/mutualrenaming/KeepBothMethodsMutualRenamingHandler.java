package br.ufpe.cin.mergers.handlers.mutualrenaming;

import br.ufpe.cin.mergers.util.MergeContext;

/**
 * This handler simply keeps both nodes involved on mutual renaming conflict.
 */
public class KeepBothMethodsMutualRenamingHandler implements MutualRenamingHandler {
    public void handle(MergeContext context) {
        // Do nothing (keep both)
    }
}
