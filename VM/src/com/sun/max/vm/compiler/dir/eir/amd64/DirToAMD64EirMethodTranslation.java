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
package com.sun.max.vm.compiler.dir.eir.amd64;

import com.sun.max.vm.compiler.b.c.d.e.amd64.*;
import com.sun.max.vm.compiler.dir.*;
import com.sun.max.vm.compiler.dir.eir.*;
import com.sun.max.vm.compiler.eir.*;
import com.sun.max.vm.compiler.eir.amd64.*;
import com.sun.max.vm.compiler.eir.amd64.AMD64EirInstruction.*;
import com.sun.max.vm.type.*;

/**
 * @author Bernd Mathiske
 */
public final class DirToAMD64EirMethodTranslation extends DirToEirMethodTranslation {

    public DirToAMD64EirMethodTranslation(EirGenerator eirGenerator, EirMethod eirMethod, DirMethod dirMethod) {
        super(eirGenerator, eirMethod, dirMethod);
    }

    @Override
    protected DirToEirInstructionTranslation createInstructionTranslation(EirBlock eirBlock) {
        return new DirToAMD64EirInstructionTranslation(this, eirBlock);
    }

    @Override
    protected EirPrologue createPrologue(EirBlock eirBlock) {
        return new AMD64EirPrologue(eirBlock, eirMethod(),
                                    calleeSavedEirVariables(), calleeSavedEirRegisters(),
                                    isCalleeSavedParameter(),
                                    eirParameters(), parameterEirLocations());
    }

    @Override
    protected EirEpilogue createEpilogue(EirBlock eirBlock) {
        return new AMD64EirEpilogue(eirBlock, eirMethod(),
                                    calleeSavedEirVariables(), calleeSavedEirRegisters(),
                                    resultEirLocation());
    }

    @Override
    protected EirInstruction createJump(EirBlock eirBlock, EirBlock toBlock) {
        return new JMP(eirBlock, toBlock);
    }

    @Override
    protected EirInstruction createReturn(EirBlock eirBlock) {
        return new RET(eirBlock);
    }

    @Override
    public EirCall createCall(EirBlock eirBlock, EirABI abi, EirValue result, EirLocation resultLocation,
                              EirValue function, EirValue[] arguments, EirLocation[] argumentLocations) {
        return new CALL(eirBlock, abi, result, resultLocation, function, arguments, argumentLocations, this);
    }

    @Override
    public EirCall createRuntimeCall(EirBlock eirBlock, EirABI abi, EirValue result, EirLocation resultLocation,
                                      EirValue function, EirValue[] arguments, EirLocation[] argumentLocations) {
        return new RUNTIME_CALL(eirBlock, abi, result, resultLocation, function, arguments, argumentLocations, this);
    }

    @Override
    public EirInstruction createAssignment(EirBlock eirBlock, Kind kind, EirValue destination, EirValue source) {
        final EirInstruction<?, ?> assignment = new AMD64EirAssignment(eirBlock, kind, destination, source);
        return assignment;
    }

    @Override
    public EirSafepoint createSafepoint(EirBlock eirBlock) {
        return new AMD64EirSafepoint(eirBlock);
    }
}
