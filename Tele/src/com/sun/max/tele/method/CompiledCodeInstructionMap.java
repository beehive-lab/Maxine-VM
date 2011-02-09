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
package com.sun.max.tele.method;

import java.util.*;

import com.sun.cri.bytecode.*;
import com.sun.max.tele.*;
import com.sun.max.tele.MaxMachineCode.*;
import com.sun.max.tele.method.CodeLocation.*;
import com.sun.max.tele.object.*;
import com.sun.max.tele.object.TeleTargetMethod.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.bytecode.BytecodeLocation;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.cps.target.*;

/**
 * Summary information about a sequence of disassembled machine code instructions
 * that were compiled by the VM from a method, stub, adapter, or other routine.
 * <br>
 * Full initialization is lazy, so that it isn't done for all the methods discovered
 * in the VM, but only those for which we want all the details about the code.
 *
 * @author Michael Van De Vanter
 */
public class CompiledCodeInstructionMap extends AbstractTeleVMHolder implements InstructionMap {

    private static final List<TargetCodeInstruction> EMPTY_TARGET_INSTRUCTIONS =
        Collections.unmodifiableList(new ArrayList<TargetCodeInstruction>(0));
    private static final MachineCodeLocation[] EMPTY_MACHINE_CODE_LOCATIONS = new MachineCodeLocation[0];
    private static final CodeStopKind[] EMPTY_CODE_STOP_KINDS = new CodeStopKind[0];
    private static final BytecodeLocation[] EMPTY_BYTECODE_LOCATIONS = new BytecodeLocation[0];
    private static final int[] EMPTY_INT_ARRAY = new int[0];
    private static final ArrayList<Integer> EMPTY_INTEGER_LIST = new ArrayList<Integer>(0);

    /**
     * Adapter for bytecode scanning that only knows the constant pool
     * index argument of the last method invocation instruction scanned.
     */
    private static final class MethodRefIndexFinder extends BytecodeAdapter  {

        int methodRefIndex = -1;

        public MethodRefIndexFinder reset() {
            methodRefIndex = -1;
            return this;
        }

        @Override
        protected void invokestatic(int index) {
            methodRefIndex = index;
        }

        @Override
        protected void invokespecial(int index) {
            methodRefIndex = index;
        }

        @Override
        protected void invokevirtual(int index) {
            methodRefIndex = index;
        }

        @Override
        protected void invokeinterface(int index, int count) {
            methodRefIndex = index;
        }

        public int methodRefIndex() {
            return methodRefIndex;
        }
    };

    private final TeleTargetMethod teleTargetMethod;
    private List<TargetCodeInstruction> instructions = EMPTY_TARGET_INSTRUCTIONS;
    private MachineCodeLocation[] instructionLocations = EMPTY_MACHINE_CODE_LOCATIONS;

    /**
     * Map:  target code instruction index -> the kind of stop at the instruction, null if not a stop.
     */
    private CodeStopKind[] codeStopKinds = EMPTY_CODE_STOP_KINDS;

    /**
     * Map: target code instruction index -> bytecode that compiled into code starting at this instruction, if known; else null.
     * The bytecode location may be in a different method that was inlined.
     */
    private BytecodeLocation[] bytecodeLocations = EMPTY_BYTECODE_LOCATIONS;

    /**
     * Map: target code instruction index -> the specific opcode implemented by the group of instructions starting
     * with this one, if known; else null.
     */
    private int[] opcodes = EMPTY_INT_ARRAY;

    /**
     * Map: target code instruction index -> constant pool index of {@Link MethodRefConstant} if this is a call instruction; else -1.
     */
    private int[] callees = EMPTY_INT_ARRAY;

    /**
     * Unmodifiable list of indexes for instructions that are labeled.
     */
    private List<Integer> labelIndexes = EMPTY_INTEGER_LIST;

    private final MethodRefIndexFinder methodRefIndexFinder = new MethodRefIndexFinder();

    public CompiledCodeInstructionMap(TeleVM teleVM, TeleTargetMethod teleTargetMethod) {
        super(teleVM);
        this.teleTargetMethod = teleTargetMethod;
    }

