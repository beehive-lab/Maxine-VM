package com.sun.max.vm.compiler.target;

import static com.sun.max.platform.Platform.*;

import java.util.*;

import com.sun.max.io.*;
import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.TargetBundleLayout.*;
import com.sun.max.vm.compiler.target.amd64.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.stack.StackFrameWalker.*;
import com.sun.max.vm.stack.amd64.*;

public class Stub extends TargetMethod {
    public Stub(Flavor flavor, String stubName, int frameSize, byte[] code, int callPosition, ClassMethodActor callee, int registerRestoreEpilogueOffset) {
        super(flavor, stubName, CallEntryPoint.OPTIMIZED_ENTRY_POINT);
        this.setFrameSize(frameSize);
        this.setRegisterRestoreEpilogueOffset(registerRestoreEpilogueOffset);

        final TargetBundleLayout targetBundleLayout = new TargetBundleLayout(0, 0, 0);
        targetBundleLayout.update(ArrayField.code, code.length);
        Code.allocate(targetBundleLayout, this);
        setData(null, null, code);
        if (callPosition != -1) {
            assert callee != null;
            setStopPositions(new int[] { callPosition}, new Object[] { callee}, 0, 0);
        }
    }

    @Override
    public void advance(Cursor current) {
        if (platform().isa == ISA.AMD64) {
            AMD64OptStackWalking.advance(current);
        } else {
            throw FatalError.unimplemented();
        }
    }

    @Override
    public boolean acceptStackFrameVisitor(Cursor current, StackFrameVisitor visitor) {
        if (platform().isa == ISA.AMD64) {
            return AMD64OptStackWalking.acceptStackFrameVisitor(current, visitor);
        }
        throw FatalError.unimplemented();
    }

    @Override
    public void gatherCalls(Set<MethodActor> directCalls, Set<MethodActor> virtualCalls, Set<MethodActor> interfaceCalls, Set<MethodActor> inlinedMethods) {
        if (directCallees != null) {
            assert directCallees.length == 1 && directCallees[0] instanceof ClassMethodActor;
            directCalls.add((MethodActor) directCallees[0]);
        }
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
