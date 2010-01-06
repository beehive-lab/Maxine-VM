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

import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.cps.target.*;
import com.sun.max.vm.compiler.RuntimeCompilerScheme;
import com.sun.max.vm.stack.StackReferenceMapPreparer;
import com.sun.max.vm.stack.StackFrameWalker;
import com.sun.max.vm.stack.StackFrameVisitor;
import com.sun.max.vm.stack.amd64.AMD64OptStackWalking;
import com.sun.max.vm.runtime.FatalError;
import com.sun.max.vm.runtime.amd64.AMD64TrapStateAccess;
import com.sun.max.program.ProgramError;

/**
 * @author Bernd Mathiske
 */
public class AMD64OptimizedTargetMethod extends OptimizedTargetMethod {

    public AMD64OptimizedTargetMethod(ClassMethodActor classMethodActor, RuntimeCompilerScheme compilerScheme) {
        super(classMethodActor, compilerScheme);
    }

    @Override
    public int callerInstructionPointerAdjustment() {
        return -1;
    }

    @Override
    public final int registerReferenceMapSize() {
        return AMD64TargetMethod.registerReferenceMapSize();
    }

    @Override
    public final void patchCallSite(int callOffset, Word callEntryPoint) {
        AMD64TargetMethod.patchCall32Site(this, callOffset, callEntryPoint);
    }

    @Override
    public void forwardTo(TargetMethod newTargetMethod) {
        AMD64TargetMethod.forwardTo(this, newTargetMethod);
    }

    @Override
    public void prepareReferenceMap(StackFrameWalker.Cursor current, StackFrameWalker.Cursor callee, StackReferenceMapPreparer preparer) {
        StackFrameWalker.CalleeKind calleeKind = callee.calleeKind();
        Pointer registerState = Pointer.zero();
        switch (calleeKind) {
            case TRAMPOLINE:
                // compute the register reference map from the call at this site
                // TODO: get the signature from the call somehow
                AMD64OptStackWalking.prepareTrampolineRefMap(current, callee, preparer);
                break;
            case TRAP_STUB:  // fall through
                // get the register state from the callee's frame
                registerState = callee.sp().plus(callee.targetMethod().frameSize()).minus(AMD64TrapStateAccess.TRAP_STATE_SIZE_WITHOUT_RIP);
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
            throw ProgramError.unexpected();
        }

        // prepare the map for this stack frame
        Pointer refmapFramePointer = current.sp();
        preparer.tracePrepareReferenceMap(this, stopIndex, refmapFramePointer, "frame");
        throw ProgramError.unexpected();
    }

    @Override
    public void catchException(StackFrameWalker.Cursor current, StackFrameWalker.Cursor callee, Throwable throwable) {
        AMD64OptStackWalking.catchException(this, current, callee, throwable);
    }

    @Override
    public boolean acceptJavaFrameVisitor(StackFrameWalker.Cursor current, StackFrameWalker.Cursor callee, StackFrameVisitor visitor) {
        return AMD64OptStackWalking.acceptJavaFrameVisitor(this, current, callee, visitor);
    }

    @Override
    public void advance(StackFrameWalker.Cursor current) {
        AMD64OptStackWalking.advance(this, current);
    }
}
