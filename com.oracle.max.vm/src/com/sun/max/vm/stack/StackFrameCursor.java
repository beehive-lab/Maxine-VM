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

import com.sun.cri.ci.*;
import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.runtime.*;

/**
 * An encapsulation of all the state associated with a single stack frame that is needed during a stack walk,
 * including the target method, the instruction pointer, stack pointer, frame pointer, register state,
 * and whether the frame is the top frame.
 * <p>
 * As instruction pointers can be either in native code (C functions) or in managed code ({@linkplain TargetMethod
 * target methods}), the {@link NativeOrVmIP} abstraction is used to store them.
 *
 * @see StackFrameWalker
 */
public class StackFrameCursor {

    private final StackFrameWalker walker;
    final NativeOrVmIP ip;
    Pointer sp = Pointer.zero();
    Pointer fp = Pointer.zero();
    private CiCalleeSaveLayout csl;
    private Pointer csa;
    boolean isTopFrame = false;

    StackFrameCursor(StackFrameWalker walker) {
        this.walker = walker;
        this.ip  = new NativeOrVmIP();
    }

    /**
     * Alternate constructor that allows subclasses to specify an alternate implementation of code
     * pointer to use for the IP, intended for use by the Inspector.
     */
    protected StackFrameCursor(StackFrameWalker walker, NativeOrVmIP ip) {
        this.walker = walker;
        this.ip = ip;
    }

    /**
     * Updates the cursor to point to the next stack frame.
     * This method implicitly sets {@link StackFrameCursor#isTopFrame()} of this cursor to {@code false}.
     *
     * @param tm the new target method (may be {@code null})
     * @param pos the instruction offset in the target method (may be 0)
     * @param ip the new instruction pointer (may be zero)
     * @param sp the new stack pointer
     * @param fp the new frame pointer
     */
    final StackFrameCursor advance(TargetMethod tm, int pos, Pointer ip, Pointer sp, Pointer fp) {
        return setFields(tm, pos, ip, sp, fp, false);
    }

    final void reset() {
        setFields(NativeOrVmIP.ZERO, Pointer.zero(), Pointer.zero(), false);
    }

    void copyFrom(StackFrameCursor other) {
        setFields(other.ip, other.sp, other.fp, other.isTopFrame);
        setCalleeSaveArea(other.csl, other.csa);
    }

    private StackFrameCursor setFields(NativeOrVmIP nojip, Pointer sp, Pointer fp, boolean isTopFrame) {
        ip.copyFrom(nojip);
        this.sp = sp;
        this.fp = fp;
        this.isTopFrame = isTopFrame;
        this.csl = null;
        this.csa = Pointer.zero();
        return this;
    }

    private StackFrameCursor setFields(TargetMethod tm, int pos, Pointer ip, Pointer sp, Pointer fp, boolean isTopFrame) {
        this.ip.primitiveSet(tm, pos, ip);
        this.sp = sp;
        this.fp = fp;
        this.isTopFrame = isTopFrame;
        this.csl = null;
        this.csa = Pointer.zero();
        return this;
    }

    /**
     * Sets the callee save details for this frame cursor. This must be called
     * while this cursor denotes the "current" frame just before {@link StackFrameWalker#advance(Word, Word, Word)
     * is called (after which this cursor will be the "callee" frame).
     *
     * @param csl the layout of the callee save area in the frame denoted by this cursor
     * @param csa the address of the callee save area in the frame denoted by this cursor
     */
    final public void setCalleeSaveArea(CiCalleeSaveLayout csl, Pointer csa) {
        FatalError.check((csl == null) == csa.isZero(), "inconsistent callee save area info");
        this.csl = csl;
        this.csa = csa;
    }

    /**
     * @return the stack frame walker for this cursor
     */
    final public StackFrameWalker stackFrameWalker() {
        return walker;
    }

    /**
     * @return the target method corresponding to the instruction pointer.
     */
    final public TargetMethod targetMethod() {
        return ip.targetMethod();
    }

    /**
     * Gets the address of the next instruction that will be executed in this frame.
     * If this is not the {@linkplain #isTopFrame() top frame}, then this is the
     * return address saved by a call instruction. The exact interpretation of this
     * return address depends on the {@linkplain ISA#offsetToReturnPC platform}.
     *
     * The value returned from this method is valid only if the frame executes a native function.
     * Callers must be sure about calling {@link #nativeIP()} and {@link vmIP()} correctly.
     *
     * @return the current instruction pointer in a native function.
     */
    final public Pointer nativeIP() {
        return ip.nativeIP();
    }

    /**
     * Gets the address of the next instruction that will be executed in this frame.
     * If this is not the {@linkplain #isTopFrame() top frame}, then this is the
     * return address saved by a call instruction. The exact interpretation of this
     * return address depends on the {@linkplain ISA#offsetToReturnPC platform}.
     *
     * The value returned from this method is valid only if the frame executes a target method.
     * Callers must be sure about calling {@link #nativeIP()} and {@link vmIP()} correctly.
     *
     * @return the current instruction pointer in a target method.
     */
    final public CodePointer vmIP() {
        return ip.vmIP();
    }

    final public Pointer ipAsPointer() {
        return ip.asPointer();
    }

    /**
     * @return the current stack pointer.
     */
    final public Pointer sp() {
        return sp;
    }

    /**
     * @return the current frame pointer.
     */
    final public Pointer fp() {
        return fp;
    }

    /**
     * @return {@code true} if this frame is the top frame
     */
    final public boolean isTopFrame() {
        return isTopFrame;
    }

    /**
     * Gets the layout of the callee save area in this frame.
     *
     * @return {@code null} if there is no callee save area in this frame
     */
    final public CiCalleeSaveLayout csl() {
        return csl;
    }

    /**
     * Gets the address of the callee save area in this frame.
     *
     * @return {@link Pointer#zero()} if there is no callee save area in this frame
     */
    final public Pointer csa() {
        return csa;
    }
}
