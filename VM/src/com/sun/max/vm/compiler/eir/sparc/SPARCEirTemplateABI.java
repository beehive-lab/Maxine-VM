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
/*VCSID=f8888cf7-8da1-41f0-ab2d-131d387c2a75*/
package com.sun.max.vm.compiler.eir.sparc;

import com.sun.max.asm.sparc.*;
import com.sun.max.lang.*;
import com.sun.max.vm.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.runtime.*;

/**
 * ABI for templates produced by the optimizing compiler and used by template-based code generator on SPARC Unix.
 * As for the ABI for compiling standard methods, local variables of the templates are allocated at offsets relative to a frame pointer.
 * However, templates use the frame pointer of the JIT, which is a local register and not the platform-defined frame pointer.
 * Further, the frame pointer is unbiased.
 * Also, the frame size for a template indicates only the space used for its local variables, and not the mandatory saving areas required by the
 * SPARC / Solaris ABI for register window and parameter passing.
 *
 * @author Laurent Daynes
 */
public class SPARCEirTemplateABI extends SPARCEirJavaABI {

    public SPARCEirTemplateABI(VMConfiguration vmConfiguration) {
        super(vmConfiguration);
        // Customize the ABI. We need to change the ABI frame pointer for that of the JIT, and remove it from the allocatable set of register.

        final GPR framePointer = StaticLoophole.cast(vmConfiguration.targetABIsScheme().jitABI().framePointer());
        final TargetABI<GPR, FPR> originalTargetABI = super.targetABI();
        final RegisterRoleAssignment<GPR, FPR> registerRoleAssignement = new RegisterRoleAssignment<GPR,  FPR>(new RegisterRoleAssignment<GPR,  FPR>(originalTargetABI.registerRoleAssignment(),
                        VMRegister.Role.ABI_FRAME_POINTER, framePointer), VMRegister.Role.ABI_RETURN, GPR.O0);
        final TargetABI<GPR, FPR> templateTargetABI = new TargetABI<GPR, FPR>(originalTargetABI, registerRoleAssignement, CallEntryPoint.OPTIMIZED_ENTRY_POINT);
        initTargetABI(templateTargetABI);
        makeUnallocatable(SPARCEirRegister.GeneralPurpose.from(framePointer));
    }


    @Override
    public int frameSize(int numLocalStackSlots) {
        return targetABI().alignFrameSize(numLocalStackSlots * stackSlotSize());
    }
}
