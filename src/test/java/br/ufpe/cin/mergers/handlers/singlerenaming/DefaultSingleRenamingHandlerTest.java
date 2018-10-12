package br.ufpe.cin.mergers.handlers.singlerenaming;

import br.ufpe.cin.app.JFSTMerge;
import br.ufpe.cin.files.FilesManager;
import br.ufpe.cin.mergers.util.JavaCompiler;
import br.ufpe.cin.mergers.util.MergeContext;
import br.ufpe.cin.mergers.util.RenamingStrategy;
import br.ufpe.cin.mergers.util.Source;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultSingleRenamingHandlerTest {
    private File baseFile = new File("testfiles/renaming/method/base_method/Test.java");
    private File bodyChangedFileBelowSignature = new File("testfiles/renaming/method/changed_body_below_signature/Test.java");
    private File bodyChangedAtEndFile = new File("testfiles/renaming/method/changed_body_at_end/Test.java");
    private File renamedMethodFile1 = new File("testfiles/renaming/method/renamed_method_1/Test.java");
    private File renamedMethodFile2 = new File("testfiles/renaming/method/renamed_method_2/Test.java");

    private JFSTMerge jfstMerge = new JFSTMerge();
    private File left, right;
    private MergeContext mergeContext;

    @BeforeClass
    public static void setUpBeforeClass() {
        //hidding sysout output
        @SuppressWarnings("unused")
        PrintStream originalStream = System.out;
        PrintStream hideStream = new PrintStream(new OutputStream() {
            public void write(int b) {
            }
        });

        JFSTMerge.renamingStrategy = RenamingStrategy.SAFE;
    }

    @Test
    public void testMethodRenamingOnLeft_whenLeftRenamesMethod_andRightChangesBodyBelowSignature_shouldReportConflict() {
        left = renamedMethodFile1;
        right = bodyChangedFileBelowSignature;

        merge();

        verifyMergeResultWithConflict("<<<<<<<MINEpublicvoidm(){inta=123;}=======publicvoidn1(){inta;}>>>>>>>YOURS");
    }

    @Test
    public void testMethodRenamingOnRight_whenRightRenamesMethod_andLeftChangesBodyBelowSignature_shouldReportConflict() {
        right = renamedMethodFile1;
        left = bodyChangedFileBelowSignature;

        merge();

        System.out.println(countMethodInvocationInstances(mergeContext.semistructuredOutput, "m"));

        verifyMergeResultWithConflict("<<<<<<<MINEpublicvoidn1(){inta;}=======publicvoidm(){inta=123;}>>>>>>>YOURS");
    }

    @Test
    public void testMethodRenamingOnLeft_whenLeftRenamesMethod_andRightChangesBodyAtEnd_shouldNotReportConflict() {
        left = renamedMethodFile1;
        right = bodyChangedAtEndFile;

        merge();

        verifyMergeResultWithoutConflict("publicclassTest{publicvoidn1(){inta;}publicvoidm(){inta;a=123;}}");
    }

    @Test
    public void testMethodRenamingOnRight_whenRightRenamesMethod_andLeftChangesBodyAtEnd_shouldNotReportConflict() {
        left = bodyChangedAtEndFile;
        right = renamedMethodFile1;

        merge();

        verifyMergeResultWithoutConflict("publicclassTest{publicvoidm(){inta;a=123;}publicvoidn1(){inta;}}");
    }

    private void merge() {
        mergeContext = jfstMerge.mergeFiles(
                left,
                baseFile,
                right,
                null);
    }

    private void verifyMergeResultWithConflict(String expectedResult) {
        String mergeResult = FilesManager.getStringContentIntoSingleLineNoSpacing(mergeContext.semistructuredOutput);
        assertThat(mergeResult).contains(expectedResult);
        assertThat(mergeContext.renamingConflicts).isOne();
    }

    private void verifyMergeResultWithoutConflict(String expectedResult) {
        String mergeResult = FilesManager.getStringContentIntoSingleLineNoSpacing(mergeContext.semistructuredOutput);
        assertThat(mergeResult).contains(expectedResult);
        assertThat(mergeResult).doesNotContain("(cause:possiblerenaming)");
        assertThat(mergeContext.renamingConflicts).isZero();
    }

    private int countMethodInvocationInstances(String source, String methodName) {
        //TODO check performance
        JavaCompiler compiler = new JavaCompiler();
//        org.eclipse.jdt.core.dom.CompilationUnit compilationUnit = compiler.compile(source);
        mergeContext.unstructuredOutput = "" +
                "import br.ufpe.cin.files.FilesManager;" +
                "public class Test {\n" +
                "    public void m(){\n" +
                "    }\n" +
                "    public void n1(){\n" +
                "        int a; m();\n" +
                "FilesManager.x();" +
                "    }\n" +
                "}";
        org.eclipse.jdt.core.dom.CompilationUnit compilationUnit = compiler.compile(mergeContext, Source.UNSTRUCTURED);
        List<ASTNode> instances = new ArrayList<>();
        compilationUnit.accept(new ASTVisitor() {
            @Override
            public boolean visit(MethodInvocation node) {
                if (methodName.equals(node.getName().getIdentifier())) {
                    instances.add(node);
                }

                return super.visit(node);
            }

        });

        Arrays.stream(compilationUnit.getProblems()).forEach(problem -> {
            System.out.println(problem.getMessage() + " : " + problem.isError());
        });

        return instances.size();
    }
}


