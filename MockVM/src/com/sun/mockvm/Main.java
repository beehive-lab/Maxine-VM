/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */

package com.sun.mockvm;

import com.sun.c1x.C1XCompiler;
import com.sun.c1x.C1XOptions;
import com.sun.c1x.target.amd64.AMD64;
import com.sun.cri.ci.CiCompiler;
import com.sun.cri.ci.CiResult;
import com.sun.cri.ci.CiTarget;
import com.sun.cri.ci.CiTargetMethod;
import com.sun.cri.ri.RiMethod;
import com.sun.cri.ri.RiRegisterConfig;
import com.sun.cri.xir.RiXirGenerator;

/**
 * @author Thomas Wuerthinger
 * 
 * Sets up a mock runtime for C1X and tries to compile a method. Generates a file output.cfg that can be viewed with the c1visualizer.
 *
 */
public class Main {

	
	public static void main(String[] args) {

		final MockRuntime runtime = new MockRuntime();
		final RiXirGenerator generator = new MockXirGenerator();
		final int wordSize = 8;
		final int stackFrameAlignment = 8;
		final int pageSize = 1024;
		final RiRegisterConfig config = new MockRegisterConfig();
        final CiTarget target = new CiTarget(new AMD64(), config, true, wordSize, wordSize, wordSize, stackFrameAlignment, pageSize, wordSize, wordSize, 16);
        final CiCompiler compiler = new C1XCompiler(runtime, target, generator);
        
        C1XOptions.setOptimizationLevel(3);
        C1XOptions.PrintCFGToFile = true;
        C1XOptions.PrintAssembly = true;
        
        MockType type = MockUniverse.lookupType("com.sun.mockvm.Main");
        RiMethod method = type.lookupMethod("main", "([Ljava/lang/String;)V");
        CiResult result = compiler.compileMethod(method, generator);
        
        if (result.bailout() != null) {
        	result.bailout().printStackTrace();
        } else {
        	CiTargetMethod targetMethod = result.targetMethod();
        	System.out.println("Code size: " + targetMethod.targetCode().length);
        }
        
        
	}
}
