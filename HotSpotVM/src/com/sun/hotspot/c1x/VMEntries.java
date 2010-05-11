package com.sun.hotspot.c1x;

import com.sun.cri.ci.CiResult;
import com.sun.cri.ri.RiMethod;

public class VMEntries {
	
	public static void compileMethod(RiMethod method) {
		System.out.println("compileMethod in Java code called!");
		CiResult result = Compiler.getCompiler().compileMethod(method, null);
		System.out.println("Compilation result: ");
		if (result.bailout() != null) {
			System.out.println("Bailout:");
			result.bailout().printStackTrace();
		} else {
			System.out.println(result.targetMethod());
		}
	}
}
