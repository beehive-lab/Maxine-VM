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

import com.sun.max.tele.debug.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.thread.*;

/**
 * Access to a thread in the Maxine VM.
 * This could in the future be merged with the JDWP interface.
 *
 * @author Michael Van De Vanter
 */
public interface MaxThread {

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
     * Gets the platform dependent handle to the native thread data structure in the VM's address space.
     * For example, on Linux this will be the pthread_self(3) value for this thread, but only becomes
     * valid once the thread has executed to the point where it is on the VM managed thread list.
     * If non-zero, this value is guaranteed to be immutable and unique among running threads.
     */
    long handle();

    /**
     * Gets the value returned by {@link #handle()} as a String.
     */
    String handleString();

    /**
     * Gets the platform dependent debug handle to the thread in the local
     * address space. For example, on Linux this will be the {@code /proc} task ID for this thread.
     * This value is guaranteed to be unique for any running thread.
     * <br>
     * Immutable; thread-safe.
     */
    long localHandle();

    /**
     * Determines if this is the primordial VM thread. The primordial VM thread is the native thread on which the VM was
     * launched. It is not associated with a {@link VmThread} instance but does have VM thread locals}.
     */
    boolean isPrimordial();

    /**
     * Determines if this thread is associated with a {@link VmThread} instance. Note that even if this method returns
     * {@code true}, the {@link #vThreadObject()} method will return {@code null} if the thread has not reached the
     * execution point in {@link VmThread#run} where the {@linkplain VmThreadLocal#VM_THREAD reference} to the
     * {@link VmThread} object has been initialized.
     * <br>
     * Immutable; thread-safe.
     */
    boolean isJava();

    /**
     * @return whether this thread is still alive.
     */
    boolean isLive();

    /**
     * @return the current state of the thread.
     */
    MaxThreadState state();

    /**
     * Gets the breakpoint this thread is currently stopped at (if any).
     *
     * @return null if this thread is not stopped at a breakpoint or if thread has died
     */
    MaxBreakpoint breakpoint();

    /**
     * Gets information about the "thread locals block" for this thread in the VM,
     * null if a non-Java thread, or the thread is very early in its creation sequence.
     * <br>
     * Thread-safe
     *
     * @return access to the thread locals block for this thread; null if not available
     */
    MaxThreadLocalsBlock localsBlock();

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
     * Current stack pointer.
     *
     * @return the current stack pointer for the thread, zero if thread has died.
     * @see #stack()
     */
    Pointer stackPointer();

    /**
     * Gets a description of the stack for this thread.
     */
    MaxStack stack();

    /**
     * @return location of the instruction pointer for the thread; null if thread has died
     */
    MaxCodeLocation instructionLocation();

    /**
     * Gets the surrogate for the heap object in the VM that implements this thread.
     * This method returns {@code null} for any of the following reasons:
     * <ol>
     * <li>The thread is a non-Java thread</li>
     * <li>The thread is a Java thread that has not yet reached the execution point (somewhere in {@link VmThread#run})
     *    that initializes {@link VmThreadLocal#VM_THREAD}.</li>
     *  </ol>
     *  Thread-safe
     *
     * @return null if this thread is not (yet) associated with a VM thread
     */
    TeleVmThread teleVmThread();

    /**
     * Gets the name for this thread in the VM, if it is a Java thread and is ready to run.
     *
     * @return the name of the associated VM thread, null if none.
     * @see vmThreadObject
     */
    String vmThreadName();

    /**
     * @return a printable version of the thread's internal state that only shows key aspects
     */
    String toShortString();

}
