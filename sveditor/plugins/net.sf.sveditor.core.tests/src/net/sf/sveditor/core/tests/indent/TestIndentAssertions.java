package net.sf.sveditor.core.tests.indent;

import net.sf.sveditor.core.SVCorePlugin;
import net.sf.sveditor.core.tests.SVCoreTestCaseBase;

public class TestIndentAssertions extends SVCoreTestCaseBase {
	
	public void testProperty() {
		SVCorePlugin.getDefault().enableDebug(false);
		String doc = 
			"module bob ();\n" +
			"	logic thevar, clk, b;\n" +
			"	property p_property (somevar);\n" +
			"		@ (posedge clk)\n" +
			"			(b === 'h0);\n" +
			"	endproperty: p_property\n" +
			"\n" +
			"	ap_thing:\n" +
			"		// comment 1\n" +
			"		assert property (p_property (thevar)) \n" +
			"		begin\n" +
			"			// comment 2\n" +
			"			a.sample ();\n" +
			"		end\n" +
			"		// comment 3\n" +
			"		else\n" +
			"		begin\n" +
			"			// comment 4\n" +
			"			$display (\"thing is %b\");\n" +
			"		end\n" +
			"	// comment 5\n" +
			"	initial\n" +
			"	begin\n" +
			"		// comment 6\n" +
			"		assert (1);\n" +
			"		/* A ml comment \n" +
			"		 */\n" +
			"		`ifdef A\n" +
			"			// comment 7\n" +
			"			begin\n" +
			"				// ifdef code 1\n" +
			"			end\n" +
			"		`else\n" +
			"			// comment 8\n" +
			"			begin\n" +
			"				// ifdef code 2\n" +
			"			end\n" +
			"		`endif\n" +
			"	end\n" +
			"endmodule\n"
			;
		
		IndentTests.runTest(getName(), fLog, doc);
	}

}
