/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.compiler.target;

import static com.sun.max.platform.Platform.*;
import static com.sun.max.vm.MaxineVM.*;
import static com.sun.max.vm.compiler.target.Safepoints.*;
import static com.sun.max.vm.compiler.target.Stub.Type.*;

import java.util.*;

import com.sun.cri.bytecode.*;
import com.sun.cri.ci.*;
import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.code.CodeManager.Lifespan;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.amd64.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.*;

/**
 * Stubs are for manually-assembled target code. Currently, a stub has the maximum of one
 * direct call to another method, so the callee is passed into the constructor directly.
 * Stack walking of stub frames is done with the same code as for optimized compiler frames.
 */
public final class Stub extends TargetMethod {

    public enum Type {
        /**
         * Trampoline for virtual method dispatch (i.e. translation of {@link Bytecodes#INVOKEVIRTUAL}).
         */
        VirtualTrampoline,

        /**
         * Trampoline for interface method dispatch (i.e. translation of {@link Bytecodes#INVOKEINTERFACE}).
         */
        InterfaceTrampoline,

        /**
         * Trampoline for static method call (i.e. translation of {@link Bytecodes#INVOKESPECIAL} or {@link Bytecodes#INVOKESTATIC}).
         */
        StaticTrampoline,

        /**
         * A stub that performs an operation on behalf of compiled code.
         * These stubs are called with a callee-save convention; the stub must save any
         * registers it may destroy and then restore them upon return. This allows the register
         * allocator to ignore calls to such stubs. Parameters to compiler stubs are
         * passed on the stack in order to preserve registers for the rest of the code.
         */
        CompilerStub,

        UnwindStub,

        UnrollStub,

        UncommonTrapStub,

        /**
         * Transition when returning from a normal call to a method being deoptimized.
         */
        DeoptStub,

        /**
         * Transition when returning from a compiler stub to a method being deoptimized. This
         * stub creates an intermediate frame to (re)save all the registers saved by a compiler stub.
         *
         * @see #CompilerStub
         */
        DeoptStubFromCompilerStub,

        /**
         * Transition when returning from a trap stub to a method being deoptimized. This
         * stub creates an intermediate frame to (re)save all the registers saved by the trap stub.
         *
         * @see Stubs#genTrapStub()
         */
        DeoptStubFromSafepoint,

        /**
         * The trap stub.
         */
        TrapStub,

        /**
         * A place holder for invalid indexes of dispatch tables (virtual / interface).
         */
        InvalidIndexTrampoline
    }

    @HOSTED_ONLY
    private static final Stub canonicalInvalidIndexStub = new Stub();

    @HOSTED_ONLY
    public static Stub canonicalInvalidIndexStub() {
        return canonicalInvalidIndexStub;
    }

    /**
     * Determines if a given address in a given target method denotes the entry point of a deoptimization stub.
     *
     * @param ip a code address
     * @param tm the target method {@linkplain Code#codePointerToTargetMethod(Pointer) found} in the code cache based on {@code ip}
     */
    public static boolean isDeoptStubEntry(Pointer ip, TargetMethod tm) {
        if (tm != null && (tm.is(DeoptStub) || tm.is(DeoptStubFromCompilerStub) || tm.is(DeoptStubFromSafepoint))) {
            return ip.asPointer().equals(tm.codeStart());
        } else {
            return false;
        }
    }

    public final Type type;

    @Override
    public Stub.Type stubType() {
        return type;
    }

    @HOSTED_ONLY
    private Stub() {
        super("Invalid Index stub",  CallEntryPoint.OPTIMIZED_ENTRY_POINT);
        type = InvalidIndexTrampoline;
    }

    public Stub(Type type, String stubName, int frameSize, byte[] code, int callPos, int callSize, ClassMethodActor callee, int registerRestoreEpilogueOffset) {
        super(stubName, CallEntryPoint.OPTIMIZED_ENTRY_POINT);
        this.type = type;
        this.setFrameSize(frameSize);
        this.setRegisterRestoreEpilogueOffset(registerRestoreEpilogueOffset);

        final TargetBundleLayout targetBundleLayout = new TargetBundleLayout(0, 0, code.length);
        Code.allocate(targetBundleLayout, this);
        setData(null, null, code);
        if (callPos != -1) {
            int safepointPos = Safepoints.safepointPosForCall(callPos, callSize);
            assert callee != null;
            setSafepoints(new Safepoints(Safepoints.make(safepointPos, callPos, DIRECT_CALL)), new Object[] {callee});
        }
        if (!isHosted()) {
            linkDirectCalls();
        }
    }

