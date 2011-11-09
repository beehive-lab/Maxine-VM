/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.stack;

import com.sun.max.unsafe.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.stack.StackFrameWalker.Cursor;

/**
 * A {@code StackFrame} object abstracts an activation frame on a call stack.
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
