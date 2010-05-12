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
import com.sun.max.jdwp.vm.proxy.*;
import com.sun.max.program.*;
import com.sun.max.tele.memory.*;
import com.sun.max.tele.method.*;
import com.sun.max.tele.method.CodeLocation.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.BytecodeLocation;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.cps.target.*;

/**
 * Holds information about a block of code in
 * the process of the VM, about which little is known
 * other than the memory location and possibly an assigned name.
 *
 * @author Michael Van De Vanter
 */
public final class TeleCompiledNativeCode extends TeleCompiledCode {

    public static final Size DEFAULT_NATIVE_CODE_LENGTH = Size.fromInt(200);

    /**
     * Description of a region of native code discovered with the VM.
     * <br>
     * This region has no parent, as little is known about it.
     * <br>
     * This region has no children.
     *
     * @author Michael Van De Vanter
     */
    private static final class CompiledNativeCodeMemoryRegion extends TeleFixedMemoryRegion implements MaxEntityMemoryRegion<MaxCompiledCode> {

        private static final IndexedSequence<MaxEntityMemoryRegion<? extends MaxEntity>> EMPTY =
            new ArrayListSequence<MaxEntityMemoryRegion<? extends MaxEntity>>(0);

        private TeleCompiledCode owner;

        private CompiledNativeCodeMemoryRegion(TeleVM teleVM, TeleCompiledNativeCode owner, String name, Address start, Size size) {
            super(teleVM, name, start, size);
            this.owner = owner;
        }

        public MaxEntityMemoryRegion< ? extends MaxEntity> parent() {
            return null;
        }

        public IndexedSequence<MaxEntityMemoryRegion< ? extends MaxEntity>> children() {
            return EMPTY;
        }

        public MaxCompiledCode owner() {
            return owner;
        }

        public boolean isBootRegion() {
            return false;
        }
    }

    /**
     * Summary information about a sequence of disassembled machine code instructions about
     * which little is known.
     *
     * @author Michael Van De Vanter
     */
    private final class CompiledNativeCodeInstructionMap implements InstructionMap {

        private IndexedSequence<MachineCodeLocation> instructionLocations = null;
        private IndexedSequence<Integer> labelIndexes = null;

        CompiledNativeCodeInstructionMap() {
        }

        private void initialize() {
            if (instructions == null) {
                instructions = getInstructions();
                final int length = instructions.length();
                final VariableSequence<MachineCodeLocation> locations = new VectorSequence<MachineCodeLocation>(length);
                final VariableSequence<Integer> labels = new ArrayListSequence<Integer>();
                for (int index = 0; index < length; index++) {
                    final TargetCodeInstruction targetCodeInstruction = instructions.get(index);
                    locations.append(codeManager().createMachineCodeLocation(targetCodeInstruction.address, "native target code instruction"));
                    if (targetCodeInstruction.label != null) {
                        labels.append(index);
                    }
                }
                instructionLocations = locations;
                labelIndexes = labels;
            }
        }

        public int length() {
            initialize();
            return instructions.length();
        }

        public TargetCodeInstruction instruction(int index) throws IllegalArgumentException {
            initialize();
            if (index < 0 || index >= instructions.length()) {
                throw new IllegalArgumentException();
            }
            return instructions.get(index);
        }

        public int findInstructionIndex(Address address) {
            if (address != null) {
                initialize();
                final int length = instructions.length();
                if (address.greaterEqual(instructions.first().address)) {
                    for (int index = 1; index < length; index++) {
                        instructions.get(index);
                        if (address.lessThan(instructions.get(index).address)) {
                            return index - 1;
                        }
                    }
                    final TargetCodeInstruction lastInstruction = instructions.last();
                    if (address.lessThan(lastInstruction.address.plus(lastInstruction.bytes.length))) {
                        return length - 1;
                    }
                }
            }
            return -1;
        }

        public MachineCodeLocation instructionLocation(int index) {
            initialize();
            if (index < 0 || index >= instructions.length()) {
                throw new IllegalArgumentException();
            }
            return instructionLocations.get(index);
        }

        public boolean isStop(int index) throws IllegalArgumentException {
            if (index < 0 || index >= instructions.length()) {
                throw new IllegalArgumentException();
            }
            return false;
        }

        public boolean isCall(int index) throws IllegalArgumentException {
            // TODO (mlvdv) how to determine this?
            return false;
        }

        public boolean isNativeCall(int index) throws IllegalArgumentException {
            return isCall(index);
        }

        public boolean isBytecodeBoundary(int index) throws IllegalArgumentException {
            if (index < 0 || index >= instructions.length()) {
                throw new IllegalArgumentException();
            }
            return false;
        }

        public BytecodeLocation bytecodeLocation(int index) throws IllegalArgumentException {
            if (index < 0 || index >= instructions.length()) {
                throw new IllegalArgumentException();
            }
            return null;
        }

