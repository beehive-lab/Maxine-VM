/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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

import java.util.*;

import com.sun.max.unsafe.*;

/**
 * Access to register state for a thread in the VM.
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
    List<MaxRegister> find(MaxMemoryRegion memoryRegion);

    /**
     * This thread's registers:  integer, floating point, and state.
     */
    List<MaxRegister> allRegisters();

    /**
     * This thread's integer registers.
     *
     * @return the integer registers; null after thread dies.
     */
    List<MaxRegister> integerRegisters();

    /**
     * This thread's floating point registers.
     *
     * @return the floating point registers; null after thread dies.
     */
    List<MaxRegister> floatingPointRegisters();

    /**
     * This thread's state registers.
     *
     * @return the state registers; null after thread dies.
     */
    List<MaxRegister> stateRegisters();

    /**
     * Visualizes a processor state registers in terms of flags.
     *
     * @param flags contents of a processor state register
     * @return a string interpreting the contents as a sequence of flags
     */
    String stateRegisterValueToString(long flags);
}
