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
package com.sun.c1x.lir;

import java.util.List;

/**
 * This class represents a calling convention instance for a particular method invocation and describes
 * the ABI for outgoing arguments and the return value, both runtime calls and Java calls.
 *
 * @author Marcelo Cintra
 * @author Thomas Wuerthinger
 *
 */
public class CallingConvention {

    private List<LIROperand> arguments;
    private int reservedStackSlots;

    public CallingConvention(List<LIROperand> arguments, int reservedStackSlots) {
        this.arguments = arguments;
        this.reservedStackSlots = reservedStackSlots;
    }

    /**
     * Returns the number of arguments.
     * @return the number of arguments
     */
    public int length() {
        return arguments.size();
    }

    /**
     * Get the LIROperand representing the argument at the specified index.
     * @param i the index into the arguments
     * @return the LIROperand representing the argument
     */
    public LIROperand at(int i) {
        return arguments.get(i);
    }

    /**
     * Gets a list of the LIROperands for all the arguments.
     * @return the list of arguments
     */
    public List<LIROperand> args() {
        return arguments;
    }

    public int reservedStackSlots() {
        return reservedStackSlots;
    }

    public void setArg(int i, LIROperand stack) {
        arguments.set(i, stack);

    }

    @Override
    public String toString() {
        StringBuffer result = new StringBuffer();
        result.append("CallingConvention[");
        for (LIROperand op : arguments) {
            result.append(op.toString() + " ");
        }
        result.append("reservedStack=" + reservedStackSlots);
        result.append("]");
        return result.toString();
    }

}
