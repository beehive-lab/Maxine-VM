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
package com.sun.max.tele.method;

import static com.sun.max.platform.Platform.*;

import java.io.*;
import java.util.*;

import com.sun.cri.ci.*;
import com.sun.cri.ri.*;
import com.sun.max.tele.*;
import com.sun.max.tele.data.*;
import com.sun.max.tele.memory.*;
import com.sun.max.tele.method.CodeLocation.MachineCodeLocation;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;

/**
 * Holds information about a block of code in the process of the VM, about which little is known
 * other than the memory location that is not in any VM-allocated region, and possibly a name
 * assigned during a session.
 * <p>
 * No attempt is made to check for changes to the code during
 * a session, unlike VM target methods.
 */
public final class TeleExternalCodeRoutine extends AbstractVmHolder implements MaxExternalCodeRoutine {

    /**
     * Description of a region of external native code discovered in the VM's process.
     * <p>
     * This region has no parent, as little is known about it.
     * <p>
     * This region has no children.
     */
    private static final class ExternalCodeMemoryRegion extends TeleFixedMemoryRegion implements MaxEntityMemoryRegion<MaxExternalCodeRoutine> {

        private static final List<MaxEntityMemoryRegion<? extends MaxEntity>> EMPTY = Collections.emptyList();

        private MaxExternalCodeRoutine owner;

        private ExternalCodeMemoryRegion(MaxVM vm, MaxExternalCodeRoutine owner, String name, Address start, long nBytes) {
            super(vm, name, start, nBytes);
            this.owner = owner;
        }

        public MaxEntityMemoryRegion< ? extends MaxEntity> parent() {
            return null;
        }

        public List<MaxEntityMemoryRegion< ? extends MaxEntity>> children() {
            return EMPTY;
        }

        public MaxExternalCodeRoutine owner() {
            return owner;
        }

        public boolean isBootRegion() {
            return false;
        }
    }

    /**
     * Summary information about a sequence of external disassembled machine code instructions about
     * which little is known.
     *
     */
    private final class ExternalCodeInstructionMap implements InstructionMap {

        private final List<MachineCodeLocation> machineCodeLocations;

        /**
         * Unmodifiable list of all instruction indexes where a label is present.
         */
        private final List<Integer> labelIndexes;

        ExternalCodeInstructionMap() throws MaxInvalidAddressException {
            instructions = getInstructions();
            final int length = instructions.size();
            final List<MachineCodeLocation> locations = new ArrayList<MachineCodeLocation>(length);
            final List<Integer> labels = new ArrayList<Integer>();
            for (int index = 0; index < length; index++) {
                final TargetCodeInstruction targetCodeInstruction = instructions.get(index);
                locations.add(codeLocationFactory().createMachineCodeLocation(targetCodeInstruction.address, "external machine code instruction"));
                if (targetCodeInstruction.label != null) {
                    labels.add(index);
                }
            }
            machineCodeLocations = locations;
            labelIndexes = Collections.unmodifiableList(labels);
        }

        public int length() {
            return instructions.size();
        }

        public TargetCodeInstruction instruction(int index) throws IllegalArgumentException {
            if (index < 0 || index >= instructions.size()) {
                throw new IllegalArgumentException();
            }
            return instructions.get(index);
        }

        public int findInstructionIndex(Address address) {
            if (address != null) {
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
            }
            return -1;
        }

        public MachineCodeLocation instructionLocation(int index) {
            if (index < 0 || index >= instructions.size()) {
                throw new IllegalArgumentException();
            }
            return machineCodeLocations.get(index);
        }

