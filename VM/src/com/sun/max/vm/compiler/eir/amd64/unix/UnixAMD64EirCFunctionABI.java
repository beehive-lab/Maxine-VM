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
package com.sun.max.vm.compiler.eir.amd64.unix;

import static com.sun.max.vm.compiler.eir.amd64.AMD64EirRegister.General.*;

import com.sun.max.annotate.*;
import com.sun.max.asm.amd64.*;
import com.sun.max.collect.*;
import com.sun.max.vm.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.eir.amd64.*;
import com.sun.max.vm.compiler.target.*;

/**
 * C functions that we implement behave almost like native functions.
 * They have the same parameters and callee save registers.
 * For simplicity, we do use the stack pointer as frame pointer, though,
 * just as we are used to in Java methods.
 *
 * @author Bernd Mathiske
 */
public class UnixAMD64EirCFunctionABI extends UnixAMD64EirJavaABI {

    private final PoolSet<AMD64EirRegister> callerSavedRegisters = PoolSet.of(AMD64EirRegister.General.pool(), RAX, RCX, RDX, RSI, RDI, R8, R9, R10);

    @Override
    public PoolSet<AMD64EirRegister> callerSavedRegisters() {
        return callerSavedRegisters;
    }

    private final PoolSet<AMD64EirRegister> calleeSavedRegisters = PoolSet.of(AMD64EirRegister.General.pool(), RBX, RBP, R12, R13, R14, R15);

    @Override
    public PoolSet<AMD64EirRegister> calleeSavedRegisters() {
        return calleeSavedRegisters;
    }

    /**
     * Creates an ABI for a VM entry point or VM exit point.
     *
     * @param isVmEntryPoint {@code true} if this is an ABI for methods called from C/native code, {@code false} if it
     *            is for {@code native} methods called from compiled Java code
     * @see C_FUNCTION
     * @see VM_ENTRY_POINT
     */
    public UnixAMD64EirCFunctionABI(VMConfiguration vmConfiguration, boolean isVmEntryPoint) {
        super(vmConfiguration);
        // Native target ABI uses different entry point.
        final TargetABI<AMD64GeneralRegister64, AMD64XMMRegister> originalTargetABI = targetABI();
        final CallEntryPoint callEntryPoint = isVmEntryPoint ? CallEntryPoint.C_ENTRY_POINT : CallEntryPoint.OPTIMIZED_ENTRY_POINT;
        initTargetABI(new TargetABI<AMD64GeneralRegister64, AMD64XMMRegister>(originalTargetABI, originalTargetABI.registerRoleAssignment(), callEntryPoint));
    }

}
