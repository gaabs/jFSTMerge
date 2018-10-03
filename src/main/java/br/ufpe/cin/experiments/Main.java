package br.ufpe.cin.experiments;

import br.ufpe.cin.app.JFSTMerge;
import br.ufpe.cin.mergers.util.MergeScenario;
import br.ufpe.cin.mergers.util.RenamingStrategy;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) {
        try {
            BufferedReader reader;
            reader = Files.newBufferedReader(Paths.get("/home/Gio/Downloads/sample/test.revisions"));
            List<String> listRevisions = reader.lines().collect(Collectors.toList());

            JFSTMerge jsFSTMerge = new JFSTMerge();

            for (String r : listRevisions) {
                MergeScenario mergeScenario = jsFSTMerge.mergeRevisions(r);
                mergeScenario
                        .getTuples()
                        .forEach(
                                filesTuple -> {
                                    if (filesTuple.getContext().renamingConflicts > 0) {
                                        System.out.println("Found renaming");
                                        System.out.println(mergeScenario.getRevisionsFilePath());
                                        System.out.println(filesTuple.getBaseFile().getAbsolutePath());
                                        System.out.println();

                                        for (RenamingStrategy strategy : RenamingStrategy.values()) {
                                            String outputPath = "logs/" +
                                                    strategy + "-" +
                                                    filesTuple.getBaseFile().getAbsolutePath().replace("/home/Gio/Downloads/sample/", "");

                                            JFSTMerge.renamingStrategy = strategy;
                                            jsFSTMerge.mergeFiles(filesTuple.getLeftFile(),
                                                    filesTuple.getBaseFile(),
                                                    filesTuple.getRightFile(),
                                                    outputPath);
                                        }
                                        JFSTMerge.renamingStrategy = RenamingStrategy.SAFE;
                                    }
                                });
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
