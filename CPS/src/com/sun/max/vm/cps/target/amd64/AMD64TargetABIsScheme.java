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
package com.sun.max.vm.cps.target.amd64;

import static com.sun.max.asm.amd64.AMD64GeneralRegister64.*;
import static com.sun.max.asm.amd64.AMD64XMMRegister.*;
import static com.sun.max.platform.Platform.*;

import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.asm.amd64.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.cps.target.*;

/**
 * @author Bernd Mathiske
 */
public final class AMD64TargetABIsScheme extends TargetABIsScheme<AMD64GeneralRegister64, AMD64XMMRegister> {

    private static final RegisterRoleAssignment<AMD64GeneralRegister64, AMD64XMMRegister> nativeRegisterRoleAssignment =
        new RegisterRoleAssignment<AMD64GeneralRegister64, AMD64XMMRegister>(AMD64GeneralRegister64.class,
                        RSP, RBP,
                        RSP, RBP,
                        RAX, null, null,
                        AMD64XMMRegister.class, XMM0, null);

    private static final RegisterRoleAssignment<AMD64GeneralRegister64, AMD64XMMRegister> jitRegisterRoleAssignment =
        new RegisterRoleAssignment<AMD64GeneralRegister64, AMD64XMMRegister>(AMD64GeneralRegister64.class,
                        RSP, RBP,
                        RSP, RBP,
                        RAX, R11, R14,
                        AMD64XMMRegister.class, XMM0, XMM15);

    private static final RegisterRoleAssignment<AMD64GeneralRegister64, AMD64XMMRegister> optimizedJavaRegisterRoleAssignment =
        new RegisterRoleAssignment<AMD64GeneralRegister64, AMD64XMMRegister>(AMD64GeneralRegister64.class,
                        RSP, RBP,
                        RSP, RSP,
                        RAX, R11, R14,
                        AMD64XMMRegister.class, XMM0, XMM15);

    private static final List<AMD64GeneralRegister64> integerParameterRegisters = Arrays.asList(RDI, RSI, RDX, RCX, R8, R9);
    private static final List<AMD64XMMRegister> floatingPointParameterRegisters = Arrays.asList(XMM0, XMM1, XMM2, XMM3, XMM4, XMM5, XMM6, XMM7);

    private static final int NULL_STACK_BIAS = 0;

    @HOSTED_ONLY
    static TargetABI<AMD64GeneralRegister64, AMD64XMMRegister> createAMD64TargetABI(RegisterRoleAssignment<AMD64GeneralRegister64, AMD64XMMRegister> registerRoleAssignment, CallEntryPoint callEntryPoint) {
        return  new TargetABI<AMD64GeneralRegister64, AMD64XMMRegister>(registerRoleAssignment, callEntryPoint,
                        integerParameterRegisters, integerParameterRegisters, floatingPointParameterRegisters,
                        false, true, target().stackAlignment, NULL_STACK_BIAS);
    }

    @HOSTED_ONLY
    public AMD64TargetABIsScheme() {
        super(createAMD64TargetABI(nativeRegisterRoleAssignment, CallEntryPoint.C_ENTRY_POINT),
              createAMD64TargetABI(jitRegisterRoleAssignment, CallEntryPoint.JIT_ENTRY_POINT),
              createAMD64TargetABI(optimizedJavaRegisterRoleAssignment, CallEntryPoint.OPTIMIZED_ENTRY_POINT));
    }
}