    public Stub(Type type, String name, CiTargetMethod tm) {
        super(name, CallEntryPoint.OPTIMIZED_ENTRY_POINT);
        this.type = type;

        initCodeBuffer(tm, true);
        initFrameLayout(tm);
        CiDebugInfo[] debugInfos = initSafepoints(tm);
        for (CiDebugInfo info : debugInfos) {
            assert info == null;
        }
    }

    @Override
    public CiCalleeSaveLayout calleeSaveLayout() {
        final RegisterConfigs rc = vm().registerConfigs;
        switch (type) {
            case DeoptStubFromCompilerStub:
            case CompilerStub:
                return rc.compilerStub.csl;
            case VirtualTrampoline:
            case StaticTrampoline:
            case InterfaceTrampoline:
                return rc.trampoline.csl;
            case DeoptStubFromSafepoint:
            case TrapStub:
                return rc.trapStub.csl;
            case UncommonTrapStub:
                return rc.uncommonTrapStub.csl;
        }
        return null;
    }

    @Override
    public Lifespan lifespan() {
        return Lifespan.LONG;
    }

    @Override
    public Pointer returnAddressPointer(StackFrameCursor frame) {
        if (platform().isa == ISA.AMD64) {
            return AMD64TargetMethodUtil.returnAddressPointer(frame);
        } else {
            throw FatalError.unimplemented();
        }
    }

    @Override
    public void advance(StackFrameCursor current) {
        if (platform().isa == ISA.AMD64) {
            CiCalleeSaveLayout csl = calleeSaveLayout();
            Pointer csa = Pointer.zero();
            if (csl != null) {
                assert csl.frameOffsetToCSA != Integer.MAX_VALUE : "stub should have fixed offset for CSA";
                csa = current.sp().plus(csl.frameOffsetToCSA);
            }
            AMD64TargetMethodUtil.advance(current, csl, csa);
        } else {
            throw FatalError.unimplemented();
        }
    }

    @Override
    @HOSTED_ONLY
    public boolean acceptStackFrameVisitor(StackFrameCursor current, StackFrameVisitor visitor) {
        if (platform().isa == ISA.AMD64) {
            return AMD64TargetMethodUtil.acceptStackFrameVisitor(current, visitor);
        } else {
            throw FatalError.unimplemented();
        }
    }

    @Override
    public VMFrameLayout frameLayout() {
        if (platform().isa == ISA.AMD64) {
            return AMD64TargetMethodUtil.frameLayout(this);
        } else {
            throw FatalError.unimplemented();
        }
    }

    @Override
    @HOSTED_ONLY
    public void gatherCalls(Set<MethodActor> directCalls, Set<MethodActor> virtualCalls, Set<MethodActor> interfaceCalls, Set<MethodActor> inlinedMethods) {
        if (directCallees != null && directCallees.length != 0) {
            assert directCallees.length == 1 && directCallees[0] instanceof ClassMethodActor;
            directCalls.add((MethodActor) directCallees[0]);
        }
    }

    @Override
    public boolean isPatchableCallSite(CodePointer callSite) {
        FatalError.unexpected("Stub should never be patched");
        return false;
    }

    @Override
    public CodePointer fixupCallSite(int callOffset, CodePointer callEntryPoint) {
        return AMD64TargetMethodUtil.fixupCall32Site(this, callOffset, callEntryPoint);
    }

    @Override
    public CodePointer patchCallSite(int callOffset, CodePointer callEntryPoint) {
        throw FatalError.unexpected("Stub should never be patched");
    }

    @Override
    public CodePointer throwAddressToCatchAddress(CodePointer throwAddress, Throwable exception) {
        return CodePointer.zero();
    }

    @Override
    public void prepareReferenceMap(StackFrameCursor current, StackFrameCursor callee, FrameReferenceMapVisitor preparer) {
    }

    @Override
    public void catchException(StackFrameCursor current, StackFrameCursor callee, Throwable throwable) {
        // Exceptions do not occur in stubs
    }
}
