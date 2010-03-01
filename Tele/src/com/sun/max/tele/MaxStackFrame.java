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
package com.sun.max.tele;

import com.sun.max.unsafe.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.stack.StackFrameWalker.*;

/**
 * Description of a stack frame in the VM.
 *
 * @author Michael Van De Vanter
 */
public interface MaxStackFrame {

    /**
     * Gets the stack containing this frame.
     * <br>
     * Thread-safe
     *
     * @return the stack that contains this frame
     */
    MaxStack stack();

    /**
     * Gets the position of this frame in the stack.
     * <br>
     * Thread-safe
     *
     * @return the position of this frame in its stack. The top frame is at position 0;
     * @see #stack()
     */
    int position();

    /**
     * Determines whether this frame is at the top of it's stack, position.
     * <br>
     * Thread-safe
     *
     * @return whether this frame is at the top of the stack.
     */
    boolean isTop();

    /**
     * Gets this frame's Instruction Pointer.
     * <br>
     * Thread-safe
     *
     * @return the address of the next instruction to be executed in this frame.
     * @see Cursor#ip()
     */
    Pointer ip();

    /**
     * Gets this frame's Stack Pointer.
     * <br>
     * Thread-safe
     *
     * @return the current stack pointer
     * @see Cursor#sp()
     */
    Pointer sp();

    /**
     * Gets this frame's Frame Pointer.
     * <br>
     * Thread-safe
     *
     * @return the current frame pointer
     * @see Cursor#fp()
     */
    Pointer fp();

    /**
     * Get's the conceptual location in code for each frame, independent of underlying
     * stack representation.
     * <br>
     * The location for the top frame in a stack is the instruction pointer.
     * <br>
     * The location for other frames in the stack is the return address:  the next address to
     * be executed when control returns to this frame.
     *
     * @return the conceptual code location for this frame:  IP or return pointer
     */
    MaxCodeLocation codeLocation();

    /**
     * Gets the compiled method enclosing the {@linkplain #ip() execution point} in this frame, if any.
     * <br>
     * Thread-safe
     *
     * @return null if this is a frame of a native function or other special frame not associated with a method
     */
    TargetMethod targetMethod();

    /**
     * Determines if this frame and another refer to the same frame.
     * <br>
     * Thread-safe
     *
     * @param stackFrame another stack frame.
     * @return whether the two refer to the same frame
     */
    boolean isSameFrame(MaxStackFrame stackFrame);

    /**
     * @return a human-readable description of this frame, suitable for debugging
     */
    String description();

    /**
     * A stack frame holding the activation of a compiled method.
     */
    public static interface Compiled extends MaxStackFrame {

        CompiledStackFrameLayout layout();

        /**
         * Gets the base address of all stack slots. This provides a convenience for stack frame visitors that need to see all stack slot as
         * located at a positive offset from some base pointer (e.g., stack inspectors etc...)
         * By default this is the frame pointer.
         */
        Pointer slotBase();

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
        Offset biasedFPOffset(Offset offset);

        /**
         * Returns the stack bias used by the stack frame. By default, it returns {@link StackBias#NONE}.
         * @return a stack bias.
         */
        StackBias bias();

    }

    /**
     * A stack frame holding the activation of native code about which little is known.
     */
    public static interface Native extends MaxStackFrame {
    }

    /**
     * A synthetic stack frame (non-VM) used to record an error
     * encountered during stack walking.
     */
    public static interface Error extends MaxStackFrame {

        /**
         * Description of an error encountered during stack walking.
         *
         * @return a message describing the nature of the error
         */
        String errorMessage();
    }
}
