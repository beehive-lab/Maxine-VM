/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.sun.cri.ci;

import java.io.*;
import java.util.*;

import com.sun.cri.ri.*;

/**
 * Represents the Java bytecode frame(s) at a given position
 * including {@link CiValue locations} where to find the values of each local variable
 * and stack slot of the bytecode frame(s).
 */
public class CiFrame extends CiCodePos implements Serializable {
    /**
     * An array of values representing how to reconstruct the state of the Java frame.
     * Entries
     * {@code [0 - numLocals)} represent the Java local variables,
     * {@code [numLocals, numLocals + numStack)} the Java operand stack, and entries
     * {@code [numLocals + numStack, values.length)} the list of acquired monitors.
     * Note that the number of locals and the number of stack slots may be smaller than the
     * maximum number of locals and stack slots as specified in the compiled method.
     */
    public final CiValue[] values;

    /**
     * The number of locks in the values array.
     */
    public final int numLocks;

    /**
     * The number of locals in the values array.
     */
    public final int numLocals;

    /**
     * The number of stack slots in the values array.
     */
    public final int numStack;

    public CiFrame(CiFrame caller, RiMethod method, int bci, CiValue[] values, int numLocals, int numStack, int numLocks) {
        super(caller, method, bci);
        this.values = values;
        this.numLocks = numLocks;
        this.numLocals = numLocals;
        this.numStack = numStack;
    }

    /**
     * Gets the value representing the specified local variable.
     * @param i the local variable index
     * @return the value that can be used to reconstruct the local's current value
     */
    public CiValue getLocalValue(int i) {
        return values[i];
    }

    /**
     * Gets the value representing the specified stack slot.
     * @param i the stack index
     * @return the value that can be used to reconstruct the stack slot's current value
     */
    public CiValue getStackValue(int i) {
        return values[i + numLocals];
    }

    /**
     * Gets the value representing the specified lock.
     * @param i the lock index
     * @return the value that can be used to reconstruct the lock's current value
     */
    public CiValue getLockValue(int i) {
        return values[i + numLocals + numStack];
    }

    /**
     * Gets the caller of this frame.
     *
     * @return {@code null} if this frame has no caller
     */
    public CiFrame caller() {
        return (CiFrame) caller;
    }

    /**
     * Deep equality test.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CiFrame) {
            CiFrame other = (CiFrame) obj;
            if (method.equals(other.method) &&
                other.bci == bci &&
                numLocals == other.numLocals &&
                numStack == other.numStack &&
                numLocks == other.numLocks &&
                Arrays.equals(values, other.values)) {
                if (caller == null) {
                    return other.caller == null;
                }
                return caller.equals(other.caller);
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return CiUtil.append(new StringBuilder(100), this).toString();
    }
}
