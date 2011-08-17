/*
 * Copyright (c) 2011, 2011, Oracle and/or its affiliates. All rights reserved.
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

import java.util.*;

import com.sun.cri.ci.*;
import com.sun.max.io.*;
import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.amd64.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.stack.StackFrameWalker.Cursor;

/**
 * Stubs are for manually-assembled target code. Currently, a stub has the maximum of one
 * direct call to another method, so the callee is passed into the constructor directly.
 * Stack walking of stub frames is done with the same code as for optimized compiler frames.
 */
public class Stub extends TargetMethod {

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
         * Transition when returning from a compiler stub call to a method being deoptimized.
         * This stub saves all registers (as a compiler stub call has callee save sematics)
         * and retrieves the return value from the stack.
         *
         * @see #CompilerStub
         */
        DeoptStubFromCompilerStub,

        /**
         * The trap stub.
         */
        TrapStub;
    }

    public final Type type;

    @Override
    public Stub.Type stubType() {
        return type;
    }

    public Stub(Type type, String stubName, int frameSize, byte[] code, int directCallPos, ClassMethodActor callee, int registerRestoreEpilogueOffset) {
        super(stubName, CallEntryPoint.OPTIMIZED_ENTRY_POINT);
        this.type = type;
        this.setFrameSize(frameSize);
        this.setRegisterRestoreEpilogueOffset(registerRestoreEpilogueOffset);

        final TargetBundleLayout targetBundleLayout = new TargetBundleLayout(0, 0, code.length);
        Code.allocate(targetBundleLayout, this);
        setData(null, null, code);
        if (directCallPos != -1) {
            int directCallStopPos = stopPosForDirectCallPos(directCallPos);
            assert callee != null;
            setStopPositions(new int[] {directCallStopPos}, new Object[] {callee}, 0, 0);
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
        CiDebugInfo[] debugInfos = initStopPositions(tm);
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
            case TrapStub:
                return rc.trapStub.csl;
            case UncommonTrapStub:
                return rc.uncommonTrapStub.csl;
        }
        return null;
    }

    @Override
    public Pointer returnAddressPointer(Cursor frame) {
        if (platform().isa == ISA.AMD64) {
            return AMD64TargetMethodUtil.returnAddressPointer(frame);
        } else {
            throw FatalError.unimplemented();
        }
    }

    @Override
    public void advance(Cursor current) {
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
    public boolean acceptStackFrameVisitor(Cursor current, StackFrameVisitor visitor) {
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
    public void gatherCalls(Set<MethodActor> directCalls, Set<MethodActor> virtualCalls, Set<MethodActor> interfaceCalls, Set<MethodActor> inlinedMethods) {
        if (directCallees != null && directCallees.length != 0) {
            assert directCallees.length == 1 && directCallees[0] instanceof ClassMethodActor;
            directCalls.add((MethodActor) directCallees[0]);
        }
    }

    @Override
    public boolean isPatchableCallSite(Address callSite) {
        FatalError.unexpected("Stub should never be patched");
        return false;
    }

    @Override
    public Address fixupCallSite(int callOffset, Address callEntryPoint) {
        return AMD64TargetMethodUtil.fixupCall32Site(this, callOffset, callEntryPoint);
    }

    @Override
    public Address patchCallSite(int callOffset, Address callEntryPoint) {
        throw FatalError.unexpected("Stub should never be patched");
    }

    @Override
    public Address throwAddressToCatchAddress(boolean isTopFrame, Address throwAddress, Class< ? extends Throwable> throwableClass) {
        if (isTopFrame) {
            throw FatalError.unexpected("Exception occurred in stub frame");
        }
        return Address.zero();
    }

    @Override
    public void traceDebugInfo(IndentWriter writer) {
    }

    @Override
    public void traceExceptionHandlers(IndentWriter writer) {
    }

    @Override
    public void prepareReferenceMap(Cursor current, Cursor callee, StackReferenceMapPreparer preparer) {
    }

    @Override
    public void catchException(Cursor current, Cursor callee, Throwable throwable) {
        // Exceptions do not occur in stubs
    }
}
