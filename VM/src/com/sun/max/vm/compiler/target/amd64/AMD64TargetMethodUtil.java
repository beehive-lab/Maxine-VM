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

import com.sun.c1x.target.amd64.*;
import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.runtime.*;

/**
 * A utility class factoring out code common to all AMD64 target method.
 *
 * @author Bernd Mathiske
 * @author Paul Caprioli
 */
public final class AMD64TargetMethodUtil {
    private static final int RCALL = 0xe8;
    private static final int RJMP = 0xe9;

    /**
     * Lock to avoid race on concurrent icache invalidation when patching target methods.
     */
    private static final Object PatchingLock = new Object();

    public static int registerReferenceMapSize() {
        return Unsigned.idiv(AMD64.cpuRegisters.length, Bytes.WIDTH);
    }

    public static boolean isPatchableCallSite(Address callSite) {
        // We only update the disp of the call instruction.
        // C1X imposes that disp of the call be aligned to a word boundary.
        // This may cause up to 7 nops to be inserted before a call.
        // CPS use the less conservative approach of requiring only that the whole
        // call instructions fits in a single word (and thus, guaranteed to be within a single cache line).
        // The following takes care of both
        final Address endOfCallSite = callSite.plus(DIRECT_METHOD_CALL_INSTRUCTION_LENGTH - 1);
        return callSite.plus(1).isWordAligned() ? true :
        // last byte of call site:
        callSite.roundedDownBy(8).equals(endOfCallSite.roundedDownBy(8));
    }

    /**
     * Patches the offset operand of a 32-bit relative CALL instruction.
     *
     * @param targetMethod the method containing the CALL instruction
     * @param callOffset the offset within the code of {@code targetMethod} of the CALL to be patched
     * @param destination the absolute target address of the CALL
     */
    public static void fixupCall32Site(TargetMethod targetMethod, int callOffset, Address destination) {
        fixupCode(targetMethod, callOffset, destination.asAddress(), RCALL);
    }

    /**
     * Set up a forwarding jump from an old copy of a method to a new one.
     * @param oldTargetMethod
     * @param newTargetMethod
     */
    public static void forwardTo(TargetMethod oldTargetMethod, TargetMethod newTargetMethod) {
        assert oldTargetMethod != newTargetMethod;
        assert oldTargetMethod.callEntryPoint != CallEntryPoint.C_ENTRY_POINT;

        final Address newOptEntry = CallEntryPoint.OPTIMIZED_ENTRY_POINT.in(newTargetMethod).asAddress();
        final Address newJitEntry = CallEntryPoint.JIT_ENTRY_POINT.in(newTargetMethod).asAddress();

        // FIXME: Shouldn't these be made mt-safe using some variant of mtSafePatchCallSite ?
        fixupCode(oldTargetMethod, CallEntryPoint.OPTIMIZED_ENTRY_POINT.offset(), newOptEntry, RJMP);
        fixupCode(oldTargetMethod, CallEntryPoint.JIT_ENTRY_POINT.offset(), newJitEntry, RJMP);
    }

    private static final long DIRECT_METHOD_CALL_INSTRUCTION_LENGTH = 5L;

    private static void fixupCode(TargetMethod targetMethod, int offset, Address target, int controlTransferOpcode) {
        final Pointer callSite = targetMethod.codeStart().plus(offset);
        long displacement = target.minus(callSite.plus(DIRECT_METHOD_CALL_INSTRUCTION_LENGTH)).toLong();
        FatalError.check((int) displacement == displacement, "Code displacement out of 32-bit range");
        displacement = displacement & 0xFFFFFFFFL;
        if (MaxineVM.isHosted()) {
            final byte[] code = targetMethod.code();
            code[offset] = (byte) controlTransferOpcode;
            code[offset + 1] = (byte) displacement;
            code[offset + 2] = (byte) (displacement >> 8);
            code[offset + 3] = (byte) (displacement >> 16);
            code[offset + 4] = (byte) (displacement >> 24);
        } else {
            // Don't care about any particular alignment here. Can fixup any control of transfer code as there isn't concurrency issues.
            callSite.writeByte(0, (byte) controlTransferOpcode);
            callSite.writeByte(1, (byte) displacement);
            callSite.writeByte(2, (byte) (displacement >> 8));
            callSite.writeByte(3, (byte) (displacement >> 16));
            callSite.writeByte(4, (byte) (displacement >> 24));
        }
    }

    // MT-safe replacement of the displacement of a direct call.
    public static void mtSafePatchCallDisplacement(TargetMethod targetMethod, Pointer callSite, Address target) {
        if (!isPatchableCallSite(callSite)) {
            FatalError.unexpected(" invalid patchable call site:  " + callSite.toHexString());
        }
        long displacement = target.minus(callSite.plus(DIRECT_METHOD_CALL_INSTRUCTION_LENGTH)).toLong();
        FatalError.check((int) displacement == displacement, "Code displacement out of 32-bit range");
        displacement = displacement & 0xFFFFFFFFL;
        synchronized (PatchingLock) {
            // Just to prevent concurrent writing and invalidation to the same instruction cache line
            // (although the lock excludes ALL concurrent patching)
            callSite.writeInt(1,  (int) displacement);
            // Don't need icache invalidation to be correct (see AMD64's Architecture Programmer Manual Vol.2, p173 on self-modifying code)
        }
    }

    // Disable instance creation.
    private AMD64TargetMethodUtil() {
    }
}
