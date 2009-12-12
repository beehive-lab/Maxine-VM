/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.max.vm.stack;

import com.sun.max.unsafe.*;
import com.sun.max.vm.compiler.target.*;

/**
 * A visitor for traversing the frames on a thread's stack. The details of each frame traversed in the
 * stack walk are passed as individual parameters to {@link #visitFrame(TargetMethod, Pointer, Pointer, Pointer, int)}
 * to avoid the allocation required to box them in an allocated {@link StackFrame} object.
 *
 * @see StackFrameVisitor
 * @author Doug Simon
 */
public interface RawStackFrameVisitor {

    int IS_TOP_FRAME = 0x0001;
    int IS_ADAPTER   = 0x0002;

    /**
     * Processes a given frame that is being traversed as part of a {@linkplain StackFrameWalker#walk stack walk}.
     *
     * @return true if the walk should continue to the caller of {@code stackFrame}, false if it should terminate now
     */
    boolean visitFrame(TargetMethod targetMethod, Pointer instructionPointer, Pointer stackPointer, Pointer framePointer, int flags);

    public static final class Util {

        /**
         * Creates a mask of the flags defined in {@link RawStackFrameVisitor} that is accepted by
         * {@link RawStackFrameVisitor#visitFrame(TargetMethod, Pointer, Pointer, Pointer, int)}.
         *
         * @param isTopFrame specifies if {@link RawStackFrameVisitor#IS_TOP_FRAME} should be set in the returned value
         * @param isAdapter specifies if {@link RawStackFrameVisitor#IS_ADAPTER} should be set in the returned value
         * @return a mask of flags
         */
        public static int makeFlags(boolean isTopFrame, boolean isAdapter) {
            int flags = 0;
            if (isTopFrame) {
                flags |= IS_TOP_FRAME;
            }
            if (isAdapter) {
                flags |= IS_ADAPTER;
            }
            return flags;
        }

        public static boolean isTopFrame(int flags) {
            return (flags & IS_TOP_FRAME) != 0;
        }

        public static boolean isAdapter(int flags) {
            return (flags & IS_ADAPTER) != 0;
        }
    }
}
