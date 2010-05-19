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

import java.io.*;

import com.sun.max.collect.*;
import com.sun.max.tele.method.*;
import com.sun.max.tele.method.CodeLocation.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.bytecode.BytecodeLocation;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.cps.target.*;

/**
 * Description of a single machine code routine in the VM, either compiled from a Java method or a block of external native code.
 *
 * @author Michael Van De Vanter
 */
public interface MaxMachineCode<MachineCode_Type extends MaxMachineCode> extends MaxEntity<MachineCode_Type> {

    /**
     * @return VM address of the first instruction in the machine code represented by this routine. Note that this
     * may differ from the designated {@linkplain #getCallEntryLocation() entry point} of the code.
     */
    Address getCodeStart();

    /**
     * @return VM location of the first instruction in the machine code represented by this routine. Note that this
     *         may differ from the designated {@linkplain #getCallEntryLocation() call entry location} of the code.
     */
    CodeLocation getCodeStartLocation();

    /**
     * Gets the compiled entry point location for this code, which in the case of a compiled method is the
     * entry specified by the ABI in use when compiled.
     *
     * @return {@link Address#zero()} if not yet been compiled
     */
    CodeLocation getCallEntryLocation();

    /**
     * @return meta-information about the machine code instructions
     */
    InstructionMap instructionMap();

    /**
     * Gets the human-readable name of a data location that can be addressed by machine
     * code instructions in this method.
     *
     * @param targetLocation
     * @return the name of the data location
     */
    String targetLocationToString(TargetLocation targetLocation);

    /**
     * Writes a textual disassembly of the machine code instructions.
     */
    void writeSummary(PrintStream printStream);

    public interface InstructionMap {

        /**
         * @return the number of machine instructions in this map
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
         * @return whether the instruction is a stop
         * @throws IllegalArgumentException unless {@code 0 <= index < length()}
         */
        boolean isStop(int index) throws IllegalArgumentException;

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
         * @return the bytecode location corresponding to the instruction at the beginning
         * of a sequence of instructions that implement a bytecode, if known, else null
         * @throws IllegalArgumentException unless {@code 0 <= index < length()}
         */
        BytecodeLocation bytecodeLocation(int index) throws IllegalArgumentException;

        /**
         * @return the target frame descriptor for the instruction; null if none.
         *
         * @throws IllegalArgumentException unless {@code 0 <= index < length()}
         */
        TargetJavaFrameDescriptor targetFrameDescriptor(int index) throws IllegalArgumentException;

        /**
         * @return the opcode corresponding the instruction at the beginning
         * of a sequence of instructions that implement a bytecode, if known, else -1.
         *
         * @throws IllegalArgumentException unless {@code 0 <= index < length()}
         */
        int opcode(int index) throws IllegalArgumentException;

        /**
         * @return if the instruction is a call, the index into the constant pool of the operand/callee; else -1.
         * @throws IllegalArgumentException unless {@code 0 <= index < length()}
         */
        int calleeConstantPoolIndex(int index) throws IllegalArgumentException;

        /**
         * Gets the instruction indexes of all labels synthesized by disassembly of this method.
         *
         * @return the index of instructions with labels, empty if none.
         */
        Sequence<Integer> labelIndexes();

        // TODO (mlvdv) abstract this interface further to this doesn't need to be exposed.
        int[] bytecodeToTargetCodePositionMap();

    }

}
