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
/*VCSID=d117957c-c0b0-476e-9275-8eeab4d6e9a8*/
package com.sun.max.tele;

import com.sun.max.collect.*;
import com.sun.max.jdwp.vm.data.*;
import com.sun.max.jdwp.vm.proxy.*;
import com.sun.max.tele.debug.*;
import com.sun.max.tele.method.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.value.*;

/**
 * Surrogate for a {@linkplain RuntimeStub runtime stub} in the tele VM.
 *
 * @author Doug Simon
 */
public class TeleRuntimeStub  extends TeleTupleObject implements TeleTargetRoutine  {

    /**
     * Gets a {@code TeleTargetMethod} instance representing the {@link RuntimeStub} in the tele VM that contains a
     * given instruction pointer. If the instruction pointer does not lie within a runtime stub, then null is returned.
     * If the instruction pointer is within a runtime stub but there is no {@code TeleRuntimeStub} instance existing
     * for it in the {@linkplain TeleCodeRegistry tele code registry}, then a new instance is created and returned.
     *
     * @param instructionPointer an instruction pointer in the tele VM's address space
     * @return {@code TeleRuntimeStub} instance representing the {@code RuntimeStub} containing {@code
     *         instructionPointer} or null if there is no {@code RuntimeStub} containing {@code instructionPointer}
     */
    public static TeleRuntimeStub make(TeleVM teleVM, Address instructionPointer) {
        assert instructionPointer != Address.zero();
        if (!teleVM.isBootImageRelocated()) {
            return null;
        }
        TeleRuntimeStub teleRuntimeStub = null;
        final TeleTargetRoutine teleTargetRoutine = teleVM.teleCodeRegistry().get(TeleTargetRoutine.class, instructionPointer);
        if (teleTargetRoutine != null) {
            if (teleTargetRoutine instanceof TeleRuntimeStub) {
                // known stub
                teleRuntimeStub =(TeleRuntimeStub) teleTargetRoutine;
            } else {
                // known native or Java method
                teleRuntimeStub = null;
            }
        } else if (teleVM.codeContains(instructionPointer)) {
            // An address, previously unknown in the registry, in the target VM code regions.
            final Reference runtimeStubReference =  teleVM.methods().Code_codePointerToRuntimeStub.interpret(new WordValue(instructionPointer)).asReference();
            if (runtimeStubReference != null && !runtimeStubReference.isZero()) {
                teleRuntimeStub = (TeleRuntimeStub) TeleObject.make(teleVM, runtimeStubReference);
            }
        }
        return teleRuntimeStub;
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

    private final RuntimeStub _runtimeStub;

    public RuntimeStub runtimeStub() {
        return _runtimeStub;
    }

    public TeleRuntimeStub(TeleVM teleVM, Reference runtimeStubReference) {
        super(teleVM, runtimeStubReference);
        _runtimeStub = (RuntimeStub) deepCopy();
        _targetCodeRegion = new TargetCodeRegion(this, _runtimeStub.start(), _runtimeStub.size());
       _teleRoutine = new TeleRoutine() {
            public String getUniqueName() {
                return  _runtimeStub.getClass().getSimpleName() + _runtimeStub;
            }
        };
        teleVM().teleCodeRegistry().add(this);
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

    public Address codeStart() {
        return _runtimeStub.start();
    }

    public Size codeSize() {
        return _runtimeStub.size();
    }

    public Address callEntryPoint() {
        return codeStart();
    }

    private IndexedSequence<TargetCodeInstruction> _instructions;

    @Override
    public IndexedSequence<TargetCodeInstruction> getInstructions() {
        if (_instructions == null) {
            final byte[] code = teleVM().teleProcess().dataAccess().readFully(codeStart(), codeSize().toInt());
            _instructions = TeleDisassembler.create(teleVM(), codeStart(), code, null);
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
