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
package com.sun.max.vm.compiler.eir.ia32.unix;

import com.sun.max.asm.ia32.*;
import com.sun.max.vm.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.runtime.*;

/** 
 * ABI for the template-based JIT on IA32 Unix.
 * The primary difference to the optco's normal Java ABI is that a frame pointer is needed.
 * The stack pointer is used explicitly by the templates to manage an expression stack.
 * 
 * @author Laurent Daynes
 */
public class UnixIA32EirTemplateABI extends UnixIA32EirJavaABI {

    public UnixIA32EirTemplateABI(VMConfiguration vmConfiguration) {
        super(vmConfiguration);
        // final TargetABI<IA32GeneralRegister32, IA32XMMRegister> originalTargetABI = super.targetABI();
        final IA32GeneralRegister32[] registerRoleAssignment = new IA32GeneralRegister32 [VMRegister.Role.VALUES.length()];
        for (VMRegister.Role registerRole : VMRegister.Role.VALUES) {
            registerRoleAssignment[registerRole.ordinal()] = /*TODO*/ null;
        }
        registerRoleAssignment[VMRegister.Role.ABI_FRAME_POINTER.ordinal()] =  IA32GeneralRegister32.EBP;
        final TargetABI<IA32GeneralRegister32, IA32XMMRegister> templateTargetABI = /*TODO*/ null;
        initTargetABI(templateTargetABI);
    }
}
