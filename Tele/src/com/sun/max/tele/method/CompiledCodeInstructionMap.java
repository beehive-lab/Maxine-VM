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
    private List<TargetCodeInstruction> instructions = null;
    private MachineCodeLocation[] instructionLocations = null;

    /**
     * Map:  target code instruction index -> the kind of stop at the instruction, null if not a stop.
     */
    private CodeStopKind[] codeStopKinds = null;

    /**
     * Map: target code instruction index -> bytecode that compiled into code starting at this instruction, if known; else null.
     * The bytecode location may be in a different method that was inlined.
     */
    private BytecodeLocation[] bytecodeLocations = null;

    /**
     * Map: target code instruction index -> the specific opcode implemented by the group of instructions starting
     * with this one, if known; else null.
     */
    private int[] opcodes = null;

    /**
     * Map: target code instruction index -> constant pool index of {@Link MethodRefConstant} if this is a call instruction; else -1.
     */
    private int[] callees = null;

    /**
     * Unmodifiable list of indexes for instructions that are labeled.
     */
    private List<Integer> labelIndexes = null;

    private final MethodRefIndexFinder methodRefIndexFinder = new MethodRefIndexFinder();

    CompiledCodeInstructionMap(TeleVM teleVM, TeleTargetMethod teleTargetMethod) {
        super(teleVM);
        this.teleTargetMethod = teleTargetMethod;
    }

    private void initialize() {
        if (instructions == null) {
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
