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
package com.sun.max.tele;

import java.util.*;

import com.sun.cri.ci.*;
import com.sun.cri.ri.*;
import com.sun.max.tele.method.*;
import com.sun.max.tele.method.CodeLocation.*;
import com.sun.max.unsafe.*;


/**
 * A consistent summary of information about a body of machine code, including a number
 * of derived maps, including relationships with bytecode when available.
 * <p>
 * Thread safe.
 */
public interface MaxMachineCodeInfo {

    /**
     * @return the number of machine instructions in this code
     */
    int length();

    /**
     * @return the instruction at a specified index in this sequence
     * of instructions.
     * @throws IllegalArgumentException unless {@code 0 <= index < length()}
     */
    TargetCodeInstruction instruction(int index) throws IllegalArgumentException;

    /**
     * @return the index of the instruction whose machine code location includes
     * a specific memory location, -1 if none.
     */
    int findInstructionIndex(Address address);

    /**
     * @return the location of the instruction at a specified index
     * @throws IllegalArgumentException unless {@code 0 <= index < length()}
     */
    MachineCodeLocation instructionLocation(int index);

    /**
     * @return whether the instruction is a safepoint
     * @throws IllegalArgumentException unless {@code 0 <= index < length()}
     */
    boolean isSafepoint(int index) throws IllegalArgumentException;

    /**
     * @return whether the instruction is a call
     * @throws IllegalArgumentException unless {@code 0 <= index < length()}
     */
    boolean isCall(int index) throws IllegalArgumentException;

    /**
     * @return whether the instruction is a native call
     * @throws IllegalArgumentException unless {@code 0 <= index < length()}
     */
    boolean isNativeCall(int index) throws IllegalArgumentException;

    /**
     * @return whether the instruction is at the beginning of a sequence
     * of instructions known precisely to implement a bytecode
     * @throws IllegalArgumentException unless {@code 0 <= index < length()}
     */
    boolean isBytecodeBoundary(int index) throws IllegalArgumentException;

    /**
     * @return the debug info corresponding to the machine code instruction at the beginning
     * of a sequence of machine code instructions that implement a bytecode, if known, else null
     * @throws IllegalArgumentException unless {@code 0 <= index < length()}
     */
    CiDebugInfo debugInfoAt(int index) throws IllegalArgumentException;

    /**
     * @return the opcode corresponding to the instruction at the beginning
     * of a sequence of instructions that implement a bytecode, if known, else -1.
     * The special value of {@link Integer#MAX_VALUE} is returned to indicate
     * that the instruction is the first one in the epilogue of the method
     *
     * @throws IllegalArgumentException unless {@code 0 <= index < length()}
     */
    int opcode(int index) throws IllegalArgumentException;

    /**
     * @return if the instruction is a call, the RiMethod denoting the callee; else -1.
     * @throws IllegalArgumentException unless {@code 0 <= index < length()}
     */
    RiMethod calleeAt(int index) throws IllegalArgumentException;

    /**
     * Gets the instruction indexes of all labels synthesized by disassembly of this method.
     *
     * @return the index of instructions with labels, empty if none.
     */
    List<Integer> labelIndexes();

    // TODO (mlvdv) should abstract this interface further so this doesn't need to be exposed.
    /**
     * Builds a map from the beginning of byte code instruction to the beginning of the
     * machine code instructions generated from it.
     *
     * @return Map: bci (byte index in bytecode) -> byte positions in machine code, null if information not available
     */
    int[] bciToMachineCodePositionMap();

}
