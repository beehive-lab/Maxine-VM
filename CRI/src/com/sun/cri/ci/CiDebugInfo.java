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
     * The {@code long} value denoting absence of a reference map.
     */
    public static final long NO_REF_MAP = Long.MIN_VALUE;
    
    /**
     * The code position (including all inlined methods) of this debug info.
     * If this is a {@link Frame} instance, then it is also the deoptimization information for each inlined frame.
     */
    public final CiCodePos codePos;

    /**
     * The reference map for the registers at this point. The reference map is <i>packed</i> in that
     * for bit {@code k} in byte {@code n}, it refers to the register whose
     * {@linkplain CiRegister#encoding encoding} is {@code (k + n * 8)}.
     */
    public final long registerRefMap;

    /**
     * The reference map for the stack frame at this point. The reference map is <i>packed</i> in that
     * for bit {@code k} in byte {@code n}, it refers to stack slot number {@code k + n * 8}.
     */
    public final byte[] frameRefMap;

    /**
     * Creates a new {@code CiDebugInfo} from the given values.
     * 
     * @param codePos the {@linkplain CiCodePos code position} or {@linkplain Frame frame} info
     * @param registerRefMap the register map
     * @param frameRefMap the reference map for {@code frame}, which may be {@code null}
     */
    public CiDebugInfo(CiCodePos codePos, long registerRefMap, byte[] frameRefMap) {
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
        return registerRefMap != NO_REF_MAP;
    }

    /**
     * @return {@code true} if this debug info has a reference map for the stack
     */
    public boolean hasStackRefMap() {
        return frameRefMap != null && frameRefMap.length > 0;
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