    private void initialize() {
        if (instructions.isEmpty()) {
            if (vm().tryLock()) {
                try {
                    instructions = teleTargetMethod.getInstructions();
                    final int instructionCount = instructions.size();

                    byte[] bytecodes = null;
                    final TeleClassMethodActor teleClassMethodActor = teleTargetMethod.getTeleClassMethodActor();
                    if (teleClassMethodActor != null) {
                        final TeleCodeAttribute teleCodeAttribute = teleClassMethodActor.getTeleCodeAttribute();
                        if (teleCodeAttribute != null) {
                            bytecodes = teleCodeAttribute.readBytecodes();
                        }
                    }

                    // First, gather general information from target method, indexed by
                    // bytecode positions of instructions (byte offset from code start)
                    final int targetCodeLength = teleTargetMethod.getCodeLength();
                    final CodeStopKind[] positionToStopKindMap = teleTargetMethod.getPositionToStopKindMap();
                    final BytecodeLocation[] positionToBytecodeLocationMap = teleTargetMethod.getPositionToBytecodeLocationMap();

                    // Non-null if we have a precise map between bytecode and machine code instructions
                    final int[] bytecodeToTargetCodePositionMap = teleTargetMethod.getBytecodeToTargetCodePositionMap();

                    // Fill in maps indexed by instruction count
                    instructionLocations = new MachineCodeLocation[instructionCount];
                    codeStopKinds = new CodeStopKind[instructionCount];
                    bytecodeLocations = new BytecodeLocation[instructionCount];
                    opcodes = new int[instructionCount];
                    Arrays.fill(opcodes, -1);
                    callees = new int[instructionCount];
                    Arrays.fill(callees, -1);

                    // Fill in list of labels (index of instruction)
                    final List<Integer> labels = new ArrayList<Integer>();

                    int bytecodeIndex = 0; // position cursor in the original bytecode stream, used if we have a bytecode-> machine code map
                    for (int index = 0; index < instructionCount; index++) {
                        final TargetCodeInstruction instruction = instructions.get(index);
                        instructionLocations[index] = codeManager().createMachineCodeLocation(instruction.address, "native target code instruction");
                        if (instruction.label != null) {
                            labels.add(index);
                        }

                        // offset in bytes of this machine code instruction from beginning
                        final int position = instruction.position;

                        // Ensure that the reported instruction position is legitimate.
                        // The disassembler sometimes seems to report wild positions
                        // when disassembling random binary; this can happen when
                        // viewing some unknown native code whose length we must guess.
                        if (position < 0 || position >= targetCodeLength) {
                            continue;
                        }

                        if (positionToBytecodeLocationMap != null) {
                            bytecodeLocations[index] = positionToBytecodeLocationMap[position];
                        }

                        if (positionToStopKindMap != null) {
                            final CodeStopKind codeStopKind = positionToStopKindMap[position];
                            if (codeStopKind != null) {
                                // We're at a stop
                                codeStopKinds[index] = codeStopKind;
                                final BytecodeLocation bytecodeLocation = bytecodeLocations[index];
                                // TODO (mlvdv) only works for non-inlined calls
                                if (bytecodeLocation != null && bytecodeLocation.classMethodActor.equals(teleTargetMethod.classMethodActor()) && bytecodeLocation.bytecodePosition >= 0) {
                                    callees[index] = findCalleeIndex(bytecodes, bytecodeLocation.bytecodePosition);
                                }
                            }
                        }
                        if (bytecodeToTargetCodePositionMap != null) {
                            // Add more information if we have a precise map from bytecode to machine code instructions
                            final int bytecodePosition = bytecodeIndex;
                            // To check if we're crossing a bytecode boundary in the JITed code, compare the offset of the instruction at the current row with the offset recorded by the JIT
                            // for the start of bytecode template.
                            if (bytecodePosition < bytecodeToTargetCodePositionMap.length &&
                                            position == bytecodeToTargetCodePositionMap[bytecodePosition]) {
                                // This is the start of the machine code block implementing the next bytecode
                                int opcode = Bytes.beU1(bytecodes, bytecodeIndex);
                                if (opcode == Bytecodes.WIDE) {
                                    opcode = Bytes.beU1(bytecodes, bytecodeIndex + 1);
                                }
                                opcodes[index] = opcode;
                                // Move bytecode position cursor to start of next instruction
                                do {
                                    ++bytecodeIndex;
                                } while (bytecodeIndex < bytecodeToTargetCodePositionMap.length &&
                                                bytecodeToTargetCodePositionMap[bytecodeIndex] == 0);
                            }
                        }
                    }
                    labelIndexes = Collections.unmodifiableList(labels);
                } finally {
                    vm().unlock();
                }
            }
        }
    }

