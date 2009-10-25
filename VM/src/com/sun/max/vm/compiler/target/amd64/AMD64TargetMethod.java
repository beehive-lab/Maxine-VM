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

import com.sun.max.asm.amd64.*;
import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.runtime.*;

/**
 * @author Bernd Mathiske
 * @author Paul Caprioli
 */
public abstract class AMD64TargetMethod {

    private static final int RCALL = 0xe8;
    private static final int RJMP = 0xe9;

    public static int registerReferenceMapSize() {
        return Unsigned.idiv(AMD64GeneralRegister64.ENUMERATOR.numberOfValues(), Bytes.WIDTH);
    }

    /**
     * Patches the offset operand of a 32-bit relative CALL instruction.
     *
     * @param targetMethod the method containing the CALL instruction
     * @param callOffset the offset within the code of {@code targetMethod} of the CALL to be patched
     * @param destination the absolute target address of the CALL
     */
    public static void patchCall32Site(TargetMethod targetMethod, int callOffset, Word destination) {
        patchCode(targetMethod, callOffset, destination.asAddress().toLong(), RCALL);
    }

    /**
     * Patches the offset operand of a 32-bit relative JUMP instruction.
     *
     * @param targetMethod the method containing the JUMP instruction
     * @param jumpOffset the offset within the code of {@code targetMethod} of the JUMP to be patched
     * @param destination the absolute target address of the JUMP
     */
    public static void patchJump32Site(TargetMethod targetMethod, int jumpOffset, Word destination) {
        patchCode(targetMethod, jumpOffset, destination.asAddress().toLong(), RJMP);
    }

    public static void forwardTo(TargetMethod oldTargetMethod, TargetMethod newTargetMethod) {
        assert oldTargetMethod != newTargetMethod;
        assert oldTargetMethod.abi().callEntryPoint() != CallEntryPoint.C_ENTRY_POINT;

        final long newOptEntry = CallEntryPoint.OPTIMIZED_ENTRY_POINT.in(newTargetMethod).asAddress().toLong();
        final long newJitEntry = CallEntryPoint.JIT_ENTRY_POINT.in(newTargetMethod).asAddress().toLong();

        patchCode(oldTargetMethod, CallEntryPoint.OPTIMIZED_ENTRY_POINT.offsetFromCodeStart(), newOptEntry, RJMP);
        patchCode(oldTargetMethod, CallEntryPoint.JIT_ENTRY_POINT.offsetFromCodeStart(), newJitEntry, RJMP);
    }

    private static void patchCode(TargetMethod targetMethod, int offset, long target, int controlTransferOpcode) {
        final Pointer callSite = targetMethod.codeStart().plus(offset);
        final long displacement = (target - (callSite.toLong() + 5L)) & 0xFFFFFFFFL;
        if (MaxineVM.isHosted()) {
            final byte[] code = targetMethod.code();
            code[offset] = (byte) controlTransferOpcode;
            code[offset + 1] = (byte) displacement;
            code[offset + 2] = (byte) (displacement >> 8);
            code[offset + 3] = (byte) (displacement >> 16);
            code[offset + 4] = (byte) (displacement >> 24);
        } else {
            // TODO: Patching code is probably not thread safe!
            //       Patch location must not straddle a cache-line (32-byte) boundary.
            if (false && !callSite.isWordAligned()) {
                FatalError.unexpected("Method " + targetMethod.description() + " entry point is not word aligned.");
            }
            // The read, modify, write below should be changed to simply a write once we have the method entry point alignment fixed.
            final Word patch = callSite.readWord(0).asAddress().and(0xFFFFFF0000000000L).or((displacement << 8) | controlTransferOpcode);
            callSite.writeWord(0, patch);
        }
    }
}
