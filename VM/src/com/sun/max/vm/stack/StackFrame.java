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
import com.sun.max.vm.stack.StackFrameWalker.*;

/**
 * A {@code StackFrame} object abstracts an activation frame on a call stack.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public abstract class StackFrame {

    /**
     * An address indicating (but not necessarily equal to) the next instruction to be executed in this frame.
     *
     * @see Cursor#ip()
     */
    public final Pointer ip;
    public final Pointer sp;
    public final Pointer fp;
    public final StackFrame callee;
    private StackFrame caller;

    protected StackFrame(StackFrame callee, Pointer ip, Pointer sp, Pointer fp) {
        this.callee = callee;
        if (callee != null) {
            callee.caller = this;
        }
        this.ip = ip;
        this.fp = fp;
        this.sp = sp;
    }

    /**
     * Gets the target method enclosing the {@linkplain #ip execution point} in this frame.
     *
     * @return null if this is a frame of a native function
     */
    public TargetMethod targetMethod() {
        return null;
    }

    /**
     * Gets the base address of all stack slots. This provides a convenience for stack frame visitors that need to see all stack slot as
     * located at a positive offset from some base pointer (e.g., stack inspectors etc...)
     * By default this is the frame pointer.
     */
    public Pointer slotBase() {
        return fp;
    }

    /**
     * Computes the biased offset from the slot base to the ABI's frame pointer register.
     * <br>
     * Some platforms (e.g., Solaris SPARC v9 in 64-bit mode) use a bias from the frame pointer to access stack slot.
     * <br>
     * The default is no bias, but the offset must be adjusted by frame size to be relative to the frame pointer.
     *
     * @param offset a offset relative to the address of the stack pointer
     * @return the biased offset, relative to the frame pointer register.
     */
    public Offset biasedFPOffset(Offset offset) {
        if (targetMethod() != null) {
            return offset.minus(targetMethod().frameSize());
        }
        return offset;
    }

    /**
     * Returns the stack bias used by the stack frame. By default, it returns {@link StackBias#NONE}.
     * @return a stack bias.
     */
    public StackBias bias() {
        return StackBias.NONE;
    }

    /**
     * Gets the frame called by this frame.
     *
     * @return null if this is the {@linkplain #isTopFrame() top} frame
     */
    public final StackFrame calleeFrame() {
        return callee;
    }

    /**
     * Gets the frame from which this frame was called.
     *
     * @return null if this is the bottom frame (i.e. the last frame traversed in a {@linkplain StackFrameWalker stack walk}).
     */
    public final StackFrame callerFrame() {
        return caller;
    }

    /**
     * Determines if this is the top frame. The top frame is the first frame traversed in a {@linkplain StackFrameWalker stack walk}.
     */
    public boolean isTopFrame() {
        return callee == null;
    }

    /**
     * Indicates if a given stack frame denotes the same frame as this object.
     */
    public abstract boolean isSameFrame(StackFrame stackFrame);

    @Override
    public abstract String toString();
}
