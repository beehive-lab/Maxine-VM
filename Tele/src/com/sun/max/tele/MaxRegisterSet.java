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

import com.sun.max.collect.*;
import com.sun.max.unsafe.*;

/**
 * Access to register state for a thread in the VM.
 *
 * @author Michael Van De Vanter
 */
public interface MaxRegisterSet extends MaxEntity<MaxRegisterSet> {

    /**
     * Gets the thread that owns these registers; doesn't change.
     * <br>
     * Thread-safe
     *
     * @return the thread that owns these registers.
     */
    MaxThread thread();

    /**
     * Gets the current instruction pointer.
     * <br>
     * Thread-safe
     *
     * @return the current instruction pointer for the thread, zero if thread has died.
     */
    Pointer instructionPointer();

    /**
     * Gets the value of the register that is used as the stack pointer.
     * <br>
     * Thread-safe
     *
     * @return the current stack pointer for the thread, zero if thread has died.
     * @see #stack()
     */
    Pointer stackPointer();

    /**
     * Returns the value of the register that is used as the frame pointer.
     * <br>
     * Thread-safe
     *
     * @return the current frame pointer for the thread, zero if thread has died
     */
    Pointer framePointer();

    /**
     * Returns the value of the register that is used to make indirect calls.
     * <br>
     * Thread-safe
     *
     * @return null if there is no fixed register used to for indirect calls on the target platform
     */
    Address getCallRegisterValue();

    /**
     * Returns the registers that currently point into a region of memory in the VM.
     *
     * @param memoryRegion description of a region of memory in the VM
     * @return all registers that currently point into the region; empty if none.
     */
    Sequence<MaxRegister> find(MaxMemoryRegion memoryRegion);

    /**
     * This threads registers:  integer, floating point, and state.
     */
    Sequence<MaxRegister> allRegisters();

    /**
     * This thread's integer registers.
     *
     * @return the integer registers; null after thread dies.
     */
    Sequence<MaxRegister> integerRegisters();

    /**
     * This thread's floating point registers.
     *
     * @return the floating point registers; null after thread dies.
     */
    Sequence<MaxRegister> floatingPointRegisters();

    /**
     * This thread's state registers.
     *
     * @return the state registers; null after thread dies.
     */
    Sequence<MaxRegister> stateRegisters();

    /**
     * Visualizes a processor state registers in terms of flags.
     *
     * @param flags contents of a processor state register
     * @return a string interpreting the contents as a sequence of flags
     */
    String stateRegisterValueToString(long flags);
}
