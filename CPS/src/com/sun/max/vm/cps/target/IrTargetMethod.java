/*
 * Copyright (c) 2008, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.cps.target;

import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.cps.ir.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.stack.StackFrameWalker.Cursor;

/**
 * This class exists solely as a work around when running the IR tests where compilation stops at some IR level
 * above TargetMethod. In this context, the only thing needed from the target method is the entry point and so this
 * hidden class is just a bridge from {@link TargetMethod#getEntryPoint(CallEntryPoint)} to
 * {@link IrMethod#getEntryPoint(CallEntryPoint)}.
 */
public class IrTargetMethod extends CPSTargetMethod {

    public static TargetMethod asTargetMethod(IrMethod irMethod) {
        if (irMethod == null) {
            return null;
        }
        if (irMethod instanceof TargetMethod) {
            return (TargetMethod) irMethod;
        }
        return new IrTargetMethod(irMethod);
    }

    final IrMethod irMethod;

    IrTargetMethod(IrMethod irMethod) {
        super(irMethod.classMethodActor(), null);
        this.irMethod = irMethod;
    }

    @Override
    protected CPSTargetMethod createDuplicate() {
        throw ProgramError.unexpected();
    }

    @Override
    public boolean isPatchableCallSite(Address callSite) {
        throw ProgramError.unexpected();
    }

    @Override
    public Word getEntryPoint(CallEntryPoint callEntryPoint) {
        return irMethod.getEntryPoint(callEntryPoint);
    }

    @Override
    public void forwardTo(TargetMethod newTargetMethod) {
        throw ProgramError.unexpected();
    }

    @Override
    public void fixupCallSite(int callOffset, Address callTarget) {
        throw ProgramError.unexpected();
    }

    @Override
    public void patchCallSite(int callOffset, Address callTarget) {
        throw ProgramError.unexpected();
    }

    @Override
    public int registerReferenceMapSize() {
        throw ProgramError.unexpected();
    }

    @Override
    public VMFrameLayout stackFrameLayout() {
        throw ProgramError.unexpected();
    }

    @Override
    public void catchException(Cursor current, Cursor callee, Throwable throwable) {
        throw ProgramError.unexpected();
    }

    @Override
    public boolean acceptStackFrameVisitor(Cursor current, StackFrameVisitor visitor) {
        throw ProgramError.unexpected();
    }

    @Override
    public void advance(Cursor current) {
        throw ProgramError.unexpected();
    }

    @Override
    public void prepareReferenceMap(Cursor current, Cursor callee, StackReferenceMapPreparer preparer) {
        throw ProgramError.unexpected();
    }
}
