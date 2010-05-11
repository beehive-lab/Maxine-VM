package com.sun.hotspot.c1x;

import com.sun.c1x.C1XCompiler;
import com.sun.c1x.C1XOptions;
import com.sun.c1x.target.amd64.AMD64;
import com.sun.cri.ci.CiCompiler;
import com.sun.cri.ci.CiTarget;
import com.sun.cri.ri.RiRegisterConfig;
import com.sun.cri.xir.RiXirGenerator;

public class Compiler {

	private static CiCompiler compiler;
	
	public static CiCompiler getCompiler() {
		
		if (compiler == null) {
			compiler = createCompiler();
		}
		
		return compiler;
	}
	
	
	private static CiCompiler createCompiler() {

		final HotSpotRuntime runtime = new HotSpotRuntime();
		final RiXirGenerator generator = new HotSpotXirGenerator();
		final int wordSize = 8;
		final int stackFrameAlignment = 8;
		final int pageSize = 1024;
		final RiRegisterConfig config = new HotSpotRegisterConfig();
        final CiTarget target = new CiTarget(new AMD64(), config, true, wordSize, wordSize, wordSize, stackFrameAlignment, pageSize, wordSize, wordSize, 16);
        final CiCompiler compiler = new C1XCompiler(runtime, target, generator);
        
        C1XOptions.setOptimizationLevel(3);
        C1XOptions.PrintCFGToFile = true;
        C1XOptions.PrintAssembly = true;
        
        return compiler;
        
	}
}
