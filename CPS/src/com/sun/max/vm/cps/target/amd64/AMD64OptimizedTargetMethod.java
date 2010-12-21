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
package com.sun.max.vm.cps.target.amd64;

import static com.sun.max.vm.MaxineVM.*;

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
        switch (calleeKind) {
            case TRAMPOLINE:
                // compute the register reference map from the call at this site
                AMD64OptStackWalking.prepareTrampolineRefMap(current, callee, preparer);
                break;
            case TRAP_STUB:  // fall through
                // get the register state from the callee's frame
                registerState = callee.sp();
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
            case NATIVE:
                // no register state.
                break;
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

            int registerReferenceMapSize = registerReferenceMapSize();
            int byteIndex = frameReferenceMapsSize() + (registerReferenceMapSize * safepointIndex);
            Pointer slotPointer = registerState;
            preparer.tracePrepareReferenceMap(this, stopIndex, slotPointer, "CPS register state");
            for (int i = 0; i < registerReferenceMapSize; i++) {
                preparer.setReferenceMapBits(current, slotPointer, referenceMaps[byteIndex] & 0xff, Bytes.WIDTH);
                slotPointer = slotPointer.plusWords(Bytes.WIDTH);
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
