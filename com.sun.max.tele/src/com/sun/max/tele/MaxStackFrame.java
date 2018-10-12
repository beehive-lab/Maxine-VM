/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.tele;

import com.sun.max.tele.debug.*;
import com.sun.max.tele.method.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.stack.*;

/**
 * Description of a stack frame in the VM.
 */
public interface MaxStackFrame extends MaxEntity<MaxStackFrame> {

    /**
     * Gets the stack containing this frame.
     * <p>
     * Thread-safe
     *
     * @return the stack that contains this frame
     */
    MaxStack stack();

    /**
     * Gets the position of this frame in the stack.
     * <p>
     * Thread-safe
     *
     * @return the position of this frame in its stack. The top frame is at position 0;
     * @see #stack()
     */
    int position();

    /**
     * Determines whether this frame is at the top of it's stack, position.
     * <p>
     * Thread-safe
     *
     * @return whether this frame is at the top of the stack.
     */
    boolean isTop();

    /**
     * Gets this frame's Instruction Pointer.
     * <p>
     * Thread-safe
     *
     * @return the address of the next instruction to be executed in this frame.
     */
    Pointer ip();

    /**
     * Gets this frame's Stack Pointer.
     * <p>
     * Thread-safe
     *
     * @return the current stack pointer
     * @see StackFrameCursor#sp()
     */
    Pointer sp();

    /**
     * Gets this frame's Frame Pointer.
     * <p>
     * Thread-safe
     *
     * @return the current frame pointer
     * @see StackFrameCursor#fp()
     */
    Pointer fp();

    /**
     * Gets the machine code, if known, corresponding to this stack frame in the VM.
     * <p>
     * This typically would be a method compilation, but it could also be a region of native
     * that has previously been registered during the session.
     *
     * @see VmCodeCacheAccess
     */
    MaxMachineCodeRoutine machineCode();

    /**
     * Get's the conceptual location in code for each frame, independent of underlying
     * stack representation.
     * <p>
     * The location for the top frame in a stack is the instruction pointer.
     * <p>
     * The location for other frames in the stack is the return address:  the next address to
     * be executed when control returns to this frame.
     *
     * @return the conceptual code location for this frame:  IP or return pointer
     */
    MaxCodeLocation codeLocation();

    /**
     * Gets the compiled code enclosing the {@linkplain #ip() execution point} in this frame, if any.
     * <p>
     * Thread-safe
     *
     * @return compiled code for this frame, null if an external function or other special frame not associated with a method
     */
    MaxCompilation compilation();

    /**
     * Determines if this frame and another refer to the same frame.
     * <p>
     * Thread-safe
     *
     * @param stackFrame another stack frame.
     * @return whether the two refer to the same frame
     */
    boolean isSameFrame(MaxStackFrame stackFrame);

    /**
     * A stack frame holding the activation of a compiled method.
     */
    public interface Compiled extends MaxStackFrame {

        VMFrameLayout layout();

        /**
         * Gets the base address of all stack slots. This provides a convenience for stack frame visitors that need to see all stack slot as
         * located at a positive offset from some base pointer (e.g., stack inspectors etc...)
         * By default this is the frame pointer.
         */
        Pointer slotBase();

        /**
         * Computes the biased offset from the slot base to the ABI's frame pointer register.
         * <p>
         * Some platforms (e.g., Solaris SPARC v9 in 64-bit mode) use a bias from the frame pointer to access stack slot.
         * <p>
         * The default is no bias, but the offset must be adjusted by frame size to be relative to the frame pointer.
         *
         * @param offset a offset relative to the address of the stack pointer
         * @return the biased offset, relative to the frame pointer register.
         */
        int biasedFPOffset(int offset);

        /**
         * Returns the stack bias used by the stack frame. By default, it returns {@link StackBias#NONE}.
         * @return a stack bias.
         */
        StackBias bias();

        /**
         * Gets the name of the source variable corresponding to a stack slot, if any.
         *
         * @param slot a stack slot
         * @return the Java source name for the frame slot, null if not available.
         */
        String sourceVariableName(int slot);

    }

    /**
     * A stack frame holding the activation of native code about which little is known.
     */
    public interface Native extends MaxStackFrame {
    }

    /**
     * A synthetic stack frame (non-VM) used to denote an incomplete stack walk.
     */
    public interface Truncated extends MaxStackFrame {

        /**
         * The error that truncated the stack walk or {@code null} if the stack walk
         * was stopped due to the frame limit specified to {@link TeleStackFrameWalker#frames(int)}
         * being reached.
         *
         * @return a message describing the nature of the error or {@code null}
         *         if this frame indicates a truncated stack walk
         */
        Throwable error();

        /**
         * @return the number of frames omitted if the stack walk was stopped due to the
         *         frame limit being hit, -1 if it was stopped due to an error
         */
        int omitted();
    }
}
