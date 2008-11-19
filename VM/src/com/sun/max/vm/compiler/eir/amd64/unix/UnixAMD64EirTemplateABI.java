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

import com.sun.max.asm.amd64.*;
import com.sun.max.collect.*;
import com.sun.max.vm.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.eir.amd64.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.runtime.*;

/**
 * ABI for templates produced by the optimizing compiler and used by template-based code generator on AMD64 Unix.
 * The primary differences to the opto's normal Java ABI are: (i) spilling should be performed relative to a frame pointer
 * distinct from the stack pointer (the stack pointer being used explicitly by the templates to manage an expression stack);
 * and (ii), no adapter frames need be generated for templates.
 *
 * @author Laurent Daynes
 */
public class UnixAMD64EirTemplateABI extends UnixAMD64EirJavaABI {

    public UnixAMD64EirTemplateABI(VMConfiguration vmConfiguration) {
        super(vmConfiguration);
        final TargetABI<AMD64GeneralRegister64, AMD64XMMRegister> originalTargetABI = super.targetABI();
        final AMD64GeneralRegister64 bp = originalTargetABI.registerRoleAssignment().integerRegisterActingAs(VMRegister.Role.CPU_FRAME_POINTER);
        final RegisterRoleAssignment<AMD64GeneralRegister64, AMD64XMMRegister> registerRoleAssignment =
            new RegisterRoleAssignment<AMD64GeneralRegister64, AMD64XMMRegister>(originalTargetABI.registerRoleAssignment(),
                            VMRegister.Role.ABI_FRAME_POINTER, bp);
        initTargetABI(new TargetABI<AMD64GeneralRegister64, AMD64XMMRegister>(originalTargetABI, registerRoleAssignment, CallEntryPoint.OPTIMIZED_ENTRY_POINT));
        makeUnallocatable(AMD64EirRegister.General.RBP);
    }

    private final PoolSet<AMD64EirRegister> _calleeSavedRegisters = PoolSet.noneOf(AMD64EirRegister.General.pool());

    @Override
    public PoolSet<AMD64EirRegister> calleeSavedRegisters() {
        return _calleeSavedRegisters;
    }
}
