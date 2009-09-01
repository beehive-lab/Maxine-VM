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

import java.util.*;

import com.sun.c1x.ci.*;

/**
 * This class represents a calling convention instance for a particular method invocation and describes the ABI for
 * outgoing arguments and the return value, both runtime calls and Java calls.
 *
 * @author Marcelo Cintra
 * @author Thomas Wuerthinger
 *
 */
public class CallingConvention {

    private int overflowArgumentsSize;
    private List<LIROperand> arguments;
    private CiLocation[] locations;

    public CallingConvention(CiLocation[] locations) {
        this.locations = locations;
        arguments = new ArrayList<LIROperand>(locations.length);
        for (CiLocation l : locations) {
            arguments.add(locationToOperand(l));

            if (l.isStackOffset()) {
                overflowArgumentsSize = Math.max(overflowArgumentsSize, l.stackOffset + l.stackSize);
            }
        }
    }

    private static LIROperand locationToOperand(CiLocation location) {
        if (location.isStackOffset()) {
            int stackOffset = location.stackOffset;
            if (location.callerStack) {
                return LIROperandFactory.address(LIROperandFactory.singleLocation(CiKind.Int, CiRegister.CallerStack), stackOffset, location.kind);
            } else {
                return LIROperandFactory.address(LIROperandFactory.singleLocation(CiKind.Int, CiRegister.Stack), stackOffset, location.kind);
            }
        } else if (location.second == null) {
            assert location.first != null;
            return new LIRLocation(location.kind, location.first);
        } else {
            assert location.first != null;
            return new LIRLocation(location.kind, location.first, location.second);
        }
    }

    public CiLocation[] locations() {
        return locations;
    }

    /**
     * Returns the number of arguments.
     *
     * @return the number of arguments
     */
    public int length() {
        return arguments.size();
    }

    /**
     * Get the LIROperand representing the argument at the specified index.
     *
     * @param i
     *            the index into the arguments
     * @return the LIROperand representing the argument
     */
    public LIROperand at(int i) {
        return arguments.get(i);
    }

    /**
     * Gets a list of the LIROperands for all the arguments.
     *
     * @return the list of arguments
     */
    public List<LIROperand> arguments() {
        return arguments;
    }

    public int overflowArgumentsSize() {
        return overflowArgumentsSize;
    }

    @Override
    public String toString() {
        StringBuffer result = new StringBuffer();
        result.append("CallingConvention[");
        for (LIROperand op : arguments) {
            result.append(op.toString() + " ");
        }
        result.append("]");
        return result.toString();
    }
}
