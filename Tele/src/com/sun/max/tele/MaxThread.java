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

import com.sun.max.annotate.*;
import com.sun.max.collect.*;
import com.sun.max.tele.debug.*;
import com.sun.max.tele.debug.TeleNativeThread.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.thread.*;


/**
 * Access to a thread in the Maxine VM.
 * This could in the future be merged with the JDWP interface.
 *
 * @author Michael Van De Vanter
 */
public interface MaxThread {

    /**
     * Gets the frames of this stack as produced by a {@linkplain StackFrameWalker stack walk}
     * when the associated thread last stopped.  Empty after thread dies.
     */
    Sequence<StackFrame> frames();

    /**
     * Have the frames of this stack changed in the epoch that completed last time this thread stopped.
     * This may be true even if the objects representing the frames have changed.
     * @see StackFrame#isSameFrame(StackFrame)
     */
    boolean framesChanged();

    /**
     * Gets the VM thread locals corresponding to a given safepoint state.
     */
    TeleThreadLocalValues threadLocalsFor(Safepoint.State state);

    /**
     * This thread's integer registers.
     *
     * @return the integer registers; null after thread dies.
     */
    TeleIntegerRegisters integerRegisters();

    /**
     * This thread's floating point registers.
     *
     * @return the floating point registers; null after thread dies.
     */
    TeleFloatingPointRegisters floatingPointRegisters();

    /**
     * This thread's state registers.
     *
     * @return the state registers; null after thread dies.
     */
    TeleStateRegisters stateRegisters();

    /**
     * Gets the breakpoint this thread is currently stopped at (if any).
     *
     * @return null if this thread is not stopped at a breakpoint or if thread has died
     */
    TeleTargetBreakpoint breakpoint();

    /**
     * @return the current state of the thread.
     */
    ThreadState state();

    /**
     * Determines if this is the primordial VM thread. The primordial VM thread is the native thread on which the VM was
     * launched. It is not associated with a {@link VmThread} instance but does have VM thread locals}.
     */
    boolean isPrimordial();

    /**
     * @return whether this thread is still alive.
     */
    boolean isLive();

    /**
     * Gets the identifier passed to {@link VmThread#run} when the thread was started. Note that this is different from
     * the platform dependent thread {@linkplain #handle() handle}.
     * <br>
     * Immutable; thread-safe.
     *
     * @return the id of this thread. A value less than, equal to or greater than 0 denotes a native thread, the
     *         primordial thread or a Java thread respectively.
     */
    int id();

    /**
     * Gets the platform dependent handle to the native thread data structure. For example, on Solaris this will be
     * the LWP identifier for this thread. This value is guaranteed to be unique for any running thread.
     * <br>
     * Immutable; thread-safe.
     */
    long handle();

    /**
     * @return the current stack pointer for the thread, zero if thread has died.
     */
    Pointer stackPointer();

    /**
     * Gets the stack for this thread.
     * <br>
     * The identity of the result is immutable and thread-safe, although its contents are not.
     *
     * @return this thread's stack
     */
    TeleNativeStack stack();

    /**
     * @return the current instruction pointer for the thread, zero if thread has died.
     */
    Pointer instructionPointer();

    /**
     * Gets the return address of the next-to-top frame on the stack. This will be null in the case where this thread is
     * in native code that was entered via a native method annotated with {@link C_FUNCTION}. The stub for such methods
     * do not leave the bread crumbs on the stack that record how to find caller frames.
     */
    Pointer getReturnAddress();

    /**
     * Gets the surrogate for the {@link VmThread} in the tele process denoted by this object.
     * This method returns {@code null} for any of the following reasons:
     *
     * 1. The thread is a non-Java thread
     * 2. The thread is a Java thread that has not yet reached the execution point (somewhere in {@link VmThread#run})
     *    that initializes {@link VmThreadLocal#VM_THREAD}.
     *
     * @return null if this thread is not (yet) associated with VmThread
     */
    MaxVMThread maxVMThread();

    /**
     * @return a printable version of the thread's internal state that only shows key aspects
     */
    String toShortString();

    /**
     * Determines if this thread is associated with a {@link VmThread} instance. Note that even if this method returns
     * {@code true}, the {@link #maxVMThread()} method will return {@code null} if the thread has not reached the
     * execution point in {@link VmThread#run} where the {@linkplain VmThreadLocal#VM_THREAD reference} to the
     * {@link VmThread} object has been initialized.
     * <br>
     * Immutable; thread-safe.
     */
    boolean isJava();

}