        public boolean isSafepoint(int index) throws IllegalArgumentException {
            if (index < 0 || index >= instructions.size()) {
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
            if (index < 0 || index >= instructions.size()) {
                throw new IllegalArgumentException();
            }
            return false;
        }

        public CiDebugInfo debugInfoAt(int index) throws IllegalArgumentException {
            if (index < 0 || index >= instructions.size()) {
                throw new IllegalArgumentException();
            }
            return null;
        }

        public int opcode(int index) throws IllegalArgumentException {
            if (index < 0 || index >= instructions.size()) {
                throw new IllegalArgumentException();
            }
            return -1;
        }

        public RiMethod calleeAt(int index) throws IllegalArgumentException {
            if (index < 0 || index >= instructions.size()) {
                throw new IllegalArgumentException();
            }
            return null;
        }

        public List<Integer> labelIndexes() {
            return labelIndexes;
        }

        public int[] bciToMachineCodePositionMap() {
            return null;
        }
    }

    private final ExternalCodeMemoryRegion externalCodeMemoryRegion;

    private InstructionMap instructionMap = null;

    private List<TargetCodeInstruction> instructions;
    private List<MachineCodeLocation> instructionLocations;
    private CodeLocation codeStartLocation = null;

    /**
     * Creates a representation of a block of native code about which little is known.
     * <p>
     * No subsequent checks are made to determine whether the code gets modified.
     *
     * @param vm the VM
     * @param start starting location of code in memory
     * @param nBytes length in bytes of code in memory
     * @param name the name to assign to the block of code in the registry
     * @throws IllegalArgumentException if the range overlaps one already in the registry
     * @throws MaxInvalidAddressException if unable to read memory.
     */
    public TeleExternalCodeRoutine(TeleVM vm, Address start, long nBytes, String name) throws MaxInvalidAddressException {
        super(vm);
        this.externalCodeMemoryRegion = new ExternalCodeMemoryRegion(vm, this, name, start, nBytes);
        this.instructionMap = new ExternalCodeInstructionMap();
    }

    private List<TargetCodeInstruction> getInstructions() throws MaxInvalidAddressException {
        if (instructions == null && vm().tryLock()) {
            byte[] code = null;
            final Address codeStart = getCodeStart();
            try {
                final long nBytes = externalCodeMemoryRegion.nBytes();
                assert nBytes < Integer.MAX_VALUE;
                code = memory().readBytes(codeStart, (int) nBytes);
            } catch (DataIOError dataIOError) {
                throw new MaxInvalidAddressException(codeStart, "Can't read data at " + codeStart.to0xHexString());
            } finally {
                vm().unlock();
            }
            if (code != null) {
                instructions = TeleDisassembler.decode(platform(), codeStart, code, null);
            }
        }
        return instructions;
    }

    public String entityName() {
        return externalCodeMemoryRegion.regionName();
    }

    public String entityDescription() {
        return "A discovered block of native code not managed by the VM";
    }

    public MaxEntityMemoryRegion<MaxExternalCodeRoutine> memoryRegion() {
        return externalCodeMemoryRegion;
    }

    public boolean contains(Address address) {
        return externalCodeMemoryRegion.contains(address);
    }

    public TeleObject representation() {
        // No distinguished object in VM runtime represents unknown native code.
        return null;
    }

    public InstructionMap getInstructionMap() {
        return instructionMap;
    }

    /** {@inheritDoc}
     * <p>
     * We don't bother to check if native code has changed once we have read and disassembled it.
     */
    @Override
    public int vmCodeGeneration() {
        return 0;
    }

    public Address getCodeStart() {
        return externalCodeMemoryRegion.start();
    }

    public CodeLocation getCodeStartLocation() {
        final Address codeStart = getCodeStart();
        if (codeStartLocation == null && codeStart != null) {
            codeStartLocation = codeLocationFactory().createMachineCodeLocation(codeStart, "code start location in external native code");
        }
        return codeStartLocation;
    }

    public CodeLocation getCallEntryLocation() {
        return null;
    }

    public void writeSummary(PrintStream printStream) {
        printStream.println("External native code: " + entityName());
        printStream.println(" ***UNIMPLEMENTED*** for external native methods");
    }
}
