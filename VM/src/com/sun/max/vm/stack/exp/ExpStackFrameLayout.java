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
package com.sun.max.vm.stack.exp;

import java.util.*;

import com.sun.max.unsafe.*;
import com.sun.max.vm.stack.exp.ExpStackWalker.*;

/**
 * This class represents a "stateless" view of a stack frame, including only the
 * operations on the stack frame that are needed by the stack frame walker. The
 * actual stack frame state (e.g the instruction pointer and stack pointer) are
 * encapsulated as a {@link ExpStackWalker.Cursor cursor} and passed to each of
 * the methods. This interface is typically implemented by a compiled method, but
 * may also be implemented by stubs, a bytecode interpreter, etc.
 *
 * @author Thomas Wuerthinger
 * @author Ben L. Titzer
 */
public interface ExpStackFrameLayout {

    /**
     * Advances the cursor to the next frame.
     * @param cursor the stack frame walking cursor
     */
    void advance(Cursor cursor);

    /**
     * Finds the address of an exception handler for the specified exception class in this
     * stack frame, if any.
     * @param cursor the cursor
     * @param throwableClass the class of the throwable
     * @return the address of the catch block, if any; {@link Address#zero()} otherwise.
     */
    Address findCatchAddress(Cursor cursor, Class<? extends Throwable> throwableClass);

    /**
     * Unwinds the stack to the specified catching address with the specified exception.
     * @param exception the exception to be thrown
     * @param cursor the cursor
     * @param address the address of the handler
     */
    void unwindToAddress(Throwable exception, Cursor cursor, Address address);

    /**
     * Prepares the reference map for this stack frame (and possibly portions of the
     * callee's frame).
     * @param current the current cursor
     * @param callee the callee's frame
     * @param preparer the reference map preparer
     */
    void prepareReferenceMap(Cursor current, Cursor callee, ExpReferenceMapPreparer preparer);

    /**
     * Produces a string description of this stack frame.
     * @param cursor the cursor
     * @return a string describing this stack frame
     */
    String description(Cursor cursor);

    /**
     * Decomposes this stack frame into Java stack frames, if any.
     * @param cursor the cursor
     * @param frames the list of frames to which to append any Java frames
     * @param debug {@code true} if the stack frame should produce debugging information (i.e.
     * deoptimization information)
     */
    void appendJavaFrames(Cursor cursor, List<ExpJavaStackFrame> frames, boolean debug);

    /**
     * Returns a pointer to the register save area, if any, on this stack frame.
     * @param cursor the cursor
     * @return a pointer to the register save area
     */
    Pointer getRegisterSaveArea(Cursor cursor);
}
