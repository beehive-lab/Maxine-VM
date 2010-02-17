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

import com.sun.max.program.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;

/**
 * Client access to code in the VM.
 *
 * @author Michael Van De Vanter
 */

public interface MaxCodeManager {

    /**
     * Creates a code location in the VM specified only by an abstract description a method, which may not
     * even have been loaded yet into the VM.  No explicit position information is given, so the implied position
     * is bytecode instruction 0, the method entry. When requested, attempts will be made to locate the surrogate
     * for the {@link ClassMethodActor} in the VM that identifies the method, once the class has been loaded.
     * <br>
     * Important: this location will always have {@link #bytecodePosition()} = -1, which in any machine code
     * compilation is understood to mean the beginning of the method prologue, which comes before the machine
     * code deriving from bytecode instruction 0;
     * <br>
     * Thread-safe
     *
     * @param methodKey an abstract description of a method
     * @param description a human-readable description, suitable for a menu or for debugging
     * @return a new location
     * @throws ProgramError if methodKey is null
     */
    MaxCodeLocation createBytecodeLocation(MethodKey methodKey, String description) throws ProgramError;

    /**
     * Creates a code location in the VM specified as a position in the bytecodes representation of a method
     * in a class loaded in the VM.  Positions 0 and -1 both refer to the first bytecode instruction.  Position -1
     * in any compiled machine code representation is understood
     * to refer to the beginning of the method prologue, which is before the machine code instructions derived
     * from the first bytecode instruction.
     * <br>
     * Thread-safe
     *
     * @param teleClassMethodActor surrogate for a {@link ClassMethodActor} in the VM that identifies a method.
     * @param bytecodePosition offset into the method's bytecodes of a bytecode instruction
     * @param description a human-readable description, suitable for a menu or for debugging
     * @return a new location
     * @throws ProgramError if teleClassMethodActor is null or bytecodePosition &lt; -1
     */
    MaxCodeLocation createBytecodeLocation(TeleClassMethodActor teleClassMethodActor, int bytecodePosition, String description) throws ProgramError;

    /**
     * Creates a code location in VM specified as the memory address of a compiled machine code instruction.
     * <br>
     * Thread-safe
     *
     * @param address a non-zero address in VM memory that represents the beginning of a compiled machine code instruction
     * @param description a human-readable description, suitable for a menu or for debugging
     * @return a newly created location
     * @throws ProgramError if the address is null or zero
     */
    MaxCodeLocation createMachineCodeLocation(Address address, String description) throws ProgramError;

    /**
     * Creates a code location in the VM based on both a bytecode and compiled machine code description:
     * a position in the bytecodes representation of a method in a class loaded in the VM, in addition
     * to the memory address of the corresponding machine code instruction in a specific compilation
     * of the method.
     * <br>
     * Thread-safe
     *
     * @param address an address in VM memory that represents the beginning of a compiled machine code instruction
     * @param teleClassMethodActor surrogate for a {@link ClassMethodActor} in the VM that identifies a method.
     * @param bytecodePosition offset into the method's bytecodes of a bytecode instruction
     * @param description a human-readable description, suitable for a menu or for debugging
     * @return a new location
     * @throws ProgramError if the address is null or zero or  if teleClassMethodActor is null or bytecodePosition &lt; -1
     */
    MaxCodeLocation createMachineCodeLocation(Address address, TeleClassMethodActor teleClassMethodActor, int bytecodePosition, String description) throws ProgramError;

}
