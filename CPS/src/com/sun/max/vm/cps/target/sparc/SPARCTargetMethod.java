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
package com.sun.max.vm.cps.target.sparc;

import com.sun.max.asm.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.runtime.*;

/**
 * @author Bernd Mathiske
 * @author Paul Caprioli
 */
public interface SPARCTargetMethod {

    public static final class Static {
        private static final int CALL = 0x40000000;
        private static final int MOV_O7_TO_G1 = 0x8210000F;
        private static final int MOV_G1_TO_O7 = 0x9E100001;
        private static final int BA_A = 0x30800000;

        private Static() {
        }

        public static InstructionSet instructionSet() {
            return InstructionSet.SPARC;
        }

        public static void patchCallSite(TargetMethod targetMethod, int callOffset, Word callEntryPoint) {
            final Pointer callSite = targetMethod.codeStart().plus(callOffset);
            final int displacement = (int) (callEntryPoint.asAddress().toLong() - callSite.toLong());
            final int callInstruction = CALL | (displacement >>> 2);
            if (MaxineVM.isHosted()) {
                final byte[] code = targetMethod.code();
                code[callOffset] = (byte) (callInstruction >> 24);
                code[callOffset + 1] = (byte) (callInstruction >> 16);
                code[callOffset + 2] = (byte) (callInstruction >> 8);
                code[callOffset + 3] = (byte) callInstruction;
            } else {
                callSite.writeInt(0, callInstruction);
            }
        }

        public static void forwardTo(TargetMethod oldTargetMethod, TargetMethod newTargetMethod) {
            assert oldTargetMethod != newTargetMethod;
            assert oldTargetMethod.abi().callEntryPoint() != CallEntryPoint.C_ENTRY_POINT;

            final long newOptEntry = CallEntryPoint.OPTIMIZED_ENTRY_POINT.in(newTargetMethod).asAddress().toLong();
            final long newJitEntry = CallEntryPoint.JIT_ENTRY_POINT.in(newTargetMethod).asAddress().toLong();

            patchCode(oldTargetMethod, CallEntryPoint.OPTIMIZED_ENTRY_POINT.offsetFromCodeStart(), newOptEntry);
            patchCode(oldTargetMethod, CallEntryPoint.JIT_ENTRY_POINT.offsetFromCodeStart(), newJitEntry);
        }

        private static void patchCode(TargetMethod targetMethod, int offset, long target) {
            final Pointer callSite = targetMethod.codeStart().plus(offset);
            final int disp22 = (int) ((target - callSite.toLong()) >>> 2);
            FatalError.check((disp22 >>> 21) == 0x0 || (disp22 >>> 21) == 0x7FF, "Forwarding too far.");
            final int cti = BA_A | (disp22 & 0x3FFFFF);

            assert !MaxineVM.isHosted() : "Should not be invoking patchCode";
            callSite.writeInt(offset, cti);
        }

    }

}
