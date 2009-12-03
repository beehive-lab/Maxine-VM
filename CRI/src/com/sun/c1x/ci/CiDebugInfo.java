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
package com.sun.c1x.ci;

/**
 * This class represents the debugging information for a particular place in the code,
 * which includes the code position, a reference map, and deoptimization information.
 *
 * @author Ben L. Titzer
 */
public class CiDebugInfo {
    /**
     * The code position (including inlined all inlined methods) of this debug info.
     */
    public final CiCodePos codePos;

    /**
     * The deoptimization information for each inlined frame, if any.
     */
    public final Frame frame;

    /**
     * The reference map for the registers at this point. The reference map is <i>packed</i> in that
     * for bit {@code k} in byte {@code n}, it refers to register number {@code k + n * 8}.
     */
    public final byte[] registerRefMap;

    /**
     * The reference map for the stack frame at this point. The reference map is <i>packed</i> in that
     * for bit {@code k} in byte {@code n}, it refers to stack slot number {@code k + n * 8}.
     */
    public final byte[] stackRefMap;

    public CiDebugInfo(CiCodePos codePos, Frame frame, byte[] registerRefMap, byte[] stackRefMap) {
        this.codePos = codePos;
        this.frame = frame;
        this.registerRefMap = registerRefMap;
        this.stackRefMap = stackRefMap;
        assert frame == null || frame.codePos == codePos : "code positions do not match";
    }

    /**
     * @return {@code true} if this debug information has a debug frame
     */
    public boolean hasDebugFrame() {
        return frame != null;
    }

    /**
     * @return {@code true} if this debug info has a reference map for the registers
     */
    public boolean hasRegisterRefMap() {
        return registerRefMap != null && registerRefMap.length > 0;
    }

    /**
     * @return {@code true} if this debug info has a reference map for the stack
     */
    public boolean hasStackRefMap() {
        return stackRefMap != null && stackRefMap.length > 0;
    }

    /**
     * This class represents debug and deoptimization information for a frame,
     * including {@link CiValue locations} where to find the values of each local variable
     * and stack slot of the Java frame.
     */
    public static class Frame {
        /**
         * The debug information for the frame of the method that called this method.
         */
        public final Frame caller;

        /**
         * The code position of this frame, which includes a link to the code position of the caller method.
         */
        public final CiCodePos codePos;

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

        public Frame(Frame caller, CiCodePos codePos, CiValue[] values, int numLocals, int numStack, int numLocks) {
            this.caller = caller;
            this.codePos = codePos;
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

    }
}
