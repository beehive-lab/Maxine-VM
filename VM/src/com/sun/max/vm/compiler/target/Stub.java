package com.sun.max.vm.compiler.target;

import static com.sun.max.platform.Platform.*;
import static com.sun.max.vm.compiler.target.TargetMethod.Flavor.*;

import java.util.*;

import com.sun.c1x.target.amd64.*;
import com.sun.max.annotate.*;
import com.sun.max.io.*;
import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.TargetBundleLayout.*;
import com.sun.max.vm.compiler.target.amd64.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.runtime.amd64.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.stack.StackFrameWalker.*;
import com.sun.max.vm.stack.amd64.*;

public class Stub extends TargetMethod {

    public Stub(Flavor flavor, String stubName,  int frameSize, byte[] code, int callPosition, ClassMethodActor callee, int registerRestoreEpilogueOffset) {
        super(flavor, stubName, CallEntryPoint.OPTIMIZED_ENTRY_POINT);
        this.setFrameSize(frameSize);
        this.setRegisterRestoreEpilogueOffset(registerRestoreEpilogueOffset);

        final TargetBundleLayout targetBundleLayout = new TargetBundleLayout(0, 0, 0);
        targetBundleLayout.update(ArrayField.code, code.length);
        Code.allocate(targetBundleLayout, this);
        setData(null, null, code);
        if (callPosition != -1) {
            assert callee != null;
            setStopPositions(new int[] { callPosition }, new Object[] { callee }, 0, 0);
        }
    }

    @HOSTED_ONLY
    private static boolean atFirstOrLastInstruction(Cursor current) {
        if (platform().isa == ISA.AMD64) {
            // check whether the current ip is at the first instruction or a return
            // which means the stack pointer has not been adjusted yet (or has already been adjusted back)
            TargetMethod targetMethod = current.targetMethod();
            Pointer entryPoint = targetMethod.callEntryPoint.equals(CallEntryPoint.C_ENTRY_POINT) ?
                CallEntryPoint.C_ENTRY_POINT.in(targetMethod) :
                CallEntryPoint.OPTIMIZED_ENTRY_POINT.in(targetMethod);

            return entryPoint.equals(current.ip()) || current.stackFrameWalker().readByte(current.ip(), 0) == AMD64TargetMethodUtil.RET;
        }
        throw FatalError.unimplemented();
    }

    @Override
    public void advance(Cursor current) {
        if (platform().isa == ISA.AMD64) {
            TargetMethod targetMethod = current.targetMethod();
            Pointer sp = current.sp();
            Pointer ripPointer = sp.plus(targetMethod.frameSize());
            if (MaxineVM.isHosted()) {
                // Only during a stack walk in the context of the Inspector can execution
                // be anywhere other than at a recorded stop (i.e. call or safepoint).
                AdapterGenerator generator = AdapterGenerator.forCallee(current.targetMethod());
                if (generator != null && generator.advanceIfInPrologue(current)) {
                    return;
                }
                if (atFirstOrLastInstruction(current)) {
                    ripPointer = sp;
                }
            }

            StackFrameWalker stackFrameWalker = current.stackFrameWalker();
            Pointer callerIP = stackFrameWalker.readWord(ripPointer, 0).asPointer();
            Pointer callerSP = ripPointer.plus(Word.size()); // Skip return instruction pointer on stack
            Pointer callerFP;
            if (targetMethod.is(TrapStub)) {
                // RBP is whatever was in the frame pointer register at the time of the trap
                Pointer calleeSaveArea = sp;
                callerFP = stackFrameWalker.readWord(calleeSaveArea, AMD64TrapStateAccess.CSA.offsetOf(AMD64.rbp)).asPointer();
            } else {
                // Propagate RBP unchanged as OPT methods do not touch this register.
                callerFP = current.fp();
            }
            stackFrameWalker.advance(callerIP, callerSP, callerFP, !targetMethod.is(TrapStub));
        } else {
            throw FatalError.unimplemented();
        }
    }

//    @Override
//    public void advance(Cursor cursor) {
//        int ripAdjustment = MaxineVM.isHosted() ? computeRipAdjustment(cursor) : Word.size();
//        StackFrameWalker stackFrameWalker = cursor.stackFrameWalker();
//
//        Pointer ripPointer = cursor.sp().plus(ripAdjustment);
//        Pointer callerIP = stackFrameWalker.readWord(ripPointer, 0).asPointer();
//        Pointer callerSP = ripPointer.plus(Word.size()); // Skip RIP word
//        Pointer callerFP = cursor.fp();
//        stackFrameWalker.advance(callerIP, callerSP, callerFP, true);
//    }

//    @Override
//    public boolean acceptStackFrameVisitor(Cursor cursor, StackFrameVisitor visitor) {
//        int ripAdjustment = MaxineVM.isHosted() ? computeRipAdjustment(cursor) : Word.size();
//
//        Pointer ripPointer = cursor.sp().plus(ripAdjustment);
//        Pointer fp = ripPointer.minus(frameSize());
//        return visitor.visitFrame(new AdapterStackFrame(cursor.stackFrameWalker().calleeStackFrame(), new Opt2BaselineAdapterFrameLayout(frameSize()), cursor.targetMethod(), cursor.ip(), fp, cursor.sp()));
//    }

    @Override
    public boolean acceptStackFrameVisitor(Cursor current, StackFrameVisitor visitor) {
        AdapterGenerator generator = AdapterGenerator.forCallee(current.targetMethod());
        Pointer sp = current.sp();
        if (MaxineVM.isHosted()) {
            // Only during a stack walk in the context of the Inspector can execution
            // be anywhere other than at a recorded stop (i.e. call or safepoint).
            if (atFirstOrLastInstruction(current) || (generator != null && generator.inPrologue(current.ip(), current.targetMethod()))) {
                sp = sp.minus(current.targetMethod().frameSize());
            }
        }
        StackFrameWalker stackFrameWalker = current.stackFrameWalker();
        StackFrame stackFrame = new AMD64JavaStackFrame(stackFrameWalker.calleeStackFrame(), current.targetMethod(), current.ip(), sp, sp);
        return visitor.visitFrame(stackFrame);
    }
    @Override
    public void gatherCalls(Set<MethodActor> directCalls, Set<MethodActor> virtualCalls, Set<MethodActor> interfaceCalls, Set<MethodActor> inlinedMethods) {
    }

    @Override
    public boolean isPatchableCallSite(Address callSite) {
        FatalError.unexpected("Adapter should never be patched");
        return false;
    }

    @Override
    public void fixupCallSite(int callOffset, Address callEntryPoint) {
        AMD64TargetMethodUtil.fixupCall32Site(this, callOffset, callEntryPoint);
    }

    @Override
    public void patchCallSite(int callOffset, Address callEntryPoint) {
        FatalError.unexpected("Adapter should never be patched");
    }

    @Override
    public Address throwAddressToCatchAddress(boolean isTopFrame, Address throwAddress, Class< ? extends Throwable> throwableClass) {
        if (isTopFrame) {
            throw FatalError.unexpected("Exception occurred in frame adapter");
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
    public byte[] referenceMaps() {
        return null;
    }

    @Override
    public void prepareReferenceMap(Cursor current, Cursor callee, StackReferenceMapPreparer preparer) {
    }

    @Override
    public void catchException(Cursor current, Cursor callee, Throwable throwable) {
        // Exceptions do not occur in stubs
    }
}
