/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.sun.max.vm.cps.eir.amd64.unix;

import com.sun.max.annotate.*;
import com.sun.max.asm.amd64.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.cps.eir.amd64.*;
import com.sun.max.vm.cps.target.*;
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

    @HOSTED_ONLY
    public UnixAMD64EirTemplateABI() {
        final TargetABI<AMD64GeneralRegister64, AMD64XMMRegister> originalTargetABI = super.targetABI();
        final AMD64GeneralRegister64 bp = originalTargetABI.registerRoleAssignment.integerRegisterActingAs(VMRegister.Role.CPU_FRAME_POINTER);
        final RegisterRoleAssignment<AMD64GeneralRegister64, AMD64XMMRegister> registerRoleAssignment =
            new RegisterRoleAssignment<AMD64GeneralRegister64, AMD64XMMRegister>(originalTargetABI.registerRoleAssignment,
                            VMRegister.Role.ABI_FRAME_POINTER, bp);
        initTargetABI(new TargetABI<AMD64GeneralRegister64, AMD64XMMRegister>(originalTargetABI, registerRoleAssignment, CallEntryPoint.OPTIMIZED_ENTRY_POINT));
        makeUnallocatable(AMD64EirRegister.General.RBP);
    }

    /**
     * Indicate whether this ABI is for templates.
     * @return true if ABI is for generating templates.
     */
    @HOSTED_ONLY
    @Override
    public boolean templatesOnly() {
        return true;
    }
}
