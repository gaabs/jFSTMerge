package br.ufpe.cin.experiments;

import br.ufpe.cin.app.JFSTMerge;
import br.ufpe.cin.mergers.util.MergeContext;
import br.ufpe.cin.mergers.util.MergeScenario;
import br.ufpe.cin.mergers.util.RenamingStrategy;
import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) {
        try {
            BufferedReader reader = Files.newBufferedReader(Paths.get("/home/Gio/Downloads/sample/renaming.revisions"));
            List<String> listRevisions = reader.lines().collect(Collectors.toList());

            JFSTMerge jsFSTMerge = new JFSTMerge();
            JFSTMerge.isGit = true;

            Map<RenamingStrategy, Integer> conflictByStrategy = new HashMap<>();
            List<String> revisionsWithRenamingConflict = new ArrayList<>();

            for (String revision : listRevisions) {
                MergeScenario mergeScenario = jsFSTMerge.mergeRevisions(revision);
                mergeScenario.getTuples().stream()
                        .filter(tuple -> tuple.getContext().renamingConflicts > 0)
                        .forEach(tuple -> {
                            System.out.println("Found renaming");
                            System.out.println(mergeScenario.getRevisionsFilePath());
                            System.out.println(tuple.getBaseFile().getAbsolutePath());
                            System.out.println();
                            revisionsWithRenamingConflict.add(revision);

                            String baseLogsPath = "logs/"
                                    + revision.substring(0, revision.lastIndexOf("/")).replace("/home/Gio/Downloads/sample/", "") + "/";
                            String fileRelativePath = tuple.getBaseFile().getAbsolutePath().substring(tuple.getBaseFile().getAbsolutePath().lastIndexOf("/"));

                            try {
                                FileUtils.copyFile(tuple.getLeftFile(), new File(baseLogsPath + "left" + fileRelativePath));
                                FileUtils.copyFile(tuple.getBaseFile(), new File(baseLogsPath + "base" + fileRelativePath));
                                FileUtils.copyFile(tuple.getRightFile(), new File(baseLogsPath + "right" + fileRelativePath));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            for (RenamingStrategy strategy : RenamingStrategy.values()) {
                                JFSTMerge.renamingStrategy = strategy;
                                MergeContext mergeContext = jsFSTMerge.mergeFiles(tuple.getLeftFile(),
                                        tuple.getBaseFile(),
                                        tuple.getRightFile(),
                                        null);

                                String outputPath = baseLogsPath +
                                        strategy + (mergeContext.renamingConflicts > 0 ? "-" : "-no-") + "conflict" +
                                        fileRelativePath;

                                jsFSTMerge.mergeFiles(tuple.getLeftFile(),
                                        tuple.getBaseFile(),
                                        tuple.getRightFile(),
                                        outputPath);

                                System.out.println(strategy);
                                System.out.println("semistructuredNumberOfConflicts: " + mergeContext.semistructuredNumberOfConflicts);
                                System.out.println("renamingConflicts: " + mergeContext.renamingConflicts);
                                conflictByStrategy.merge(strategy, mergeContext.renamingConflicts, Integer::sum);

                                try {
                                    FileUtils.write(new File(baseLogsPath + "UNSTRUCTURED" + fileRelativePath),
                                            mergeContext.unstructuredOutput);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                            JFSTMerge.renamingStrategy = RenamingStrategy.SAFE;
                        });
            }

            FileUtils.write(new File("mockito-renaming.revisions"),
                    String.join("\n", revisionsWithRenamingConflict));

            System.out.println(conflictByStrategy);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}