package br.ufpe.cin.mergers.handlers;

import br.ufpe.cin.app.JFSTMerge;
import br.ufpe.cin.files.FilesManager;
import br.ufpe.cin.mergers.util.MergeContext;
import br.ufpe.cin.mergers.util.RenamingStrategy;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;

import static org.assertj.core.api.Assertions.assertThat;

public class RenamingConflictsHandlerTest2 {
    private File baseFile = new File("testfiles/renaming/method_with_calls/base/Test.java");
    private File bodyChangedAtEndFile = new File("testfiles/renaming/method_with_calls/changed_body_at_end/Test.java");
    private File bodyChangedWithNewCallFile = new File("testfiles/renaming/method_with_calls/changed_body_at_end_with_new_call/Test.java");
    private File renamedMethodAndCallsFile = new File("testfiles/renaming/method_with_calls/renamed_method_and_calls/Test.java");
    private File renamedMethodButNotCallsFile = new File("testfiles/renaming/method_with_calls/renamed_method_but_not_calls/Test.java");
    private File renamedMethodWithNewCallFile = new File("testfiles/renaming/method_with_calls/renamed_method_with_new_call/Test.java");

    private JFSTMerge jfstMerge = new JFSTMerge();

    @BeforeClass
    public static void setUpBeforeClass() {
        //hidding sysout output
        @SuppressWarnings("unused")
        PrintStream originalStream = System.out;
        PrintStream hideStream = new PrintStream(new OutputStream() {
            public void write(int b) {
            }
        });
//        System.setOut(hideStream);
    }


    //    // 1
//    When Left changed method body
//    And Right renamed method and corresponding calls
//    Then KeepBothMethods should not report conflict
//    And MergeMethods should not report conflict
    @Test
    public void givenKeepBothMethods_whenLeftChangedMethodBody_andRightRenamedMethodAndCalls_shouldNotReportConflict() {
        JFSTMerge.renamingStrategy = RenamingStrategy.KEEP_BOTH_METHODS;

        MergeContext ctx = jfstMerge.mergeFiles(
                bodyChangedAtEndFile,
                baseFile,
                renamedMethodAndCallsFile,
                null);
        String mergeResult = FilesManager.getStringContentIntoSingleLineNoSpacing(ctx.semistructuredOutput);

        assertThat(mergeResult).isEqualTo("publicclassTest{publicvoidm(){inta;a=123;}publicvoidn2(){inta;}publicstaticvoidmain(String[]args){n2();}}");
        assertThat(ctx.renamingConflicts).isZero();
    }

    @Test
    public void givenMergeMethods_whenLeftChangedMethodBody_andRightRenamedMethodAndCalls_shouldNotReportConflict() {
        JFSTMerge.renamingStrategy = RenamingStrategy.MERGE_METHODS;

        MergeContext ctx = jfstMerge.mergeFiles(
                bodyChangedAtEndFile,
                baseFile,
                renamedMethodAndCallsFile,
                null);
        String mergeResult = FilesManager.getStringContentIntoSingleLineNoSpacing(ctx.semistructuredOutput);

        assertThat(mergeResult).isEqualTo("publicclassTest{publicvoidn2(){inta;a=123;}publicstaticvoidmain(String[]args){n2();}}");
        assertThat(ctx.renamingConflicts).isZero();
    }

//
//    // 2
//    When Left changed method body
//    And Right renamed method but forgot calls
//    Then KeepBothMethods should report conflict
//    And MergeMethods should not report conflict

    @Test
    public void givenKeepBothMethods_whenLeftChangedMethodBody_andRightRenamedMethodButNotCalls_shouldReportConflict() {
        JFSTMerge.renamingStrategy = RenamingStrategy.KEEP_BOTH_METHODS;

        MergeContext ctx = jfstMerge.mergeFiles(
                bodyChangedAtEndFile,
                baseFile,
                renamedMethodButNotCallsFile,
                null);
        String mergeResult = FilesManager.getStringContentIntoSingleLineNoSpacing(ctx.semistructuredOutput);

        assertThat(mergeResult).isEqualTo("publicclassTest{<<<<<<<MINEpublicvoidm(){inta;a=123;}=======publicvoidn2(){inta;}>>>>>>>YOURSpublicstaticvoidmain(String[]args){m();}}");
        assertThat(ctx.renamingConflicts).isOne();
    }

