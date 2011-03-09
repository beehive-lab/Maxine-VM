/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

import static com.sun.max.vm.MaxineVM.*;
import static com.sun.max.vm.stack.StackReferenceMapPreparer.*;

import com.sun.cri.ci.*;
import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.compiler.target.amd64.*;
import com.sun.max.vm.cps.target.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.stack.StackFrameWalker.Cursor;
import com.sun.max.vm.stack.amd64.*;

/**
 * @author Bernd Mathiske
 */
public class AMD64OptimizedTargetMethod extends OptimizedTargetMethod {

    public AMD64OptimizedTargetMethod(ClassMethodActor classMethodActor) {
        super(classMethodActor);
    }

    @Override
    protected CPSTargetMethod createDuplicate() {
        return new AMD64OptimizedTargetMethod(classMethodActor);
    }

    @Override
    public int callerInstructionPointerAdjustment() {
        return -1;
    }

    @Override
    public final int registerReferenceMapSize() {
        return AMD64TargetMethodUtil.registerReferenceMapSize();
    }

    @Override
    public boolean isPatchableCallSite(Address callSite) {
        return AMD64TargetMethodUtil.isPatchableCallSite(callSite);
    }

    @Override
    public final void fixupCallSite(int callOffset, Address callEntryPoint) {
        AMD64TargetMethodUtil.fixupCall32Site(this, callOffset, callEntryPoint);
    }

    @Override
    public void forwardTo(TargetMethod newTargetMethod) {
        AMD64TargetMethodUtil.forwardTo(this, newTargetMethod);
    }

    @Override
    public final void patchCallSite(int callOffset, Address callEntryPoint) {
        AMD64TargetMethodUtil.mtSafePatchCallDisplacement(this, codeStart().plus(callOffset), callEntryPoint.asAddress());
    }

    @Override
    public void prepareReferenceMap(Cursor current, Cursor callee, StackReferenceMapPreparer preparer) {
        StackFrameWalker.CalleeKind calleeKind = callee.calleeKind();
        Pointer registerState = Pointer.zero();
        Pointer resumptionIp = current.ip();
        CiCalleeSaveArea csa = null;
        switch (calleeKind) {
            case TRAMPOLINE:
                // compute the register reference map from the call at this site
                AMD64OptStackWalking.prepareTrampolineRefMap(current, callee, preparer);
                break;
            case TRAP_STUB:  // fall through
                // get the register state from the callee's frame
                registerState = callee.sp();
                assert callee.targetMethod().getRegisterConfig() == vm().registerConfigs.trapStub;
                csa = callee.targetMethod().getRegisterConfig().getCalleeSaveArea();
                int trapNum = vm().trapStateAccess.getTrapNumber(registerState);
                Class<? extends Throwable> throwableClass = Trap.Number.toImplicitExceptionClass(trapNum);
                if (throwableClass != null) {
                    // this trap will cause an exception to be thrown
                    resumptionIp = throwAddressToCatchAddress(true, resumptionIp, throwableClass).asPointer();
                    if (resumptionIp.isZero()) {
                        // there is no handler for this exception in this method
                        // so don't bother scanning the references
                        return;
                    }
                }
                break;
            case NONE:
            case NATIVE:
            case JAVA:
                // no register state.
                break;
            case CALLEE_SAVED:
                throw FatalError.unexpected("opt methods should not call callee-saved methods");
        }
        int stopIndex = findClosestStopIndex(current.ip());

        if (!registerState.isZero()) {
            // the callee contains register state from this frame;
            // use register reference maps in this method to fill in the map for the callee
            final int safepointIndex = this.findSafepointIndex(resumptionIp);
            if (safepointIndex < 0) {
                Log.print("Could not find safepoint index for instruction at position ");
                Log.print(resumptionIp.minus(codeStart()).toInt());
                Log.print(" in ");
                Log.printMethod(this, true);
                FatalError.unexpected("Could not find safepoint index");
            }

            // the callee contains register state from this frame;
            // use register reference maps in this method to fill in the map for the callee
            Pointer slotPointer = registerState;
            int registerReferenceMapSize = registerReferenceMapSize();
            int byteIndex = frameReferenceMapsSize() + (registerReferenceMapSize * safepointIndex);
            preparer.tracePrepareReferenceMap(this, stopIndex, slotPointer, "CPS register state");
            // Need to translate from register numbers (as stored in the reg ref maps) to frame slots.
            for (int i = 0; i < registerReferenceMapSize; i++) {
                int b = referenceMaps[byteIndex] & 0xff;
                int reg = i * 8;
                while (b != 0) {
                    if ((b & 1) != 0) {
                        int offset = csa.offsetOf(reg);
                        if (traceStackRootScanning()) {
                            Log.print("    register: ");
                            Log.println(csa.registers[reg].name);
                        }
                        preparer.setReferenceMapBits(callee, slotPointer.plus(offset), 1, 1);
                    }
                    reg++;
                    b = b >>> 1;
                }
                byteIndex++;
            }

        }

        // prepare the map for this stack frame
        Pointer slotPointer = current.sp();
        preparer.tracePrepareReferenceMap(this, stopIndex, slotPointer, "CPS frame");
        int frameReferenceMapSize = frameReferenceMapSize();
        int byteIndex = stopIndex * frameReferenceMapSize;
        for (int i = 0; i < frameReferenceMapSize; i++) {
            preparer.setReferenceMapBits(current, slotPointer, referenceMaps[byteIndex] & 0xff, Bytes.WIDTH);
            slotPointer = slotPointer.plusWords(Bytes.WIDTH);
            byteIndex++;
        }
    }

    @Override
    public void catchException(Cursor current, Cursor callee, Throwable throwable) {
        AMD64OptStackWalking.catchException(this, current, callee, throwable);
    }

    @Override
    public boolean acceptStackFrameVisitor(Cursor current, StackFrameVisitor visitor) {
        return AMD64OptStackWalking.acceptStackFrameVisitor(current, visitor);
    }

    @Override
    public void advance(Cursor current) {
        AMD64OptStackWalking.advance(current);
    }
}
