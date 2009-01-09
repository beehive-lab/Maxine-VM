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
package com.sun.max.vm.compiler.target.sparc.systemV;

import static com.sun.max.asm.sparc.FPR.*;
import static com.sun.max.asm.sparc.GPR.*;

import com.sun.max.asm.sparc.*;
import com.sun.max.vm.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.compiler.target.sparc.*;

/**
 * Target ABI following the SPARC V9 System V conventions.
 * Note: the JIT differs from the normal system V conventions in that it uses L0 as a ABI frame pointer.
 * The JIT doesn't use register windows for jit-ed code's stack frame and uses an explicitly managed frame pointer
 * using the local register L0.
 *
 * TODO: floating point parameter registers
 *
 * @author Bernd Mathiske
 * @author Laurent Daynes
 */
public final class SPARCSystemVTargetABIsScheme extends SPARCTargetABIsScheme {

    private static final RegisterRoleAssignment<GPR, FPR> _javaRegisterRoleAssignment =
        new RegisterRoleAssignment<GPR, FPR>(GPR.class, O6, I6, O6, I6, I0, O0, G1, G2, L7, FPR.class, F0, F30, I7, O7);

    private static final RegisterRoleAssignment<GPR, FPR> _javaJitRegisterRoleAssignment =
        new RegisterRoleAssignment<GPR, FPR>(GPR.class, O6, I6, O6, L6, I0, O0, G1, G2, L7, FPR.class, F0, F30, I7, O7);

    public SPARCSystemVTargetABIsScheme(VMConfiguration vmConfiguration) {
        super(vmConfiguration,
                        createSPARC64TargetABI(_javaJitRegisterRoleAssignment, CallEntryPoint.JIT_ENTRY_POINT, _incomingIntegerParameterRegisters, _outgoingIntegerParameterRegisters, _floatingPointParameterRegisters, false, false),
                        createSPARC64TargetABI(_javaRegisterRoleAssignment, CallEntryPoint.OPTIMIZED_ENTRY_POINT, _incomingIntegerParameterRegisters, _outgoingIntegerParameterRegisters, _floatingPointParameterRegisters, true, false));
    }

}
