/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.max.tele.method.CodeLocation.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;


public class TeleNativeFunction extends AbstractVmHolder implements MaxNativeFunction, Comparable<TeleNativeFunction> {

    /**
     * Description of a region of external native code discovered in the VM's process.
     * <p>
     * This region has no children.
     */
    static final class NativeFunctionMemoryRegion extends TeleFixedMemoryRegion implements MaxEntityMemoryRegion<MaxNativeFunction> {

        private static final List<MaxEntityMemoryRegion<? extends MaxEntity>> EMPTY = Collections.emptyList();

        private MaxNativeFunction owner;

        private NativeFunctionMemoryRegion(MaxVM vm, MaxNativeFunction owner) {
            super(vm, owner.name(), owner.getCodeStart(), owner.length());
            this.owner = owner;
        }

        @SuppressWarnings("unchecked")
        public MaxEntityMemoryRegion< ? extends MaxEntity> parent() {
            if (owner.library() == null) {
                return null;
            } else {
                return owner.library().memoryRegion();
            }
        }

        public List<MaxEntityMemoryRegion< ? extends MaxEntity>> children() {
            return EMPTY;
        }

        public MaxNativeFunction owner() {
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
    private final class NativeFunctionMachineCodeInfo implements MaxMachineCodeInfo {

        private final List<MachineCodeLocation> machineCodeLocations;

        /**
         * Unmodifiable list of all instruction indexes where a label is present.
         */
        private final List<Integer> labelIndexes;

        NativeFunctionMachineCodeInfo() throws MaxInvalidAddressException {
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

    private final String name;
    Address base;
    int length;
    private TeleNativeLibrary lib;  // null for disconnected function (rare)
    private NativeFunctionMemoryRegion nativeFunctionMemoryRegion;
    private MaxMachineCodeInfo machineCodeInfo;
    private List<TargetCodeInstruction> instructions;
    private List<MachineCodeLocation> instructionLocations;
    private CodeLocation codeStartLocation = null;

    private TeleNativeFunction(TeleVM vm, String name, Address base) {
        super(vm);
        this.name = name;
        this.base = base;
    }
    /**
     * Create a {@link TeleNativeFunction}.
     * @param vm
     * @param name function name
     * @param offset initially the offset from the base of the library.
     * @param lib associated native library.
     * @throws MaxInvalidAddressException
     */
    public TeleNativeFunction(TeleVM vm, String name, Address offset, TeleNativeLibrary lib) {
        this(vm, name, offset);
        this.lib = lib;
    }

    /**
     * Create a disconnected native function.
     * @param vm
     * @param name
     * @param base
     * @param length
     */
    public TeleNativeFunction(TeleVM vm, String name, Address base, long length) throws MaxInvalidAddressException {
        this(vm, name, base);
        this.length = (int) length;
        this.nativeFunctionMemoryRegion = new NativeFunctionMemoryRegion(vm(), this);
        this.machineCodeInfo = new NativeFunctionMachineCodeInfo();
    }

    public void updateAddress() {
        assert lib != null && lib.base().isNotZero();
        base = base.plus(lib.base());
    }

    public void updateLength(int length) throws MaxInvalidAddressException {
        this.length = length;
        this.nativeFunctionMemoryRegion = new NativeFunctionMemoryRegion(vm(), this);
        this.machineCodeInfo = new NativeFunctionMachineCodeInfo();
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public String entityName() {
        return qualName();
    }

    @Override
    public String entityDescription() {
        return "Native function " + qualName();
    }

    @Override
    public MaxEntityMemoryRegion<MaxNativeFunction> memoryRegion() {
        return nativeFunctionMemoryRegion;
    }

    @Override
    public boolean contains(Address address) {
        return nativeFunctionMemoryRegion.contains(address);
    }

    @Override
    public TeleObject representation() {
        // No distinguished object in VM runtime represents a native function.
        return null;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public int length() {
        return length;
    }

    @Override
    public String qualName() {
        return lib == null ? name : lib.entityName() + ":" + name;
    }

    @Override
    public MaxNativeLibrary library() {
        return lib;
    }

    public int compareTo(TeleNativeFunction other) {
        if (lib.sortByName()) {
            return name.compareToIgnoreCase(other.name);
        } else {
            if (base.lessThan(other.base)) {
                return -1;
            } else if (base.greaterThan(other.base)) {
                return 1;
            } else {
                return 0;
            }
        }
    }
    public MaxMachineCodeInfo getMachineCodeInfo() {
        return machineCodeInfo;
    }

    /** {@inheritDoc}
     * <p>
     * We don't bother to check if native code has changed once we have read and disassembled it.
     */
    @Override
    public int codeVersion() {
        return 0;
    }

    public Address getCodeStart() {
        return base;
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

    private List<TargetCodeInstruction> getInstructions() throws MaxInvalidAddressException {
        if (instructions == null && vm().tryLock()) {
            byte[] code = null;
            final Address codeStart = getCodeStart();
            try {
                final long nBytes = nativeFunctionMemoryRegion.nBytes();
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


}
