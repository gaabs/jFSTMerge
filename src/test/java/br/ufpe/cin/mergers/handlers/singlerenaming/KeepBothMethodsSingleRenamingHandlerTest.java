package br.ufpe.cin.mergers.handlers.singlerenaming;

import br.ufpe.cin.app.JFSTMerge;
import br.ufpe.cin.mergers.util.MergeContext;
import br.ufpe.cin.mergers.util.RenamingStrategy;
import br.ufpe.cin.util.TestUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;

public class KeepBothMethodsSingleRenamingHandlerTest {
    private File baseFile = new File("testfiles/renaming/method/base_method/Test.java");
    private File bodyChangedFileBelowSignature = new File("testfiles/renaming/method/changed_body_below_signature/Test.java");
    private File bodyChangedAtEndFile = new File("testfiles/renaming/method/changed_body_at_end/Test.java");
    private File renamedMethodFile = new File("testfiles/renaming/method/renamed_method_1/Test.java");

    private File baseFileWithCalls = new File("testfiles/renaming/method_with_calls/base/Test.java");
    private File bodyChangedAtEndFileWithCalls = new File("testfiles/renaming/method_with_calls/changed_body_at_end/Test.java");
    private File bodyChangedWithNewCallFile = new File("testfiles/renaming/method_with_calls/changed_body_at_end_with_new_call/Test.java");
    private File renamedMethodAndCallsFile = new File("testfiles/renaming/method_with_calls/renamed_method_and_calls/Test.java");
    private File renamedMethodButNotCallsFile = new File("testfiles/renaming/method_with_calls/renamed_method_but_not_calls/Test.java");
    private File renamedMethodWithNewCallFile = new File("testfiles/renaming/method_with_calls/renamed_method_with_new_call/Test.java");

    private JFSTMerge jfstMerge = new JFSTMerge();
    private File base, left, right;
    private MergeContext mergeContext;

    @BeforeClass
    public static void setUpBeforeClass() {
        TestUtils.hideSystemOutput();

        JFSTMerge.renamingStrategy = RenamingStrategy.KEEP_BOTH_METHODS;
    }

    @Test
    public void testHandle_whenLeftRenamesMethod_andRightChangesBodyBelowSignature_shouldNotReportConflict() {
        base = baseFile;
        left = renamedMethodFile;
        right = bodyChangedFileBelowSignature;

        merge();

        TestUtils.verifyMergeResultWithoutRenamingConflict(mergeContext, "publicclassTest{publicvoidn1(){inta;}publicvoidm(){inta=123;}}");
    }

    @Test
    public void testHandle_whenRightRenamesMethod_andLeftChangesBodyBelowSignature_shouldNotReportConflict() {
        base = baseFile;
        left = bodyChangedFileBelowSignature;
        right = renamedMethodFile;

        merge();

        TestUtils.verifyMergeResultWithoutRenamingConflict(mergeContext, "publicclassTest{publicvoidm(){inta=123;}publicvoidn1(){inta;}}");
    }

    @Test
    public void testHandle_whenLeftRenamesMethod_andRightChangesBodyAtEnd_shouldNotReportConflict() {
        base = baseFile;
        left = renamedMethodFile;
        right = bodyChangedAtEndFile;

        merge();

        TestUtils.verifyMergeResultWithoutRenamingConflict(mergeContext, "publicclassTest{publicvoidn1(){inta;}publicvoidm(){inta;a=123;}}");
    }

    @Test
    public void testHandle_whenLeftRenamesMethod_andRightnChangesBodyAtEnd_shouldNotReportConflict() {
        base = baseFile;
        left = bodyChangedAtEndFile;
        right = renamedMethodFile;

        merge();

        TestUtils.verifyMergeResultWithoutRenamingConflict(mergeContext, "publicclassTest{publicvoidm(){inta;a=123;}publicvoidn1(){inta;}}");
    }

    @Test
    public void testHandleWithCalls_whenLeftChangedMethodBody_andRightRenamedMethodAndCalls_shouldNotReportConflict() {
        left = bodyChangedAtEndFileWithCalls;
        base = baseFileWithCalls;
        right = renamedMethodAndCallsFile;

        merge();

        TestUtils.verifyMergeResultWithoutRenamingConflict(mergeContext, "publicclassTest{publicvoidm(){inta;a=123;}publicvoidn2(){inta;}publicstaticvoidmain(String[]args){n2();}}");
    }


    @Test
    public void testHandleWithCalls_whenLeftChangedMethodBody_andRightRenamedMethodButNotCalls_shouldReportConflict() {
        left = bodyChangedAtEndFileWithCalls;
        base = baseFileWithCalls;
        right = renamedMethodButNotCallsFile;

        merge();

        TestUtils.verifyMergeResultWithRenamingConflict(mergeContext, "publicclassTest{<<<<<<<MINEpublicvoidm(){inta;a=123;}=======publicvoidn2(){inta;}>>>>>>>YOURSpublicstaticvoidmain(String[]args){m();}}");
    }

    @Test
    public void testHandleWithCalls_whenLeftChangedMethodBody_andRightRenamedMethodAndAddedNewCall_shouldNotReportConflict() {
        left = bodyChangedAtEndFileWithCalls;
        base = baseFileWithCalls;
        right = renamedMethodWithNewCallFile;

        merge();

        TestUtils.verifyMergeResultWithoutRenamingConflict(mergeContext, "publicclassTest{publicvoidm(){inta;a=123;}publicvoidn2(){inta;}publicvoidn2_call(){n2();}publicstaticvoidmain(String[]args){n2();}}");
    }

    @Test
    public void testHandleWithCalls_whenLeftChangedMethodBodyAndAddedNewCall_andRightRenamedMethodAndCalls_shouldNotReportConflict() {
        left = bodyChangedWithNewCallFile;
        base = baseFileWithCalls;
        right = renamedMethodAndCallsFile;

        merge();

        TestUtils.verifyMergeResultWithoutRenamingConflict(mergeContext, "publicclassTest{publicvoidm(){inta;a=123;}publicvoidm_call(){m();}publicvoidn2(){inta;}publicstaticvoidmain(String[]args){n2();}}");
    }

    @Test
    public void testHandleWithCalls_whenLeftChangedMethodBodyAndAddedNewCall_andRightRenamedMethodButNotCalls_shouldReportConflict() {
        left = bodyChangedWithNewCallFile;
        base = baseFileWithCalls;
        right = renamedMethodButNotCallsFile;

        merge();

        TestUtils.verifyMergeResultWithRenamingConflict(mergeContext, "publicclassTest{<<<<<<<MINEpublicvoidm(){inta;a=123;}=======publicvoidn2(){inta;}>>>>>>>YOURSpublicvoidm_call(){m();}publicstaticvoidmain(String[]args){m();}}");
    }

    @Test
    public void testHandleWithCalls_whenLeftChangedMethodBodyAndAddedNewCall_andRightRenamedMethodAndCorrespondingCallsAndAddedNewCall_shouldNotReportConflict() {
        left = bodyChangedWithNewCallFile;
        base = baseFileWithCalls;
        right = renamedMethodWithNewCallFile;

        merge();

        TestUtils.verifyMergeResultWithoutRenamingConflict(mergeContext, "publicclassTest{publicvoidm(){inta;a=123;}publicvoidm_call(){m();}publicvoidn2(){inta;}publicvoidn2_call(){n2();}publicstaticvoidmain(String[]args){n2();}}");
    }

    private void merge() {
        mergeContext = jfstMerge.mergeFiles(
                left,
                base,
                right,
                null);
    }
}
