package br.ufpe.cin.mergers.handlers;

import br.ufpe.cin.app.JFSTMerge;
import br.ufpe.cin.mergers.util.MergeContext;
import br.ufpe.cin.util.TestUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertTrue;

public class DuplicatedDeclarationErrorsHandlerTest {

	@BeforeClass
	public static void setUpBeforeClass() {
		TestUtils.hideSystemOutput();
	}

	@Test
	public void testDuplicationErrorNoConflict() {
		MergeContext ctx = 	new JFSTMerge().mergeFiles(
				new File("testfiles/duplicationsnoconflict/left/Test.java"),
				new File("testfiles/duplicationsnoconflict/base/Test.java"),
				new File("testfiles/duplicationsnoconflict/right/Test.java"),
				null);
		assertTrue(ctx.duplicatedDeclarationErrors==1);
	}

	@Test
	public void testConflictingDuplicationError() {
		MergeContext ctx = 	new JFSTMerge().mergeFiles(
				new File("testfiles/duplicationsconflicting/left/Test.java"),
				new File("testfiles/duplicationsconflicting/base/Test.java"),
				new File("testfiles/duplicationsconflicting/right/Test.java"),
				null);
		assertTrue(ctx.duplicatedDeclarationErrors==0);
	}
}
