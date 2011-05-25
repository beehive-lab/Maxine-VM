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

import java.util.*;

import com.sun.max.io.*;
import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.TargetBundleLayout.ArrayField;
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
            setStopPositions(new int[] {callPosition}, new Object[] {callee}, 0, 0);
        }
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
            AMD64TargetMethodUtil.advance(current);
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
    public void gatherCalls(Set<MethodActor> directCalls, Set<MethodActor> virtualCalls, Set<MethodActor> interfaceCalls, Set<MethodActor> inlinedMethods) {
        if (directCallees != null) {
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
    public void fixupCallSite(int callOffset, Address callEntryPoint) {
        AMD64TargetMethodUtil.fixupCall32Site(this, callOffset, callEntryPoint);
    }

    @Override
    public void patchCallSite(int callOffset, Address callEntryPoint) {
        FatalError.unexpected("Stub should never be patched");
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
