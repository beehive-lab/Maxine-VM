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

import com.sun.c1x.xir.XirAssembler.XirConstant;
import com.sun.c1x.xir.XirAssembler.XirInstruction;
import com.sun.c1x.xir.XirAssembler.XirLabel;
import com.sun.c1x.xir.XirAssembler.XirParameter;
import com.sun.c1x.xir.XirAssembler.XirTemp;
import com.sun.c1x.xir.XirAssembler.XirVariable;
import com.sun.c1x.ci.CiRegister;

/**
 * This class represents a completed template of XIR code that has been first assembled by
 * the runtime, and then verified and preprocessed by the compiler.
 */
public class XirTemplate {

    public static int DESTROY = 0x10000000;
    public static int INPUT   = 0x20000000;
    public static int OUTPUT  = 0x40000000;
    public static int FIXED   = 0x80000000;

    enum GlobalFlags {
    	HAS_JAVA_CALL,
    	HAS_STUB_CALL,
    	HAS_RUNTIME_CALL,
    	HAS_CONTROL_FLOW,
    	GLOBAL_STUB;
    	
    	public int mask() {
    		return 1 << ordinal();
    	}
    }

    public final String name;
    public final XirVariable resultOperand;
    public final XirAssembler.XirInstruction[] fastPath;
    public final XirAssembler.XirInstruction[] slowPath;
    public final XirLabel[] labels;
    public final XirParameter[] parameters;
    public final XirAssembler.XirTemp[] temps;
    public final XirAssembler.XirConstant[] constants;
    public final int variableCount;
    
    public int[] tempFlags;
    public int flags;

    XirTemplate(String name, int variableCount, XirVariable resultOperand, XirAssembler.XirInstruction[] fastPath, XirAssembler.XirInstruction[] slowPath, XirLabel[] labels, XirParameter[] parameters, XirTemp[] temps, XirConstant[] constants, int flags) {
    	this.name = name;
    	this.variableCount = variableCount;
    	this.resultOperand = resultOperand;
        this.fastPath = fastPath;
        this.slowPath = slowPath;
        this.labels = labels;
        this.parameters = parameters;
        this.flags = flags;
        this.temps = temps;
        this.constants = constants;
        
        assert fastPath != null;
        assert labels != null;
        assert parameters != null;
    }

    public boolean destroysTemp(int index) {
        return (tempFlags[index] & DESTROY) != 0;
    }

    public boolean inputTemp(int index) {
        return (tempFlags[index] & INPUT) != 0;
    }

    public boolean outputTemp(int index) {
        return (tempFlags[index] & OUTPUT) != 0;
    }

    public boolean inputOutputTemp(int index) {
        return inputTemp(index) && outputTemp(index);
    }

    public boolean fixedTemp(int index) {
        return (tempFlags[index] & FIXED) != 0;
    }

    public CiRegister fixedRegister(int index, CiRegister[] registers) {
        if ((tempFlags[index] & FIXED) != 0) {
            int regnum = tempFlags[index] & 0xfff;
            return registers[regnum];
        }
        return null;
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
	    		if ((this.flags & flag.mask()) != 0) {
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