    @Test
    public void givenMergeMethods_whenLeftChangedMethodBody_andRightRenamedMethodButNotCalls_shouldNotReportConflict() {
        JFSTMerge.renamingStrategy = RenamingStrategy.MERGE_METHODS;

        MergeContext ctx = jfstMerge.mergeFiles(
                bodyChangedAtEndFile,
                baseFile,
                renamedMethodButNotCallsFile,
                null);
        String mergeResult = FilesManager.getStringContentIntoSingleLineNoSpacing(ctx.semistructuredOutput);

        assertThat(mergeResult).isEqualTo("publicclassTest{publicvoidn2(){inta;a=123;}publicstaticvoidmain(String[]args){m();}}");
        assertThat(ctx.renamingConflicts).isZero();
    }
//
//    // 3
//    When Left changed method body
//    And Right renamed method and added new call
//    Then KeepBothMethods should not report conflict
//    And MergeMethods should report conflict

    @Test
    public void givenKeepBothMethods_whenLeftChangedMethodBody_andRightRenamedMethodAndAddedNewCall_shouldNotReportConflict() {
        JFSTMerge.renamingStrategy = RenamingStrategy.KEEP_BOTH_METHODS;

        MergeContext ctx = jfstMerge.mergeFiles(
                bodyChangedAtEndFile,
                baseFile,
                renamedMethodWithNewCallFile,
                null);
        String mergeResult = FilesManager.getStringContentIntoSingleLineNoSpacing(ctx.semistructuredOutput);

        assertThat(mergeResult).isEqualTo("publicclassTest{publicvoidm(){inta;a=123;}publicvoidn2(){inta;}publicvoidn2_call(){n2();}publicstaticvoidmain(String[]args){n2();}}");
        assertThat(ctx.renamingConflicts).isZero();
    }

    @Test
    public void givenMergeMethods_whenLeftChangedMethodBody_andRightRenamedMethodButAndAddedNewCall_shouldReportConflict() {
        JFSTMerge.renamingStrategy = RenamingStrategy.MERGE_METHODS;

        MergeContext ctx = jfstMerge.mergeFiles(
                bodyChangedAtEndFile,
                baseFile,
                renamedMethodWithNewCallFile,
                null);
        String mergeResult = FilesManager.getStringContentIntoSingleLineNoSpacing(ctx.semistructuredOutput);

        assertThat(mergeResult).isEqualTo("publicclassTest{<<<<<<<MINEpublicvoidm(){inta;a=123;}=======publicvoidn2(){inta;}>>>>>>>YOURSpublicvoidn2_call(){n2();}publicstaticvoidmain(String[]args){n2();}}");
        assertThat(ctx.renamingConflicts).isOne();
    }

//
//    // 4
//    When Left changed method body and added new call
//    And Right renamed method and corresponding calls
//    Then KeepBothMethods should not report conflict
//    And MergeMethods should not report conflict

    @Test
    public void givenKeepBothMethods_whenLeftChangedMethodBodyAndAddedNewCall_andRightRenamedMethodAndCalls_shouldNotReportConflict() {
        JFSTMerge.renamingStrategy = RenamingStrategy.KEEP_BOTH_METHODS;

        MergeContext ctx = jfstMerge.mergeFiles(
                bodyChangedWithNewCallFile,
                baseFile,
                renamedMethodAndCallsFile,
                null);
        String mergeResult = FilesManager.getStringContentIntoSingleLineNoSpacing(ctx.semistructuredOutput);

        assertThat(mergeResult).isEqualTo("publicclassTest{publicvoidm(){inta;a=123;}publicvoidm_call(){m();}publicvoidn2(){inta;}publicstaticvoidmain(String[]args){n2();}}");
        assertThat(ctx.renamingConflicts).isZero();
    }

    @Test
    public void givenMergeMethods_whenLeftChangedMethodBodyAndAddedNewCall_andRightRenamedMethodAndCalls_shouldNotReportConflict() {
        JFSTMerge.renamingStrategy = RenamingStrategy.MERGE_METHODS;

        MergeContext ctx = jfstMerge.mergeFiles(
                bodyChangedWithNewCallFile,
                baseFile,
                renamedMethodAndCallsFile,
                null);
        String mergeResult = FilesManager.getStringContentIntoSingleLineNoSpacing(ctx.semistructuredOutput);

        assertThat(mergeResult).isEqualTo("publicclassTest{<<<<<<<MINEpublicvoidm(){inta;a=123;}=======publicvoidn2(){inta;}>>>>>>>YOURSpublicvoidm_call(){m();}publicstaticvoidmain(String[]args){n2();}}");
        assertThat(ctx.renamingConflicts).isOne();
    }

//
//    // 5
//    When Left changed method body and added new call
//    And Right renamed method but forgot calls
//    Then KeepBothMethods should report conflict
//    And MergeMethods should not report conflict

