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

import static com.sun.max.platform.Platform.*;

import java.io.*;
import java.util.*;

import com.sun.cri.ci.*;
import com.sun.max.tele.data.*;
import com.sun.max.tele.memory.*;
import com.sun.max.tele.method.*;
import com.sun.max.tele.method.CodeLocation.MachineCodeLocation;
import com.sun.max.tele.object.*;
import com.sun.max.tele.util.*;
import com.sun.max.unsafe.*;

/**
 * Holds information about a block of code in
 * the process of the VM, about which little is known
 * other than the memory location and possibly a name
 * assigned during a session.
 * <br>
 * No attempt is made to check for changes to the code during
 * a session, unlike VM target methods.
 */
public final class TeleExternalCode extends AbstractTeleVMHolder implements MaxExternalCode {

    /**
     * Description of a region of external native code discovered with the VM.
     * <br>
     * This region has no parent, as little is known about it.
     * <br>
     * This region has no children.
     *
     */
    private static final class ExternalCodeMemoryRegion extends TeleFixedMemoryRegion implements MaxEntityMemoryRegion<MaxExternalCode> {

        private static final List<MaxEntityMemoryRegion<? extends MaxEntity>> EMPTY = Collections.emptyList();

        private MaxExternalCode owner;

        private ExternalCodeMemoryRegion(TeleVM vm, MaxExternalCode owner, String name, Address start, long nBytes) {
            super(vm, name, start, nBytes);
            this.owner = owner;
        }

        public MaxEntityMemoryRegion< ? extends MaxEntity> parent() {
            return null;
        }

        public List<MaxEntityMemoryRegion< ? extends MaxEntity>> children() {
            return EMPTY;
        }

        public MaxExternalCode owner() {
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

        ExternalCodeInstructionMap() {
            instructions = getInstructions();
            final int length = instructions.size();
            final List<MachineCodeLocation> locations = new ArrayList<MachineCodeLocation>(length);
            final List<Integer> labels = new ArrayList<Integer>();
            for (int index = 0; index < length; index++) {
                final TargetCodeInstruction targetCodeInstruction = instructions.get(index);
                locations.add(codeManager().createMachineCodeLocation(targetCodeInstruction.address, "external machine code instruction"));
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

        public boolean isStop(int index) throws IllegalArgumentException {
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

        public CiFrame bytecodeFrames(int index) throws IllegalArgumentException {
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

        public int calleeConstantPoolIndex(int index) throws IllegalArgumentException {
            if (index < 0 || index >= instructions.size()) {
                throw new IllegalArgumentException();
            }
            return -1;
        }

        public List<Integer> labelIndexes() {
            return labelIndexes;
        }

        public int[] bytecodeToMachineCodePositionMap() {
            return null;
        }
    }

    /**
     * Creates representation for a block of native code not previously known.
     * <br>
     * Must be called in thread with the VM lock held.
     *
     * @param codeStart starting memory location of the code in VM memory
     * @param nBytes size of the region in bytes
     * @param name a name for the region
     *  @return a newly created surrogate for a block of native code discovered in the VM
     * about which little more is known than its location.  The location must not overlap any code
     * region already known.
     * @throws MaxInvalidAddressException if memory cannot be read
     */
    public static TeleExternalCode create(TeleVM teleVM, Address codeStart, long nBytes, String name) throws MaxInvalidAddressException {
        assert teleVM.lockHeldByCurrentThread();
        TeleExternalCode teleExternalCode = null;
        try {
            // Fail if the region specified by 'address' and 'nBytes' overlaps an existing native entry
            teleExternalCode = new TeleExternalCode(teleVM, codeStart, nBytes, name);
        } catch (IllegalArgumentException illegalArgumentException) {
            TeleError.unexpected("External native code region is overlapping an existing code region");
        }
        return teleExternalCode;
    }

    private final ExternalCodeMemoryRegion externalCodeMemoryRegion;

    private InstructionMap instructionMap = null;

    private List<TargetCodeInstruction> instructions;
    private List<MachineCodeLocation> instructionLocations;
    private CodeLocation codeStartLocation = null;

    /**
     * Creates a representation of a block of native code about which little is known.
     * <br>
     * No subsequent checks are made to determine whether the code gets modified.
     *
     * @param teleVM
     * @param start starting location of code in memory
     * @param nBytes length in bytes of code in memory
     * @param name the name to assign to the block of code in the registry
     * @throws IllegalArgumentException if the range overlaps one already in the registry
     * @throws MaxInvalidAddressException if unable to read memory.
     */
    private TeleExternalCode(TeleVM teleVM, Address start, long nBytes, String name) throws MaxInvalidAddressException {
        super(teleVM);
        this.externalCodeMemoryRegion = new ExternalCodeMemoryRegion(teleVM, this, name, start, nBytes);

        try {
            this.instructionMap = new ExternalCodeInstructionMap();
        } catch (DataIOError dataIOError) {
            throw new MaxInvalidAddressException(start);
        }
        // Register so that it can be located by address.
        vm().codeCache().register(this);
    }

    private List<TargetCodeInstruction> getInstructions() {
        if (instructions == null && vm().tryLock()) {
            byte[] code = null;
            try {
                final long nBytes = externalCodeMemoryRegion.nBytes();
                assert nBytes < Integer.MAX_VALUE;
                code = vm().dataAccess().readFully(getCodeStart(), (int) nBytes);
            } finally {
                vm().unlock();
            }
            if (code != null) {
                instructions = TeleDisassembler.decode(platform(), getCodeStart(), code, null);
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

    public MaxEntityMemoryRegion<MaxExternalCode> memoryRegion() {
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
            codeStartLocation = codeManager().createMachineCodeLocation(codeStart, "code start location in external native code");
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
