package net.sf.sveditor.core.tests.parser;

import net.sf.sveditor.core.SVCorePlugin;
import net.sf.sveditor.core.db.SVDBFile;
import net.sf.sveditor.core.parser.SVParseException;
import net.sf.sveditor.core.tests.SVDBTestUtils;
import junit.framework.TestCase;

public class TestParseBehavioralStmts extends TestCase {

	public void testModulePreBodyImport3() {
		String doc = 
			"package p;\n" +
			"endpackage\n" +
			"\n" +
			"module t import p::*, p1::*, p2::*;\n" +
			"	#(\n" +
			"		parameter a = 0\n" +
			"	) // Error.\n" +
			"	();\n" +
			"endmodule\n" +
			"\n"
			;
		
		SVCorePlugin.getDefault().enableDebug(false);
		
		runTest("testModulePreBodyImport3", doc, new String[] {
				"p", "t", "p::*", "p1::*", "p2::*"});
	}

	public void testVarDeclForStmt() throws SVParseException {
		String doc =
			"module t;\n" +
			"	initial begin\n" +
			"		for (int i=0; i<5; i++) begin\n" +
			"			x++;\n" +
			"		end\n" +
			"	end\n" +
			"endmodule\n"
			;
		SVCorePlugin.getDefault().enableDebug(false);
		
		runTest("testVarDeclForStmt", doc, new String[] { "t" });
		
	}

	public void testVarDeclListForStmt() throws SVParseException {
		String doc =
			"module t;\n" +
			"	initial begin\n" +
			"		for (int i=0, j=2; i<5; i++, j++) begin\n" +
			"			x++;\n" +
			"		end\n" +
			"	end\n" +
			"endmodule\n"
			;
		SVCorePlugin.getDefault().enableDebug(false);
		
		runTest("testVarDeclListForStmt", doc, new String[] { "t" });
		
	}
	
	
	public void testVarDeclListForStmt2() throws SVParseException {
		String doc =
			"module t;\n" +
			"	initial begin\n" +
			"	for(i__=0; i__<data.size() && i__<local_data__.data.size(); ++i__) begin\n" +
			"			x++;\n" +
			"		end\n" +
			"	end\n" +
			"endmodule\n"
			;
		SVCorePlugin.getDefault().enableDebug(false);
		
		runTest("testVarDeclListForStmt", doc, new String[] { "t" });
		
	}
	
	private void runTest(
			String			testname,
			String			doc,
			String			exp_items[]) {
		SVDBFile file = SVDBTestUtils.parse(doc, testname);
		
		SVDBTestUtils.assertNoErrWarn(file);
		SVDBTestUtils.assertFileHasElements(file, exp_items);
	}
	
}