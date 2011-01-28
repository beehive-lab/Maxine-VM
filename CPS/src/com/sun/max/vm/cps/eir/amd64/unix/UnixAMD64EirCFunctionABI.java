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

import static com.sun.max.vm.cps.eir.amd64.AMD64EirRegister.General.*;

import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.asm.amd64.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.cps.eir.amd64.*;
import com.sun.max.vm.cps.target.*;

/**
 * C functions that we implement behave almost like native functions.
 * They have the same parameters and callee save registers.
 * For simplicity, we do use the stack pointer as frame pointer, though,
 * just as we are used to in Java methods.
 *
 * @author Bernd Mathiske
 */
public class UnixAMD64EirCFunctionABI extends UnixAMD64EirJavaABI {

    /**
     * Creates an ABI for a VM entry point or VM exit point.
     * @param isVmEntryPoint {@code true} if this is an ABI for methods called from C/native code, {@code false} if it
     *            is for {@code native} methods called from compiled Java code
     *
     * @see C_FUNCTION
     * @see VM_ENTRY_POINT
     */
    public UnixAMD64EirCFunctionABI(boolean isVmEntryPoint) {
        if (isVmEntryPoint) {
            calleeSavedRegisters = Arrays.asList(new AMD64EirRegister[] {RBX, RBP, R12, R13, R14, R15});
        } else {
            calleeSavedRegisters = Collections.emptyList();
        }
        // Native target ABI uses different entry point.
        final TargetABI<AMD64GeneralRegister64, AMD64XMMRegister> originalTargetABI = targetABI();
        final CallEntryPoint callEntryPoint = isVmEntryPoint ? CallEntryPoint.C_ENTRY_POINT : CallEntryPoint.OPTIMIZED_ENTRY_POINT;
        initTargetABI(new TargetABI<AMD64GeneralRegister64, AMD64XMMRegister>(originalTargetABI, originalTargetABI.registerRoleAssignment, callEntryPoint));
    }

}
