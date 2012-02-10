/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.code;

import static com.sun.max.platform.Platform.*;
import static com.sun.max.vm.compiler.CallEntryPoint.*;

import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.compiler.target.amd64.*;
import com.sun.max.vm.debug.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.thread.*;

/**
 * Check all dispatch table entries, direct call sites, and return addresses.
 * They must be in either of the code regions (but not in baseline from-space),
 * and dispatch table entries and direct call sites must refer to valid entry points.
 */
public final class CodeCacheValidation extends VmOperation {

    public static final CodeCacheValidation instance = new CodeCacheValidation();

    final class StackValidator extends RawStackFrameVisitor {
        @Override
        public boolean visitFrame(StackFrameCursor current, StackFrameCursor callee) {
            final TargetMethod tm = callee.targetMethod();
            if (tm == null) {
                return true;
            }
            final Pointer rap = tm.returnAddressPointer(callee);
            final CodePointer ret = CodePointer.from(rap.readWord(0));
            // native callee?
            if (current.targetMethod() == null && !current.sp().isZero()) {
                return true;
            }
            assert validCodeAddress(ret) : "invalid return address in " + tm + " -> " + ret.to0xHexString();
            return true;
        }
    }

    final class DirectCallValidator implements TargetMethod.Closure {
        @Override
        public boolean doTargetMethod(TargetMethod targetMethod) {
            validateDirectCallsOf(targetMethod);
            return true;
        }
    }

    final class DispatchTableValidator implements ClassActor.Closure {
        @Override
        public boolean doClass(ClassActor classActor) {
            final DynamicHub dhub = classActor.dynamicHub();
            if (dhub != null) {
                validateHub(dhub, DynamicHub.vTableStartIndex(), true);
            }

            final StaticHub shub = classActor.staticHub();
            if (shub != null) {
                validateHub(shub, Hub.vTableStartIndex(), !(classActor.isInterface() || classActor.isPrimitiveClassActor()));
            }

            validateMethods(classActor.localStaticMethodActors());
            validateMethods(classActor.localVirtualMethodActors());

            return true;
        }

        private void validateMethods(MethodActor[] methods) {
            for (MethodActor ma : methods) {
                if (ma instanceof ClassMethodActor) {
                    final TargetMethod tm = ((ClassMethodActor) ma).currentTargetMethod();
                    if (tm != null) {
                        assert validCodeAddress(tm.codeStart()) : "invalid method code start: " + tm + "@" + tm.codeStart().to0xHexString();
                    }
                }
            }
        }
    }

    final class TargetStateValidator implements TargetMethod.Closure {
        @Override
        public boolean doTargetMethod(TargetMethod targetMethod) {
            if (CodeManager.runtimeBaselineCodeRegion.isInToSpace(targetMethod.codeStart().toAddress())) {
                final ClassMethodActor cma = targetMethod.classMethodActor;
                assert cma != null : "class method actor null for " + targetMethod;
                if (!targetMethod.isProtected()) { // target state for a protected method is not yet initialised
                    assert cma.compiledState != Compilations.EMPTY : "target state null for " + targetMethod;
                    final TargetMethod tm = cma.currentTargetMethod();
                    assert tm != null : "current target method null for " + targetMethod + " (via class method actor " + cma + ")";
                    final CodePointer cs = tm.codeStart();
                    assert validCodeAddress(cs) : "target state not referencing to-space for " + targetMethod + ": " + cs.to0xHexString();
                }
            } else {
                assert targetMethod.classMethodActor.compiledState == Compilations.EMPTY : "target state SHOULD BE null for " + targetMethod;
            }
            return true;
        }
    }

    private class TargetAddressesValidator implements TargetMethod.Closure {
        public Address lastAddress;

        @Override
        public boolean doTargetMethod(TargetMethod targetMethod) {
            assert targetMethod.start().greaterThan(lastAddress) : "method order violation: " + targetMethod.start().to0xHexString() + " must be greater than " + lastAddress.to0xHexString();
            lastAddress = targetMethod.start();
            final Address tmDataStart = DebugHeap.adjustForDebugTag(targetMethod.start().asPointer());
            final Address tmRefLits = Reference.fromJava(targetMethod.referenceLiterals()).toOrigin().asAddress();
            assert tmDataStart.equals(tmRefLits) : "reference literals not at method start for " + targetMethod + " should: " + tmDataStart.to0xHexString() + " is: " + tmRefLits.to0xHexString();
            CodePointer tmCodeStart = targetMethod.codeStart();
            final Offset tmCodeSize = Offset.fromLong(targetMethod.codeLength());
            final Address tmUpperBound = tmDataStart.plus(targetMethod.size()).minus(1);
            CodePointer tmCodeEnd = tmCodeStart.plus(tmCodeSize).minus(1);
            assert tmCodeEnd.toAddress().lessEqual(tmUpperBound)
                : "code exceeds upper bound for " + targetMethod + " code start: " + tmCodeStart.to0xHexString() + " size: " + tmCodeSize.to0xHexString() +
                  " ends at: " + tmCodeEnd.to0xHexString() + " exceeds: " + tmUpperBound.to0xHexString();
            return true;
        }
    }