    /**
     * @param bytecodes
     * @param bytecodePosition byte offset into bytecodes
     * @return if a call instruction, the index into the constant pool of the called {@link MethodRefConstant}; else -1.
     */
    private int findCalleeIndex(byte[] bytecodes, int bytecodePosition) {
        if (bytecodes == null || bytecodePosition >= bytecodes.length) {
            return -1;
        }
        final BytecodeScanner bytecodeScanner = new BytecodeScanner(methodRefIndexFinder.reset());
        bytecodeScanner.scanInstruction(bytecodes, bytecodePosition);
        return methodRefIndexFinder.methodRefIndex();
    }

    public int length() {
        initialize();
        return instructions.size();
    }

    public TargetCodeInstruction instruction(int index) throws IllegalArgumentException {
        initialize();
        if (index < 0 || index >= instructions.size()) {
            throw new IllegalArgumentException();
        }
        return instructions.get(index);
    }

    public int findInstructionIndex(Address address) {
        initialize();
        final int length = instructions.size();
        if (address.greaterEqual(instructions.get(0).address)) {
            for (int index = 1; index < length; index++) {
                instructions.get(index);
                if (address.lessThan(instructions.get(index).address)) {
                    return index - 1;
                }
            }
            final TargetCodeInstruction lastInstruction = instructions.get(instructions.size() - 1);
            if (address.lessThan(lastInstruction.address.plus(lastInstruction.bytes.length))) {
                return length - 1;
            }
        }
        return -1;
    }
    public MachineCodeLocation instructionLocation(int index) {
        initialize();
        if (index < 0 || index >= instructions.size()) {
            throw new IllegalArgumentException();
        }
        return instructionLocations[index];
    }

    public boolean isStop(int index) throws IllegalArgumentException {
        initialize();
        if (index < 0 || index >= instructions.size()) {
            throw new IllegalArgumentException();
        }
        return codeStopKinds[index] != null;
    }

    public boolean isCall(int index) throws IllegalArgumentException {
        initialize();
        if (index < 0 || index >= instructions.size()) {
            throw new IllegalArgumentException();
        }
        final CodeStopKind stopKind = codeStopKinds[index];
        return stopKind != null && stopKind != CodeStopKind.SAFE;
    }

    public boolean isNativeCall(int index) throws IllegalArgumentException {
        initialize();
        if (index < 0 || index >= instructions.size()) {
            throw new IllegalArgumentException();
        }
        final CodeStopKind stopKind = codeStopKinds[index];
        return stopKind == CodeStopKind.NATIVE_CALL;
    }

    public boolean isBytecodeBoundary(int index) throws IllegalArgumentException {
        initialize();
        if (index < 0 || index >= instructions.size()) {
            throw new IllegalArgumentException();
        }
        return opcodes[index] >= 0;
    }

    public BytecodeLocation bytecodeLocation(int index) throws IllegalArgumentException {
        initialize();
        if (index < 0 || index >= instructions.size()) {
            throw new IllegalArgumentException();
        }
        return bytecodeLocations[index];
    }

    public TargetJavaFrameDescriptor targetFrameDescriptor(int index) throws IllegalArgumentException {
        initialize();
        if (index < 0 || index >= instructions.size()) {
            throw new IllegalArgumentException();
        }
        return teleTargetMethod.getTargetFrameDescriptor(index);
    }

    public int opcode(int index) throws IllegalArgumentException {
        if (index < 0 || index >= instructions.size()) {
            throw new IllegalArgumentException();
        }
        return opcodes[index];
    }

    public int calleeConstantPoolIndex(int index) {
        initialize();
        if (index < 0 || index >= instructions.size()) {
            throw new IllegalArgumentException();
        }
        return callees[index];
    }

    public List<Integer> labelIndexes() {
        initialize();
        return labelIndexes;
    }

    public int[] bytecodeToTargetCodePositionMap() {
        return teleTargetMethod.getBytecodeToTargetCodePositionMap();
    }

}
