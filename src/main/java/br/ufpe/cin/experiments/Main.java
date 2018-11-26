package br.ufpe.cin.experiments;

import br.ufpe.cin.app.JFSTMerge;
import br.ufpe.cin.files.FilesTuple;
import br.ufpe.cin.mergers.util.MergeContext;
import br.ufpe.cin.mergers.util.MergeScenario;
import br.ufpe.cin.mergers.util.RenamingStrategy;
import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Predicates.not;

public class Main {
    private static final String SAMPLE_BASE_PATH = "/home/gio/Downloads/sample/";
    private static final String SAMPLE_REVISIONS_FILENAME = "test.revisions";
    private static final String LOGS_BASE_PATH = "logs/";

    private static final Map<String, Integer> numberOfFilesWithAnyConflict = new ConcurrentHashMap<>();
    private static final Map<String, Integer> numberOfAnyConflict = new ConcurrentHashMap<>();
    private static final Map<String, Integer> numberOfFilesWithRenamingConflict = new ConcurrentHashMap<>();
    private static final Map<String, Integer> numberOfRenamingConflicts = new ConcurrentHashMap<>();

    private static List<String> filesWithSafeRenamingConflict = new ArrayList<>();
    private static List<String> filesWithNoSafeRenamingConflict = new ArrayList<>();

    private static JFSTMerge jsFSTMerge;

