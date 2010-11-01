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
package com.sun.cri.ci;


/**
 * A calling convention describes the locations in which the arguments for a call are placed.
 *
 * @author Marcelo Cintra
 * @author Thomas Wuerthinger
 * @author Doug Simon
 */
public class CiCallingConvention {

    /**
     * The amount of stack space (in bytes) required for the stack-based arguments of the call.
     */
    public final int stackSize;

    /**
     * The locations in which the arguments are placed. This array ordered by argument index.
     */
    public final CiValue[] locations;

    public CiCallingConvention(CiValue[] locations, int stackSize) {
        this.locations = locations;
        this.stackSize = stackSize;
        assert verify();
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("CallingConvention[");
        for (CiValue op : locations) {
            result.append(op.toString()).append(" ");
        }
        result.append("]");
        return result.toString();
    }

    private boolean verify() {
        for (int i = 0; i < locations.length; i++) {
            CiValue location = locations[i];
            assert location.isStackSlot() || location.isRegister();
        }
        return true;
    }
}
