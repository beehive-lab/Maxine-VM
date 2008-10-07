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
/*VCSID=fc8cd373-998a-4100-af46-9d6a63c72491*/
package com.sun.max.vm.compiler.eir.sparc;

import com.sun.max.asm.sparc.*;
import com.sun.max.collect.*;
import com.sun.max.vm.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.runtime.VMRegister.*;

/**
 *
 * @author Bernd Mathiske
 * @author Laurent Daynes
 */
public class SPARCEirCFunctionABI extends SPARCEirJavaABI {

    private final PoolSet<SPARCEirRegister> _calleeSavedRegisters;

    @Override
    public PoolSet<SPARCEirRegister> calleeSavedRegisters() {
        return _calleeSavedRegisters;
    }

    /**
     * Function annotated with C_FUNCTION falls currently in two categories: function used only by native code and that can only be called from native (i.e., C) code.
     * This is the case for "JNI" function.
     */
    public SPARCEirCFunctionABI(VMConfiguration vmConfiguration, boolean onlyCalledFromC) {
        super(vmConfiguration);
        // Can't trust C code to preserve application global register.
        _calleeSavedRegisters = PoolSet.of(SPARCEirRegister.GeneralPurpose.pool(), SPARCEirRegister.GeneralPurpose.from(targetABI().registerRoleAssignment().integerRegisterActingAs(Role.SAFEPOINT_LATCH)));
        // Native target ABI uses different entry point.
        final TargetABI<GPR, FPR> originalTargetABI = targetABI();
        initTargetABI(new TargetABI<GPR, FPR>(originalTargetABI, originalTargetABI.registerRoleAssignment(), onlyCalledFromC ? CallEntryPoint.C_ENTRY_POINT : CallEntryPoint.OPTIMIZED_ENTRY_POINT));
    }

}
