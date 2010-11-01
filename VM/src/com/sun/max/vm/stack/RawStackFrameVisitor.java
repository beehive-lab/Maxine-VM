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

import com.sun.max.vm.stack.StackFrameWalker.Cursor;

/**
 * A visitor for traversing the frames on a thread's stack. This visitor avoids any allocation
 * by the stack frame walker.
 *
 * @see StackFrameVisitor
 * @author Doug Simon
 */
public abstract class RawStackFrameVisitor {

    public static final int IS_TOP_FRAME = 0x0001;
    public static final int IS_ADAPTER   = 0x0002;

    /**
     * Processes a given frame that is being traversed as part of a {@linkplain StackFrameWalker#walk stack walk}.
     *
     * @return true if the walk should continue to the caller of {@code stackFrame}, false if it should terminate now
     */
    public abstract boolean visitFrame(Cursor current, Cursor callee);
}
