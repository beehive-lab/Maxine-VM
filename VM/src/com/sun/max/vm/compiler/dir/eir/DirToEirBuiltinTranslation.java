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
package com.sun.max.vm.compiler.dir.eir;

import com.sun.max.program.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.compiler.builtin.SpecialBuiltin.*;
import com.sun.max.vm.compiler.dir.*;
import com.sun.max.vm.compiler.eir.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * Platform-independent aspects of translations from DIR to EIR for builtins.
 *
 * @author Bernd Mathiske
 */
public abstract class DirToEirBuiltinTranslation extends BuiltinAdapter<DirValue> {

    private final DirToEirInstructionTranslation _instructionTranslation;

    protected DirToEirInstructionTranslation instructionTranslation() {
        return _instructionTranslation;
    }

    protected DirToEirMethodTranslation methodTranslation() {
        return _instructionTranslation.methodTranslation();
    }

    protected DirToEirBuiltinTranslation(DirToEirInstructionTranslation instructionTranslation) {
        _instructionTranslation = instructionTranslation;
    }

    public EirABI abi() {
        return _instructionTranslation.abi();
    }

    public void addInstruction(EirInstruction instruction) {
        _instructionTranslation.addInstruction(instruction);
    }

    public EirBlock eirBlock() {
        return _instructionTranslation.eirBlock();
    }

    public void setEirBlock(EirBlock eirBlock) {
        _instructionTranslation.setBlock(eirBlock);
    }

    public EirValue dirToEirValue(DirValue dirValue) {
        return _instructionTranslation.dirToEirValue(dirValue);
    }

    public EirConstant dirToEirConstant(DirConstant dirConstant) {
        return _instructionTranslation.dirToEirConstant(dirConstant);
    }

    public EirVariable createEirVariable(Kind kind) {
        return _instructionTranslation.createEirVariable(kind);
    }

    public EirConstant createEirConstant(Value value) {
        return _instructionTranslation.createEirConstant(value);
    }

    public EirInstruction assign(Kind kind, EirValue destination, EirValue source) {
        return _instructionTranslation.assign(kind, destination, source);
    }

    @Override
    public final void visitCall(Call builtin, DirValue dirResult, DirValue[] dirArguments) {
        EirValue address;
        EirValue[] arguments = null;
        EirLocation[] argumentLocations = null;
        switch (dirArguments.length) {
            case 0: {
                // Special direct call that will be linked later (e.g. by the JIT) - provide an arbitrary placeholder callee:
                address = new EirMethodValue(methodTranslation().classMethodActor());
                break;
            }
            case 1: {
                // Regular indirect call.
                address = dirToEirValue(dirArguments[0]);
                break;
            }
            case 2: {
                // Indirect call with receiver.
                address = dirToEirValue(dirArguments[0]);
                arguments = new EirValue[]{dirToEirValue(dirArguments[1])};
                argumentLocations = methodTranslation().abi().getParameterLocations(EirStackSlot.Purpose.LOCAL, Kind.REFERENCE);
                break;
            }
            default: {
                throw ProgramError.unknownCase();
            }
        }
        final EirValue result = dirToEirValue(dirResult);
        final EirLocation resultLocation = result == null ? null : methodTranslation().abi().getResultLocation(result.kind());
        addInstruction(methodTranslation().createCall(eirBlock(), methodTranslation().abi(), result, resultLocation, address, arguments, argumentLocations));
    }

    @Override
    public final void visitBreakpoint(Breakpoint builtin, DirValue dirResult, DirValue[] dirArguments) {
        assert dirArguments.length == 0;
        addInstruction(new EirBreakpoint(eirBlock()));
    }

    @Override
    public final void visitMarker(Marker builtin, DirValue dirResult, DirValue[] dirArguments) {
        assert dirArguments.length == 0;
        addInstruction(new EirMarker(eirBlock()));
    }

}
