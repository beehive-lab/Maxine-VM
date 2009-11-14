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
package com.sun.c1x.xir;

import java.io.PrintStream;

import com.sun.c1x.xir.CiXirAssembler.XirConstant;
import com.sun.c1x.xir.CiXirAssembler.XirInstruction;
import com.sun.c1x.xir.CiXirAssembler.XirLabel;
import com.sun.c1x.xir.CiXirAssembler.XirParameter;
import com.sun.c1x.xir.CiXirAssembler.XirTemp;
import com.sun.c1x.xir.CiXirAssembler.XirOperand;

/**
 * This class represents a completed template of XIR code that has been first assembled by
 * the runtime, and then verified and preprocessed by the compiler.
 */
public class XirTemplate {

    public enum GlobalFlags {
    	HAS_JAVA_CALL,
    	HAS_STUB_CALL,
    	HAS_RUNTIME_CALL,
    	HAS_CONTROL_FLOW,
    	GLOBAL_STUB;

    	public final int mask = 1 << ordinal();

    }

    public final String name;
    public final XirOperand resultOperand;
    public final CiXirAssembler.XirInstruction[] fastPath;
    public final CiXirAssembler.XirInstruction[] slowPath;
    public final XirLabel[] labels;
    public final XirParameter[] parameters;
    public final boolean[] parameterDestroyed;
    public final XirTemp[] temps;
    public final XirConstant[] constants;
    public final int variableCount;
    public final boolean allocateResultOperand;

    public final int flags;

    public XirTemplate(String name, int variableCount, boolean allocateResultOperand, XirOperand resultOperand, CiXirAssembler.XirInstruction[] fastPath, CiXirAssembler.XirInstruction[] slowPath, XirLabel[] labels, XirParameter[] parameters, XirTemp[] temps, XirConstant[] constantValues, int flags) {
    	this.name = name;
    	this.variableCount = variableCount;
    	this.resultOperand = resultOperand;
        this.fastPath = fastPath;
        this.slowPath = slowPath;
        this.labels = labels;
        this.parameters = parameters;
        this.flags = flags;
        this.temps = temps;
        this.allocateResultOperand = allocateResultOperand;
        this.constants = constantValues;

        assert fastPath != null;
        assert labels != null;
        assert parameters != null;

        parameterDestroyed = new boolean[parameters.length];
        for (int i=0; i<parameters.length; i++) {
        	for (XirInstruction ins : fastPath) {
        		if (ins.result == parameters[i]) {
        			parameterDestroyed[i] = true;
        			break;
        		}
        	}

        	if (slowPath != null && !parameterDestroyed[i]) {
        		for (XirInstruction ins : slowPath) {
            		if (ins.result == parameters[i]) {
            			parameterDestroyed[i] = true;
            		}
            	}
        	}
        }
    }

    public boolean isParameterDestroyed(int index) {
    	return parameterDestroyed[index];
    }
    
    public boolean hasJavaCall() {
    	return (flags & GlobalFlags.HAS_JAVA_CALL.mask) != 0;
    }

    @Override
    public String toString() {
    	return name;
    }

    public void print(PrintStream p) {

    	final String indent = "   ";

    	p.println();
    	p.println("Template " + name);

    	p.print("Param:");
    	for (XirParameter param : parameters) {
        	p.print(" " + param.detailedToString());
    	}
    	p.println();

    	if (temps.length > 0) {
	    	p.print("Temps:");
	    	for (XirTemp temp : temps) {
	    		p.print(" " + temp.detailedToString());
	    	}
	    	p.println();
    	}

    	if (constants.length > 0) {
	    	p.print("Constants:");
	    	for (XirConstant c : constants) {
	    		p.print(" " + c.detailedToString());
	    	}
	    	p.println();
    	}

    	if (flags != 0) {
	    	p.print("Flags:");
	    	for (XirTemplate.GlobalFlags flag : XirTemplate.GlobalFlags.values()) {
	    		if ((this.flags & flag.mask) != 0) {
	    			p.print(" " + flag.name());
	    		}
	    	}
	    	p.println();
    	}

    	p.println("Fast path:");
    	for (XirInstruction i : fastPath) {
    		p.println(indent + i.toString());
    	}

    	if (slowPath != null) {
    		p.println("Slow path:");
    		for (XirInstruction i : slowPath) {
    			p.println(indent + i.toString());
    		}
    	}
    }

}