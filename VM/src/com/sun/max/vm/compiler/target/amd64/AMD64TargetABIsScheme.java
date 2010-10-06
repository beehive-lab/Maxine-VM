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
package com.sun.max.vm.compiler.target.amd64;

import static com.sun.max.asm.amd64.AMD64GeneralRegister64.*;
import static com.sun.max.asm.amd64.AMD64XMMRegister.*;
import static com.sun.max.platform.Platform.*;

import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.asm.amd64.*;
import com.sun.max.platform.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.*;

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
                        false, true, stackFrameAlignment(), NULL_STACK_BIAS);
    }

    @HOSTED_ONLY
    public AMD64TargetABIsScheme() {
        super(createAMD64TargetABI(nativeRegisterRoleAssignment, CallEntryPoint.C_ENTRY_POINT),
              createAMD64TargetABI(jitRegisterRoleAssignment, CallEntryPoint.JIT_ENTRY_POINT),
              createAMD64TargetABI(optimizedJavaRegisterRoleAssignment, CallEntryPoint.OPTIMIZED_ENTRY_POINT));
    }

    @HOSTED_ONLY
    private static int stackFrameAlignment() {
        if (platform().operatingSystem == OperatingSystem.DARWIN) {
            // Darwin requires 16-byte stack frame alignment.
            return 16;
        }
        if (platform().operatingSystem == OperatingSystem.LINUX) {
            // Linux apparently also requires it for functions that pass floating point functions on the stack.
            // One such function in the Maxine code base is log_print_float() in log.c which passes a float
            // value to fprintf on the stack. However, gcc doesn't fix the alignment itself so we simply
            // adopt the global convention on Linux of 16-byte alignment for stacks. If this is a performance issue,
            // this can later be refined to only be for JNI stubs that pass a float or double to native code.
            return 16;
        }
        return Word.size();
    }
}
