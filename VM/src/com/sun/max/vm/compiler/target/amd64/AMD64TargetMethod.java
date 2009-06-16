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
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.snippet.*;
import com.sun.max.vm.compiler.target.*;

/**
 * @author Bernd Mathiske
 */
public interface AMD64TargetMethod {

    public static final class Static {
        private static final byte RCALL = (byte) 0xe8;
        private static final byte RJMP = (byte) 0xe9;

        private Static() {
        }

        public static int registerReferenceMapSize() {
            return Unsigned.idiv(AMD64GeneralRegister64.ENUMERATOR.numberOfValues(), Bytes.WIDTH);
        }

        public static void patchCallSite(TargetMethod targetMethod, int callOffset, Word callEntryPoint) {
            patchRipCode(targetMethod, callOffset, callEntryPoint.asAddress().toLong(), RCALL);
        }

        public static void forwardTo(TargetMethod oldTargetMethod, TargetMethod newTargetMethod) {
            assert oldTargetMethod != newTargetMethod;
            assert !oldTargetMethod.isNative();
            assert oldTargetMethod.abi().callEntryPoint() != CallEntryPoint.C_ENTRY_POINT;

            final long newOptEntry = CallEntryPoint.OPTIMIZED_ENTRY_POINT.in(newTargetMethod).asAddress().toLong();
            final long newJitEntry = CallEntryPoint.JIT_ENTRY_POINT.in(newTargetMethod).asAddress().toLong();

            patchRipCode(oldTargetMethod, CallEntryPoint.OPTIMIZED_ENTRY_POINT.offsetFromCodeStart(), newOptEntry, RJMP);
            patchRipCode(oldTargetMethod, CallEntryPoint.JIT_ENTRY_POINT.offsetFromCodeStart(), newJitEntry, RJMP);
        }

        private static void patchRipCode(TargetMethod targetMethod, int offset, long target, byte controlTransferOpcode) {
            // TODO: patching RIP code is probably not thread safe!
            final Pointer callSite = targetMethod.codeStart().plus(offset);
            final byte[] code = targetMethod.code();
            final int displacement = (int) (target - (callSite.toLong() + 5));
            ArraySetSnippet.SetByte.setByte(code, offset, controlTransferOpcode);
            ArraySetSnippet.SetByte.setByte(code, offset + 1, (byte) displacement);
            ArraySetSnippet.SetByte.setByte(code, offset + 2, (byte) (displacement >> 8));
            ArraySetSnippet.SetByte.setByte(code, offset + 3, (byte) (displacement >> 16));
            ArraySetSnippet.SetByte.setByte(code, offset + 4, (byte) (displacement >> 24));
        }
    }

}
