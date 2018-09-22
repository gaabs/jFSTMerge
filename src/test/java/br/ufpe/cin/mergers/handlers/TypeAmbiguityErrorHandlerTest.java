package br.ufpe.cin.mergers.handlers;

import br.ufpe.cin.app.JFSTMerge;
import br.ufpe.cin.files.FilesManager;
import br.ufpe.cin.mergers.util.MergeContext;
import br.ufpe.cin.util.TestUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertTrue;

public class TypeAmbiguityErrorHandlerTest {
	
	@BeforeClass
	public static void setUpBeforeClass() {
		TestUtils.hideSystemOutput();
	}

	@Test
	public void testImportMemberMember() {
		MergeContext ctx = 	new JFSTMerge().mergeFiles(
				new File("testfiles/importmembermember/left/Test/src/Test.java"), 
				new File("testfiles/importmembermember/base/Test/src/Test.java"), 
				new File("testfiles/importmembermember/right/Test/src/Test.java"),
				null);
		String mergeResult = FilesManager.getStringContentIntoSingleLineNoSpacing(ctx.semistructuredOutput);
		
		assertTrue(mergeResult.contains("<<<<<<<MINEimportjava.awt.List;=======importjava.util.List;>>>>>>>YOURS"));
		assertTrue(ctx.typeAmbiguityErrorsConflicts==1);
	}
	
	@Test
	public void testImportPackagePackage() {
		MergeContext ctx = 	new JFSTMerge().mergeFiles(
				new File("testfiles/importpackagepackage/left/Test/src/Test.java"), 
				new File("testfiles/importpackagepackage/base/Test/src/Test.java"), 
				new File("testfiles/importpackagepackage/right/Test/src/Test.java"),
				null);
		String mergeResult = FilesManager.getStringContentIntoSingleLineNoSpacing(ctx.semistructuredOutput);
		
		assertTrue(mergeResult.contains("<<<<<<<MINEimportpckt.*;=======importpcktright.*;>>>>>>>YOURS"));
		assertTrue(ctx.typeAmbiguityErrorsConflicts==1);
	}
	
	@Test
	public void testImportPackageMember() {
		MergeContext ctx = 	new JFSTMerge().mergeFiles(
				new File("testfiles/importpackagemember/left/Test/src/Test.java"), 
				new File("testfiles/importpackagemember/base/Test/src/Test.java"), 
				new File("testfiles/importpackagemember/right/Test/src/Test.java"),
				null);
		String mergeResult = FilesManager.getStringContentIntoSingleLineNoSpacing(ctx.semistructuredOutput);
		
		assertTrue(mergeResult.contains("<<<<<<<MINEimportpckt.A;=======importpcktright.*;>>>>>>>YOURS"));
		assertTrue(ctx.typeAmbiguityErrorsConflicts==1);
	}

}
