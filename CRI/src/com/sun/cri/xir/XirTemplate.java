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
package com.sun.cri.xir;

import java.io.*;
import java.util.*;

import com.sun.cri.xir.CiXirAssembler.*;

/**
 * Represents a completed template of XIR code that has been first assembled by
 * the runtime, and then verified and preprocessed by the compiler. An {@code XirTemplate}
 * instance is immutable.
 *
 * @author Ben L. Titzer
 */
public class XirTemplate {

    /**
     * Flags that indicate key features of the template for quick checking.
     */
    public enum GlobalFlags {
    	/**
    	 * Contains a call to a {@link GlobalFlags#GLOBAL_STUB} template.
    	 */
    	HAS_STUB_CALL,
    	/**
    	 * Contains a call to the runtime.
    	 */
    	HAS_RUNTIME_CALL,
    	/**
    	 * Not simply a linear sequence of instructions, contains control transfers.
    	 */
    	HAS_CONTROL_FLOW,
    	/**
    	 * Is a shared instruction sequence for use by other templates.
    	 */
    	GLOBAL_STUB;

    	public final int mask = 1 << ordinal();
    }

    /**
     * Name of the template.
     */
    public final String name;
    /**
     * TBD.
     */
    public final XirOperand resultOperand;
    /**
     * The sequence of instructions for the fast (inline) path.
     */
    public final CiXirAssembler.XirInstruction[] fastPath;
    /**
     * The sequence of instructions for the slow (out of line) path.
     */
    public final CiXirAssembler.XirInstruction[] slowPath;
    /**
     * Labels used in control transfers.
     */
    public final XirLabel[] labels;
    /**
     * Parameters to the template.
     */
    public final XirParameter[] parameters;
    /**
     * An array of same length as {@link #parameters} where {@code parameterDestroyed[i]} is {@code true}
     * iff {@code parameters[i]} is the {@link XirInstruction#result result} of any {@link XirInstruction} in either
     * {@link #fastPath} or {@link #slowPath}.
     */
    public final boolean[] parameterDestroyed;
    /**
     * Temporary variables used by the template.
     */
    public final XirTemp[] temps;
    /**
     * Constant5s used in the template.
     */
    public final XirConstant[] constants;
    /**
     * The total number of variables. (relation to temps/parameters???)
     */
    public final int variableCount;
    /**
     * TBD.
     */
    public final boolean allocateResultOperand;
    /**
     * TBD.
     */
    public final XirTemplate[] calleeTemplates;

    
    public final XirMark[] marks;
    
    public final int outgoingStackSize;
    
    public final XirOperand[] inputOperands;
    public final XirOperand[] inputTempOperands;
    public final XirOperand[] tempOperands;
    
    
    /**
     * The {@link GlobalFlags} associated with the template.
     */
    public final int flags;

    public XirTemplate(String name, int variableCount, boolean allocateResultOperand, XirOperand resultOperand, CiXirAssembler.XirInstruction[] fastPath, CiXirAssembler.XirInstruction[] slowPath, XirLabel[] labels, XirParameter[] parameters, XirTemp[] temps, XirConstant[] constantValues, int flags, XirTemplate[] calleeTemplates, XirMark[] marks, int outgoingStackSize) {
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
        this.calleeTemplates = calleeTemplates;
        this.marks = marks;
        this.outgoingStackSize = outgoingStackSize;

        assert fastPath != null;
        assert labels != null;
        assert parameters != null;

        List<XirOperand> inputOperands = new ArrayList<XirOperand>(4);
        List<XirOperand> inputTempOperands = new ArrayList<XirOperand>(4);
        List<XirOperand> tempOperands = new ArrayList<XirOperand>(4);
        
        parameterDestroyed = new boolean[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
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
            
            if (parameterDestroyed[i]) {
                inputTempOperands.add(parameters[i]);
            } else {
                inputOperands.add(parameters[i]);
            }
        }
        
        for (XirTemp temp : temps) {
            if (temp.reserve) {
                tempOperands.add(temp);
            }
        }
        
        this.inputOperands = inputOperands.toArray(new XirOperand[inputOperands.size()]);
        this.inputTempOperands = inputTempOperands.toArray(new XirOperand[inputTempOperands.size()]);
        this.tempOperands = tempOperands.toArray(new XirOperand[tempOperands.size()]);
    }

    /**
     * Convenience getter that returns the value at a given index in the {@link #parameterDestroyed} array.
     * @param index
     * @return the value at {@code parameterDestroyed[index]}
     */
    public boolean isParameterDestroyed(int index) {
        return parameterDestroyed[index];
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