    private CodeCacheValidation() {
        super("code cache validation", null, Mode.Safepoint);
    }

    /**
     * Validates the machine code of a single target method.
     * This only regards direct calls in the method's machine code.
     */
    public boolean validateSingleMethod(final TargetMethod tm) {
        validateDirectCallsOf(tm);
        return true;
    }

    @Override
    protected void doIt() {
        validateDirectCalls();
        validateDispatchTableEntries();
        validateTargetStates();
        validateTargetMethodAddresses();
        doAllThreads();
    }

    @Override
    protected void doThread(VmThread vmThread, Pointer ip, Pointer sp, Pointer fp) {
        if (ip.isZero() && sp.isZero() && fp.isZero()) {
            return;
        }
        sfw.setTLA(vmThread.tla());
        sfw.inspect(ip, sp, fp, stackValidator);
    }

    private final StackValidator stackValidator = new StackValidator();

    private final VmStackFrameWalker sfw = new VmStackFrameWalker(Pointer.zero());

    private final DirectCallValidator directCallValidator = new DirectCallValidator();

    private DispatchTableValidator dispatchTableValidator = new DispatchTableValidator();

    private final TargetStateValidator targetStateValidator = new TargetStateValidator();

    private final TargetAddressesValidator targetAddressesValidator = new TargetAddressesValidator();

    private void validateDirectCalls() {
        CodeManager.runtimeBaselineCodeRegion.doNewTargetMethods(directCallValidator);
        CodeManager.runtimeOptCodeRegion.doAllTargetMethods(directCallValidator);
        Code.bootCodeRegion().doAllTargetMethods(directCallValidator);
    }

    private void validateDispatchTableEntries() {
        ClassActor.allClassesDo(dispatchTableValidator);
    }

    private void validateTargetStates() {
        CodeManager.runtimeBaselineCodeRegion.doAllTargetMethods(targetStateValidator);
    }

    private void validateTargetMethodAddresses() {
        targetAddressesValidator.lastAddress = Address.zero();
        CodeManager.runtimeBaselineCodeRegion.doNewTargetMethods(targetAddressesValidator);
    }

    private boolean validCodeAddress(CodePointer cp) {
        return Code.contains(cp.toAddress());
    }

    private void validateDirectCallsOf(TargetMethod targetMethod) {
        if (targetMethod.classMethodActor != null && targetMethod.classMethodActor.isTemplate()) {
            return;
        }
        final Safepoints safepoints = targetMethod.safepoints();
        for (int spi = safepoints.nextDirectCall(0); spi >= 0; spi = safepoints.nextDirectCall(spi + 1)) {
            final int callPos = safepoints.causePosAt(spi);
            if (platform().isa == ISA.AMD64) {
                final CodePointer callTarget = AMD64TargetMethodUtil.readCall32Target(targetMethod, callPos);
                final TargetMethod actualCallee = callTarget.toTargetMethod();
                assert validCodeAddress(callTarget) : "invalid call target (address) in direct call from " + targetMethod + "@" + spi + "(pos " + callPos + ") -> " + actualCallee + " (target: " + callTarget.to0xHexString() + ")";
                assert actualCallee != null && validEntryPoint(callTarget, actualCallee) : "invalid entry point in direct call from " + targetMethod + "@" + spi + " -> " + actualCallee + " (target: " + callTarget.to0xHexString() + ")";
            } else {
                throw FatalError.unimplemented();
            }
        }
    }

    private void validateHub(final Hub hub, final int vstart, final boolean checkVtable) {
        if (checkVtable) {
            // vtable entries - this is not necessary for static hubs describing interfaces or primitive types
            final int vend = vstart + hub.vTableLength();
            for (int i = vstart; i < vend; ++i) {
                final CodePointer address = CodePointer.from(hub.getWord(i));
                assert validCodeAddress(address) : "invalid code address in vtable entry: " + hub + "@" + i + " (vt@" + (i - vstart) + ") -> " + address.toString();
            }
        }

        // itable entries
        final int istart = hub.iTableStartIndex;
        final int iend = istart + hub.iTableLength;
        for (int i = istart + 1; i < iend; ++i) { // start iterating at istart+1 because the first entry is null
            final CodePointer p = CodePointer.from(hub.getWord(i));
            assert p.isZero()          // itable entries can be zero
                || Hub.validItableEntry(p)  // or they can represent a class ID
                || validCodeAddress(p) // or they can point to code
                : "invalid itable entry: " + hub + "@" + i + " (it@" + (i - istart) + ") -> " + p.toString();
        }
    }

    private boolean validEntryPoint(CodePointer a, TargetMethod tm) {
        return a.equals(tm.getEntryPoint(BASELINE_ENTRY_POINT))
            || a.equals(tm.getEntryPoint(OPTIMIZED_ENTRY_POINT))
            || a.equals(tm.getEntryPoint(VTABLE_ENTRY_POINT))
            || a.equals(tm.getEntryPoint(C_ENTRY_POINT));
    }

}