        public TargetJavaFrameDescriptor targetFrameDescriptor(int index) throws IllegalArgumentException {
            return null;
        }

        public int opcode(int index) throws IllegalArgumentException {
            if (index < 0 || index >= instructions.length()) {
                throw new IllegalArgumentException();
            }
            return -1;
        }

        public int calleeConstantPoolIndex(int index) throws IllegalArgumentException {
            if (index < 0 || index >= instructions.length()) {
                throw new IllegalArgumentException();
            }
            return -1;
        }

        public IndexedSequence<Integer> labelIndexes() {
            initialize();
            return labelIndexes;
        }

        public int[] bytecodeToTargetCodePositionMap() {
            return null;
        }
    }

    /**
     * @param name a name for the region
     * @return a newly created surrogate for a block of native code discovered in the VM
     * about which little more is known than its location.  The location must not overlap any code
     * region already known.
     */
    public static TeleCompiledNativeCode create(TeleVM teleVM, Address codeStart, Size codeSize, String name) {
        TeleCompiledNativeCode teleCompiledNativeCode = null;
        try {
            // Fail if the region specified by 'address' and 'size' overlaps an existing native entry
            teleCompiledNativeCode = new TeleCompiledNativeCode(teleVM, codeStart, codeSize, name);
        } catch (IllegalArgumentException illegalArgumentException) {
            ProgramError.unexpected("Native code region is overlapping an existing code region");
        }
        return teleCompiledNativeCode;
    }

    private final CompiledNativeCodeMemoryRegion compiledNativeCodeMemoryRegion;

    private InstructionMap instructionMap = null;

    private IndexedSequence<TargetCodeInstruction> instructions;
    private IndexedSequence<MachineCodeLocation> instructionLocations;
    private CodeLocation codeStartLocation = null;

    private TeleCompiledNativeCode(TeleVM teleVM, Address start, Size size, String name) {
        super(teleVM);
        this.compiledNativeCodeMemoryRegion = new CompiledNativeCodeMemoryRegion(teleVM, this, name, start, size);
        this.instructionMap = new CompiledNativeCodeInstructionMap();
        // Register so that it can be located by address.
        vm().codeCache().register(this);
    }

    public String entityName() {
        return compiledNativeCodeMemoryRegion.regionName();
    }

    public String entityDescription() {
        return "A discovered block of native code not managed by the VM";
    }

    public MaxEntityMemoryRegion<MaxCompiledCode> memoryRegion() {
        return compiledNativeCodeMemoryRegion;
    }

    public boolean contains(Address address) {
        return compiledNativeCodeMemoryRegion.contains(address);
    }

    public InstructionMap instructionMap() {
        return instructionMap;
    }

    public Address getCodeStart() {
        return compiledNativeCodeMemoryRegion.start();
    }

    public CodeLocation getCodeStartLocation() {
        final Address codeStart = getCodeStart();
        if (codeStartLocation == null && codeStart != null) {
            codeStartLocation = codeManager().createMachineCodeLocation(codeStart, "code start location in native routine");
        }
        return codeStartLocation;
    }

    public Address getCallEntryPoint() {
        return getCodeStart();
    }

    public CodeLocation getEntryLocation() {
        return getCodeStartLocation();
    }

    public CodeLocation getCallEntryLocation() {
        return null;
    }

    public int compilationIndex() {
        return -1;
    }

    public TeleClassMethodActor getTeleClassMethodActor() {
        return null;
    }

    public String targetLocationToString(TargetLocation targetLocation) {
        return null;
    }

    public StopPositions getStopPositions() {
        return null;
    }

    private IndexedSequence<TargetCodeInstruction> getInstructions() {
        if (instructions == null && vm().tryLock()) {
            byte[] code = null;
            try {
                code = vm().dataAccess().readFully(getCodeStart(), codeSize().toInt());
            } finally {
                vm().unlock();
            }
            if (code != null) {
                instructions = TeleDisassembler.decode(vm().vmConfiguration().platform().processorKind, getCodeStart(), code, null);
            }
        }
        return instructions;
    }

    public TargetABI getAbi() {
        return null;
    }

    public ClassMethodActor classMethodActor() {
        return null;
    }

    public Size codeSize() {
        return compiledNativeCodeMemoryRegion.size();
    }

    @Deprecated
    public MethodProvider getMethodProvider() {
        return this.getTeleClassMethodActor();
    }

    public ClassActor classActorForObjectType() {
        return null;
    }

    public byte[] getCode() {
        return null;
    }

    public int[] bytecodeToTargetCodePositionMap() {
        return null;
    }

    public void writeSummary(PrintStream printStream) {
        printStream.println("Native method: " + entityName());
        printStream.println(" ***UNIMPLEMENTED*** ");
    }
}
