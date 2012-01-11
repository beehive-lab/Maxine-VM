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

import com.sun.max.tele.method.*;
import com.sun.max.tele.object.*;
import com.sun.max.tele.util.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;

/**
 * Client access to code locations in the VM.
 */
public interface MaxCodeLocationFactory {

    /**
     * Creates a code location in the VM specified only by an abstract description a method, which may not
     * even have been loaded yet into the VM.  No explicit position information is given, so the implied position
     * is bytecode instruction 0, the method entry. When requested, attempts will be made to locate the surrogate
     * for the {@link ClassMethodActor} in the VM that identifies the method, once the class has been loaded.
     * <p>
     * Important: this location will always have {@link #bci()} = -1, which in any machine code
     * compilation is understood to mean the beginning of the method prologue, which comes before the machine
     * code deriving from bytecode instruction 0;
     * <p>
     * Thread-safe
     *
     * @param methodKey an abstract description of a method
     * @param description a human-readable description, suitable for a menu or for debugging
     * @return a new location
     * @throws TeleError if methodKey is null
     */
    MaxCodeLocation createBytecodeLocation(MethodKey methodKey, String description) throws TeleError;

    /**
     * Creates a code location in the VM specified as a position in the bytecodes representation of a method
     * in a class loaded in the VM.  Positions 0 and -1 both refer to the first bytecode instruction.  Position -1
     * in any compiled machine code representation is understood
     * to refer to the beginning of the method prologue, which is before the machine code instructions derived
     * from the first bytecode instruction.
     * <p>
     * Thread-safe
     *
     * @param teleClassMethodActor surrogate for a {@link ClassMethodActor} in the VM that identifies a method.
     * @param bci index into the method's bytecodes of a bytecode instruction
     * @param description a human-readable description, suitable for a menu or for debugging
     * @return a new location
     * @throws TeleError if teleClassMethodActor is null or bci &lt; -1
     */
    MaxCodeLocation createBytecodeLocation(TeleClassMethodActor teleClassMethodActor, int bci, String description) throws TeleError;

    /**
     * Creates a code location in VM specified as the memory address of a compiled machine code instruction.
     * <p>
     * Thread-safe
     *
     * @param address a non-zero address in VM memory that represents the beginning of a compiled machine code instruction
     * @param description a human-readable description, suitable for a menu or for debugging
     * @return a newly created location
     * @throws InvalidCodeAddressException if the address is null, zero, or points somewhere known not to contain code
     */
    MaxCodeLocation createMachineCodeLocation(Address address, String description) throws InvalidCodeAddressException;

    /**
     * Creates a code location in the VM based on both a bytecode and compiled machine code description:
     * a position in the bytecodes representation of a method in a class loaded in the VM, in addition
     * to the memory address of the corresponding machine code instruction in a specific compilation
     * of the method.
     * <p>
     * Thread-safe
     *
     * @param address an address in VM memory that represents the beginning of a compiled machine code instruction
     * @param teleClassMethodActor surrogate for a {@link ClassMethodActor} in the VM that identifies a method.
     * @param bci index into the method's bytecodes of a bytecode instruction
     * @param description a human-readable description, suitable for a menu or for debugging
     * @return a new location
     * @throws TeleError if the address is null or zero or if teleClassMethodActor is null or bci &lt; -1
     * @throws InvalidCodeAddressException if the address points into a region that should not have code in it.
     */
    MaxCodeLocation createMachineCodeLocation(Address address, TeleClassMethodActor teleClassMethodActor, int bci, String description) throws TeleError, InvalidCodeAddressException;

}