    public static void main(String[] args) {
        Long startTime = System.currentTimeMillis();
        try {
            BufferedReader reader = Files.newBufferedReader(Paths.get(SAMPLE_BASE_PATH + SAMPLE_REVISIONS_FILENAME));
            List<String> listRevisions = reader.lines().collect(Collectors.toList());

            jsFSTMerge = new JFSTMerge();
            JFSTMerge.isGit = true;

            int currentRevisionNumber = 0;
            for (String revision : listRevisions) {
                currentRevisionNumber++;
                System.out.printf("------- At %d out of %d revisions -------\n", currentRevisionNumber, listRevisions.size());
                System.out.println(revision);

                JFSTMerge.renamingStrategy = RenamingStrategy.MERGE_METHODS;
                mergeRevision(revision);
            }

            writeListFile(LOGS_BASE_PATH + "filesWithSafeRenamingConflict.txt", filesWithSafeRenamingConflict);
            writeListFile(LOGS_BASE_PATH + "filesWithNoSafeRenamingConflict.txt", filesWithNoSafeRenamingConflict);

            System.out.println("numberOfFilesWithAnyConflict:" + numberOfFilesWithAnyConflict);
            System.out.println("numberOfConflicts:" + numberOfAnyConflict);
            System.out.println("numberOfFilesWithRenamingConflict:" + numberOfFilesWithRenamingConflict);
            System.out.println("numberOfRenamingConflicts:" + numberOfRenamingConflicts);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        Long endTime = System.currentTimeMillis();
        System.out.printf("took %.3f minutes\n", (endTime - startTime) / (1000.0 * 60));
    }

    private static void mergeRevision(String revision) {
        MergeScenario mergeScenario = jsFSTMerge.mergeRevisions(revision);
        mergeScenario.getTuples().stream()
                .peek(tuple -> {
                    numberOfAnyConflict.merge("unstructured", tuple.getContext().unstructuredNumberOfConflicts, Integer::sum);
                    numberOfFilesWithAnyConflict.merge("unstructured", tuple.getContext().unstructuredNumberOfConflicts > 0 ? 1 : 0, Integer::sum);
                    numberOfAnyConflict.merge("semistructured", tuple.getContext().semistructuredNumberOfConflicts, Integer::sum);
                    numberOfFilesWithAnyConflict.merge("semistructured", tuple.getContext().semistructuredNumberOfConflicts > 0 ? 1 : 0, Integer::sum);
                })
//                        .filter(tuple -> tuple.getBaseFile() != null)
//                        .filter(tuple -> tuple.getContext().renamingConflicts > 0)
//                        .filter(tuple -> tuple.getLeftFile() != null && tuple.getBaseFile() != null && tuple.getRightFile() != null)
//                        .filter(tuple -> !RenamingUtils.getMethodsOrConstructors(tuple.getContext().nodesDeletedByLeft).isEmpty() ||
//                                !RenamingUtils.getMethodsOrConstructors(tuple.getContext().nodesDeletedByRight).isEmpty())
//                        .filter(tuple -> {
//                            JFSTMerge.renamingStrategy = RenamingStrategy.MERGE_METHODS;
//                            MergeContext mergeContext = jsFSTMerge.mergeFiles(tuple.getLeftFile(),
//                                    tuple.getBaseFile(),
//                                    tuple.getRightFile(),
//                                    null);
//
//                            return !mergeContext.semistructuredOutput.equals(tuple.getContext().semistructuredOutput);
//                        })
                .forEach(tuple -> {
                    String logPath = getLogPath(revision, tuple, tuple.getContext().renamingConflicts > 0);

                    if (tuple.getContext().renamingConflicts > 0) {
                        filesWithSafeRenamingConflict.add(logPath);
                    } else {
                        filesWithNoSafeRenamingConflict.add(logPath);
//                        return;
                    }

//                            System.out.println("Found renaming");
//                            System.out.println(mergeScenario.getRevisionsFilePath());
//                            System.out.println(logPath);
//                            System.out.println();

                    copyFile(tuple.getLeftFile(), logPath + "/left");
                    copyFile(tuple.getBaseFile(), logPath + "/base");
                    copyFile(tuple.getRightFile(), logPath + "/right");
                    writeFile(logPath + "/UNSTRUCTURED", tuple.getContext().unstructuredOutput);

                    for (RenamingStrategy strategy : RenamingStrategy.values()) {
                        JFSTMerge.renamingStrategy = strategy;
                        JFSTMerge.logFiles = true;
                        MergeContext mergeContext = jsFSTMerge.mergeFiles(tuple.getLeftFile(),
                                tuple.getBaseFile(),
                                tuple.getRightFile(),
                                null);

                        String outputPath = logPath + "/" + strategy;

                        jsFSTMerge.mergeFiles(tuple.getLeftFile(),
                                tuple.getBaseFile(),
                                tuple.getRightFile(),
                                outputPath);

//                                System.out.println(strategy);
//                                System.out.println("semistructuredNumberOfConflicts: " + mergeContext.semistructuredNumberOfConflicts);
//                                System.out.println("renamingConflicts: " + mergeContext.renamingConflicts);
                        synchronized (numberOfRenamingConflicts) {
                            numberOfRenamingConflicts.merge(strategy.name(), mergeContext.renamingConflicts, Integer::sum);
                        }
                        synchronized (numberOfFilesWithRenamingConflict) {
                            numberOfFilesWithRenamingConflict.merge(strategy.name(), mergeContext.renamingConflicts > 0 ? 1 : 0, Integer::sum);
                        }
                    }
                });
    }

    private static String getLogPath(String revision, FilesTuple tuple, boolean hasSafeRenamingConflict) {
        String revisionPath = revision.substring(0, revision.lastIndexOf("/")).replace("/home/gio/Downloads/sample/", "") + "/";
        revisionPath = revisionPath.replaceAll("/", ".");

        String fileAbsolutePath = Stream.of(tuple.getBaseFile(), tuple.getLeftFile(), tuple.getRightFile())
                .filter(not(Objects::isNull))
                .map(File::getAbsolutePath)
                .findFirst()
                .get();

        String fileRelativePath = fileAbsolutePath.substring(fileAbsolutePath.lastIndexOf("/") + 1)
                .replaceAll("/", ".");

        return LOGS_BASE_PATH
//                + (hasSafeRenamingConflict ? "conflict/" : "no-conflict/")
                + revisionPath + fileRelativePath;
    }

    private static void copyFile(File sourceFile, String destinationPath) {
        if (sourceFile == null) {
            System.out.println("Empty source file. No file will be written at " + destinationPath);
            return;
        }

        try {
            FileUtils.copyFile(sourceFile, new File(destinationPath));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void writeFile(String destination, String source) {
        try {
            FileUtils.write(new File(destination), source);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void writeListFile(String destination, Iterable<String> list) {
        String lines = String.join("\n", list);
        writeFile(destination, lines);
    }
}