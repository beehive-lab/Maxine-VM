/*
 * Copyright (c) 2009, 2010, Oracle and/or its affiliates. All rights reserved.
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

import java.util.*;

import com.sun.cri.ri.*;


/**
 * Represents the debugging information for a particular place in the code,
 * which includes the code position, a reference map, and deoptimization information.
 *
 * @author Ben L. Titzer
 */
public class CiDebugInfo {
    
    /**
     * The code position (including all inlined methods) of this debug info.
     * If this is a {@link Frame} instance, then it is also the deoptimization information for each inlined frame.
     */
    public final CiCodePos codePos;

    /**
     * The reference map for the registers at this point. The reference map is <i>packed</i> in that
     * for bit {@code k} in byte {@code n}, it refers to the register whose
     * {@linkplain CiRegister#number number} is {@code (k + n * 8)}.
     */
    public final CiBitMap registerRefMap;

    /**
     * The reference map for the stack frame at this point. A set bit at {@code k} in the map
     * represents stack slot number {@code k}.
     */
    public final CiBitMap frameRefMap;

    /**
     * Creates a new {@code CiDebugInfo} from the given values.
     * 
     * @param codePos the {@linkplain CiCodePos code position} or {@linkplain Frame frame} info
     * @param registerRefMap the register map
     * @param frameRefMap the reference map for {@code frame}, which may be {@code null}
     */
    public CiDebugInfo(CiCodePos codePos, CiBitMap registerRefMap, CiBitMap frameRefMap) {
        this.codePos = codePos;
        this.registerRefMap = registerRefMap;
        this.frameRefMap = frameRefMap;
    }

    /**
     * @return {@code true} if this debug information has a frame
     */
    public boolean hasFrame() {
        return codePos instanceof Frame;
    }

    /**
     * @return {@code true} if this debug info has a reference map for the registers
     */
    public boolean hasRegisterRefMap() {
        return registerRefMap != null && registerRefMap.size() > 0;
    }

    /**
     * @return {@code true} if this debug info has a reference map for the stack
     */
    public boolean hasStackRefMap() {
        return frameRefMap != null && frameRefMap.size() > 0;
    }


    /**
     * Gets the deoptimization information for each inlined frame (if available).
     * 
     * @return {@code null} if no frame de-opt info is {@linkplain #hasDebugFrame available}
     */
    public Frame frame() {
        if (hasFrame()) {
            return (Frame) codePos;
        }
        return null;
    }

    @Override
    public String toString() {
        return CiUtil.append(new StringBuilder(100), this).toString();
    }
    
    /**
     * Represents debug and deoptimization information for a frame,
     * including {@link CiValue locations} where to find the values of each local variable
     * and stack slot of the Java frame.
     */
    public static class Frame extends CiCodePos {
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

        public Frame(Frame caller, RiMethod method, int bci, CiValue[] values, int numLocals, int numStack, int numLocks) {
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
        public Frame caller() {
            return (Frame) caller;
        }
        
        /**
         * Deep equality test.
         */
        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Frame) {
                Frame other = (Frame) obj;
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
}
