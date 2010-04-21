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

import com.sun.max.collect.*;
import com.sun.max.jdwp.vm.data.*;
import com.sun.max.jdwp.vm.proxy.*;
import com.sun.max.program.*;
import com.sun.max.tele.method.*;
import com.sun.max.tele.method.CodeLocation.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.compiler.target.*;

/**
 * Holds information about a block of code in
 * the process of the VM, about which little is known
 * other than the memory location and possibly an assigned name.
 *
 * @author Michael Van De Vanter
 */
public final class TeleNativeTargetRoutine extends AbstractTeleVMHolder implements TeleTargetRoutine {

    public static final Size DEFAULT_NATIVE_CODE_LENGTH = Size.fromInt(200);

    /**
     * @param name a name for the region
     * @return a newly created surrogate for a block of native code discovered in the VM
     * about which little more is known than its location.  The location must not overlap any code
     * region already known.
     */
    public static TeleNativeTargetRoutine create(TeleVM teleVM, Address codeStart, Size codeSize, String name) {
        TeleNativeTargetRoutine teleNativeTargetRoutine = null;
        try {
            // Fail if the region specified by 'address' and 'size' overlaps an existing native  entry
            teleNativeTargetRoutine = new TeleNativeTargetRoutine(teleVM, codeStart, codeSize, name);
        } catch (IllegalArgumentException illegalArgumentException) {
            ProgramError.unexpected("Native code region is overlapping an existing code region");
        }
        return teleNativeTargetRoutine;
    }

    /**
     * @return an already existing surrogate for a block of native code discovered in the VM
     * whose location includes the specified address, null if not known to be a native routine
     * or if known to be a Java method.
     */
    public static TeleNativeTargetRoutine make(TeleVM teleVM, Address address) {
        return teleVM.findTeleTargetRoutine(TeleNativeTargetRoutine.class, address);
    }

    private final TeleRoutine teleRoutine;

    /**
     * @return surrogate that represents the (implied) code body from which the code was compiled.
     */
    public TeleRoutine teleRoutine() {
        return teleRoutine;
    }

    private final TargetCodeRegion targetCodeRegion;

    public TargetCodeRegion targetCodeRegion() {
        return targetCodeRegion;
    }

    private final String name;

    public String getName() {
        return name;
    }

    private TeleNativeTargetRoutine(TeleVM teleVM, Address start, Size size, String name) {
        super(teleVM);
        this.teleRoutine = new TeleRoutine() {
            public String getUniqueName() {
                return "native method @ " + targetCodeRegion().start().toHexString();
            }
        };
        this.targetCodeRegion = new NativeTargetCodeRegion(teleVM, this, start, size);
        this.name = name;
        // Register so that it can be located by address.
        vm().registerTeleTargetRoutine(this);
    }

    public Address getCodeStart() {
        return targetCodeRegion.start();
    }

    public Size codeSize() {
        return targetCodeRegion.size();
    }

    public Address callEntryPoint() {
        return getCodeStart();
    }

    private IndexedSequence<TargetCodeInstruction> instructions;
    private IndexedSequence<MachineCodeLocation> instructionLocations;

    public IndexedSequence<TargetCodeInstruction> getInstructions() {
        if (instructions == null) {
            final byte[] code = vm().dataAccess().readFully(getCodeStart(), codeSize().toInt());
            instructions = TeleDisassembler.decode(vm().vmConfiguration().platform().processorKind, getCodeStart(), code, null);
        }
        return instructions;
    }

    public IndexedSequence<MachineCodeLocation> getInstructionLocations() {
        if (instructionLocations == null) {
            getInstructions();
            final int length = instructions.length();
            final MutableSequence<MachineCodeLocation> locations = new VectorSequence<MachineCodeLocation>(length);
            for (int i = 0; i < length; i++) {
                locations.set(i, codeManager().createMachineCodeLocation(instructions.get(i).address, "native target code instruction"));
            }
            instructionLocations = locations;
        }
        return instructionLocations;
    }

    public CodeLocation entryLocation() {
        return codeManager().createMachineCodeLocation(callEntryPoint(), "entry for native routine " + getName());
    }

    public Sequence<MaxCodeLocation> labelLocations() {
        final AppendableSequence<MaxCodeLocation> locations = new ArrayListSequence<MaxCodeLocation>();
        for (TargetCodeInstruction targetCodeInstruction : getInstructions()) {
            if (targetCodeInstruction.label != null) {
                final String description = "Label " + targetCodeInstruction.label.toString() + " in " + getName();
                locations.append(codeManager().createMachineCodeLocation(targetCodeInstruction.address, description));
            }
        }
        return locations;
    }

    public TeleClassMethodActor getTeleClassMethodActor() {
        return null;
    }

    public StopPositions getStopPositions() {
        return null;
    }

    public int getJavaStopIndex(Address address) {
        return -1;
    }

    public MachineCodeInstructionArray getTargetCodeInstructions() {
        final IndexedSequence<TargetCodeInstruction> instructions = getInstructions();
        final MachineCodeInstruction[] result = new MachineCodeInstruction[instructions.length()];
        for (int i = 0; i < result.length; i++) {
            final TargetCodeInstruction ins = instructions.get(i);
            result[i] = new MachineCodeInstruction(ins.mnemonic, ins.position, ins.address.toLong(), ins.label, ins.bytes, ins.operands, ins.getTargetAddressAsLong());
        }
        return new MachineCodeInstructionArray(result);
    }

    public MethodProvider getMethodProvider() {
        return this.getTeleClassMethodActor();
    }
}
