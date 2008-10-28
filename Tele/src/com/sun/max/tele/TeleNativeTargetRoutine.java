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
import com.sun.max.tele.debug.*;
import com.sun.max.tele.method.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.bytecode.*;

/**
 * Holds information about a block of code in
 * the process of the target VM, about which little is known
 * other than the memory location.
 *
 * @author Michael Van De Vanter
 *
 */
public final class TeleNativeTargetRoutine extends TeleVMHolder implements TeleTargetRoutine {

    public static final Size DEFAULT_NATIVE_CODE_LENGTH = Size.fromInt(200);

    /**
     * @return a newly created surrogate for a block of native code discovered in the tele VM
     * about which little more is known than its location.
     * @throws IllegalArgumentException if any code already known occupies the specified code location.
     */
    public static TeleNativeTargetRoutine create(TeleVM teleVM, Address codeStart, Size codeSize) {
        TeleNativeTargetRoutine teleNativeTargetRoutine = null;
        try {
            // It's possible that the region specified by 'address' and 'size' overlaps an existing native method since
            // the sizes are estimates by the user.
            teleNativeTargetRoutine = new TeleNativeTargetRoutine(teleVM, codeStart, codeSize);
        } catch (IllegalArgumentException illegalArgumentException) {
            throw new TeleError("Native code region is overlapping an existing code region");
        }
        return teleNativeTargetRoutine;
    }

    /**
     * @return an already existing surrogate for a block of native code discovered in the teleVM
     * whose location includes the specified address, null if not known to be a native routine
     * or if known to be a Java method.
     */
    public static TeleNativeTargetRoutine get(TeleVM teleVM, Address address) {
        return teleVM.teleCodeRegistry().get(TeleNativeTargetRoutine.class, address);
    }

    private final TeleRoutine _teleRoutine;

    /**
     * @return surrogate that represents the (implied) code body from which the code was compiled.
     */
    public TeleRoutine teleRoutine() {
        return _teleRoutine;
    }

    private final TargetCodeRegion _targetCodeRegion;

    public TargetCodeRegion targetCodeRegion() {
        return _targetCodeRegion;
    }

    public TeleNativeTargetRoutine(TeleVM teleVM, Address start, Size size) {
        super(teleVM);
        _teleRoutine = new TeleRoutine() {
            public String getUniqueName() {
                return "native method @ " + targetCodeRegion().start().toHexString();
            }
        };
        _targetCodeRegion = new TargetCodeRegion(this, start, size);
        teleVM().teleCodeRegistry().add(this);
    }

    public Address codeStart() {
        return _targetCodeRegion.start();
    }

    public Size codeSize() {
        return _targetCodeRegion.size();
    }

    public Address callEntryPoint() {
        return codeStart();
    }

    private IndexedSequence<TargetCodeInstruction> _instructions;

    public IndexedSequence<TargetCodeInstruction> getInstructions() {
        if (_instructions == null) {
            final byte[] code = teleVM().teleProcess().dataAccess().readFully(codeStart(), codeSize().toInt());
            _instructions = TeleDisassembler.decode(teleVM(), codeStart(), code, null);
        }
        return _instructions;
    }

    public TeleTargetBreakpoint setTargetBreakpointAtEntry() {
        return teleVM().teleProcess().targetBreakpointFactory().makeBreakpoint(callEntryPoint(), false);
    }

    public void setTargetCodeLabelBreakpoints() {
        for (TargetCodeInstruction targetCodeInstruction : getInstructions()) {
            if (targetCodeInstruction.label() != null) {
                teleVM().teleProcess().targetBreakpointFactory().makeBreakpoint(targetCodeInstruction.address(), false);
            }
        }
    }

    public void clearTargetCodeLabelBreakpoints() {
        for (TargetCodeInstruction targetCodeInstruction : getInstructions()) {
            if (targetCodeInstruction.label() != null) {
                teleVM().teleProcess().targetBreakpointFactory().removeBreakpointAt(targetCodeInstruction.address());
            }
        }
    }

    public TeleClassMethodActor getTeleClassMethodActor() {
        return null;
    }

    public int[] getStopPositions() {
        return null;
    }

    public int[] bytecodeToTargetCodePositionMap() {
        return null;
    }

    public BytecodeInfo[] bytecodeInfos() {
        return null;
    }

    public int getJavaStopIndex(Address address) {
        return -1;
    }


    @Override
    public MachineCodeInstructionArray getTargetCodeInstructions() {
        final IndexedSequence<TargetCodeInstruction> instructions = getInstructions();
        final MachineCodeInstruction[] result = new MachineCodeInstruction[instructions.length()];
        for (int i = 0; i < result.length; i++) {
            final TargetCodeInstruction ins = instructions.get(i);
            result[i] = new MachineCodeInstruction(ins.getMnemonic(), ins.getPosition(), ins.getAddress(), ins.getLabel(), ins.getBytes(), ins.getOperands(), ins.getTargetAddress());
        }
        return new MachineCodeInstructionArray(result);
    }

    @Override
    public MethodProvider getMethodProvider() {
        return this.getTeleClassMethodActor();
    }
}
