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
package com.sun.max.vm.compiler.dir.eir.sparc;

import com.sun.max.vm.compiler.b.c.d.e.sparc.*;
import com.sun.max.vm.compiler.dir.*;
import com.sun.max.vm.compiler.dir.eir.*;
import com.sun.max.vm.compiler.eir.*;
import com.sun.max.vm.compiler.eir.sparc.*;
import com.sun.max.vm.compiler.eir.sparc.SPARCEirInstruction.*;
import com.sun.max.vm.stack.sparc.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * @author Bernd Mathiske
 * @author Laurent Daynes
 */
public final class DirToSPARCEirMethodTranslation extends DirToEirMethodTranslation {
    public static final SPARCEirRegister.GeneralPurpose SAVED_SAFEPOINT_LATCH_LOCAL = SPARCEirRegister.GeneralPurpose.L5;

    boolean saveSafetpoinLatchInLocal = false;

    public void needsSavingSafepointLatchInLocal() {
        saveSafetpoinLatchInLocal = true;
    }

    public boolean saveSafetpoinLatchInLocal() {
        return saveSafetpoinLatchInLocal;
    }

    public DirToSPARCEirMethodTranslation(EirGenerator eirGenerator, EirMethod eirMethod, DirMethod dirMethod) {
        super(eirGenerator, eirMethod, dirMethod);
    }

    /**
     * SPARC does not support direct floating point to integer register moves. These operations require a staging area in memory.
     * Methods that perform such operations allocate an extra-slot on top of their frame (below the mandatory minimal stack frame)
     * The EirGenerator can detect the use of this staging area by checking if the _offsetToConversionArea is non-null.
     */
    private EirConstant offsetToConversionArea;

    public EirConstant offsetToConversionArea() {
        if (offsetToConversionArea == null) {
            offsetToConversionArea = createEirConstant(IntValue.from(SPARCStackFrameLayout.offsetToFirstFreeSlotFromStackPointer()));
        }
        return offsetToConversionArea;
    }

    public boolean usesConversionArea() {
        return offsetToConversionArea != null;
    }

    @Override
    protected DirToEirInstructionTranslation createInstructionTranslation(EirBlock eirBlock) {
        return new DirToSPARCEirInstructionTranslation(this, eirBlock);
    }

    @Override
    protected EirPrologue createPrologue(EirBlock eirBlock) {
        return new SPARCEirPrologue(eirBlock, eirMethod(),
                                    calleeSavedEirVariables, calleeSavedEirRegisters,
                                    isCalleeSavedParameter,
                                    eirParameters, parameterEirLocations);
    }

    @Override
    protected EirEpilogue createEpilogue(EirBlock eirBlock) {
        return new SPARCEirEpilogue(eirBlock, eirMethod(),
                                    calleeSavedEirVariables, calleeSavedEirRegisters,
                                    resultEirLocation());
    }

    @Override
    protected EirInstruction createJump(EirBlock eirBlock, EirBlock toBlock) {
        return new BA(eirBlock, toBlock);
    }

    @Override
    protected EirInstruction createReturn(EirBlock eirBlock) {
        return new RET(eirBlock);
    }

    @Override
    protected EirInstruction createTrampolineExit(EirBlock eirBlock, boolean isStaticTrampoline) {
        return new RET(eirBlock, isStaticTrampoline ? RET.FROM.STATIC_TRAMPOLINE : RET.FROM.TRAMPOLINE);
    }

    @Override
    protected EirInstruction createTrapStubExit(EirBlock eirBlock) {
        return new RET(eirBlock, RET.FROM.TRAP_STUB);
    }

    @Override
    public EirCall createCall(EirBlock eirBlock, EirABI abi, EirValue result, EirLocation resultLocation,
                    EirValue function, EirValue[] arguments, EirLocation[] argumentLocations, boolean isNativeFunctionCall) {
        if (function instanceof EirConstant) {
            final EirValue functionPointer = createEirVariable(Kind.WORD);
            eirBlock.appendInstruction(createAssignment(eirBlock, Kind.WORD, functionPointer, function));
            return new CALL(eirBlock, abi, result, resultLocation, functionPointer, arguments, argumentLocations, isNativeFunctionCall, this);
        }
        return new CALL(eirBlock, abi, result, resultLocation, function, arguments, argumentLocations, isNativeFunctionCall, this);
    }

    @Override
    public EirCall createRuntimeCall(EirBlock eirBlock, EirABI abi, EirValue result, EirLocation resultLocation,
                                      EirValue function, EirValue[] arguments, EirLocation[] argumentLocations) {
        return new CALL(eirBlock, abi, result, resultLocation, function, arguments, argumentLocations, false, this);
    }

    @Override
    public EirInstruction createAssignment(EirBlock eirBlock, Kind kind, EirValue destination, EirValue source) {
        return new SPARCEirAssignment(eirBlock, kind, destination, source);
    }

    @Override
    public EirSafepoint createSafepoint(EirBlock eirBlock) {
        return new SPARCEirSafepoint(eirBlock);
    }

    /**
     * A boolean value indicating that at least one call use an ABI that requires the caller to save the latch register.
     */
    private boolean callerMustSaveLatchRegister;

    /**
     * Set a flag indicating that a call that uses an ABI that requires the caller to save the latch register is being made.
     *
     * @return a boolean indicating whether this is the first time for the method being compiled.
     */
    public boolean callerMustSaveLatchRegister() {
        final boolean firstTime = !callerMustSaveLatchRegister;
        callerMustSaveLatchRegister = true;
        return firstTime;
    }
}