    @Test
    public void givenKeepBothMethods_whenLeftChangedMethodBodyAndAddedNewCall_andRightRenamedMethodButNotCalls_shouldReportConflict() {
        JFSTMerge.renamingStrategy = RenamingStrategy.KEEP_BOTH_METHODS;

        MergeContext ctx = jfstMerge.mergeFiles(
                bodyChangedWithNewCallFile,
                baseFile,
                renamedMethodButNotCallsFile,
                null);
        String mergeResult = FilesManager.getStringContentIntoSingleLineNoSpacing(ctx.semistructuredOutput);

        assertThat(mergeResult).isEqualTo("publicclassTest{<<<<<<<MINEpublicvoidm(){inta;a=123;}=======publicvoidn2(){inta;}>>>>>>>YOURSpublicvoidm_call(){m();}publicstaticvoidmain(String[]args){m();}}");
        assertThat(ctx.renamingConflicts).isOne();
    }

    @Test
    public void givenMergeMethods_whenLeftChangedMethodBodyAndAddedNewCall_andRightRenamedMethodButNotCalls_shouldReportConflict() {
        JFSTMerge.renamingStrategy = RenamingStrategy.MERGE_METHODS;

        MergeContext ctx = jfstMerge.mergeFiles(
                bodyChangedWithNewCallFile,
                baseFile,
                renamedMethodButNotCallsFile,
                null);
        String mergeResult = FilesManager.getStringContentIntoSingleLineNoSpacing(ctx.semistructuredOutput);

        assertThat(mergeResult).isEqualTo("publicclassTest{<<<<<<<MINEpublicvoidm(){inta;a=123;}=======publicvoidn2(){inta;}>>>>>>>YOURSpublicvoidm_call(){m();}publicstaticvoidmain(String[]args){m();}}");
        assertThat(ctx.renamingConflicts).isOne();
    }
//
//    // 6
//    When Left changed method body and added new call
//    And Right renamed method and added new call
//    Then KeepBothMethods should not report conflict
//    And MergeMethods should report conflict

    @Test
    public void givenKeepBothMethods_whenLeftChangedMethodBodyAndAddedNewCall_andRightRenamedMethodAndCorrespondingCallsAndAddedNewCall_shouldNotReportConflict() {
        JFSTMerge.renamingStrategy = RenamingStrategy.KEEP_BOTH_METHODS;

        MergeContext ctx = jfstMerge.mergeFiles(
                bodyChangedWithNewCallFile,
                baseFile,
                renamedMethodWithNewCallFile,
                null);
        String mergeResult = FilesManager.getStringContentIntoSingleLineNoSpacing(ctx.semistructuredOutput);

        assertThat(mergeResult).contains("publicclassTest{publicvoidm(){inta;a=123;}publicvoidm_call(){m();}publicvoidn2(){inta;}publicvoidn2_call(){n2();}publicstaticvoidmain(String[]args){n2();}}");
        assertThat(ctx.renamingConflicts).isZero();
    }

    @Test
    public void givenMergeMethods_whenLeftChangedMethodBodyAndAddedNewCall_andRightRenamedMethodAndAddedNewCall_shouldReportConflict() {
        JFSTMerge.renamingStrategy = RenamingStrategy.MERGE_METHODS;

        MergeContext ctx = jfstMerge.mergeFiles(
                bodyChangedWithNewCallFile,
                baseFile,
                renamedMethodWithNewCallFile,
                null);
        String mergeResult = FilesManager.getStringContentIntoSingleLineNoSpacing(ctx.semistructuredOutput);

        assertThat(mergeResult).contains("publicclassTest{<<<<<<<MINEpublicvoidm(){inta;a=123;}=======publicvoidn2(){inta;}>>>>>>>YOURSpublicvoidm_call(){m();}publicvoidn2_call(){n2();}publicstaticvoidmain(String[]args){n2();}}");
        assertThat(ctx.renamingConflicts).isOne();
    }

    // mutual?
}