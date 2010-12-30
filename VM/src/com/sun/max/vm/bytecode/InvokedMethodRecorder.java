/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.bytecode;

import java.util.*;

import com.sun.max.program.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.type.*;

/**
 * Recorder of invoked methods.
 * A visitor that adds methods referenced by invoke bytecodes to an appendable sequence.
 *
 * @author Ben L. Titzer
 * @author Doug Simon
 */
public class InvokedMethodRecorder extends BytecodeAdapter {
    protected final Set<MethodActor> directCalls;
    protected final Set<MethodActor> virtualCalls;
    protected final Set<MethodActor> interfaceCalls;
    protected final ClassMethodActor classMethodActor;
    protected final ConstantPool constantPool;

    public InvokedMethodRecorder(ClassMethodActor classMethodActor,
                    Set<MethodActor> directCalls,
                    Set<MethodActor> virtualCalls,
                    Set<MethodActor> interfaceCalls) {
        this.classMethodActor = classMethodActor;
        this.constantPool = classMethodActor.codeAttribute().constantPool;
        this.directCalls = directCalls;
        this.virtualCalls = virtualCalls;
        this.interfaceCalls = interfaceCalls;
    }

    private static final String maxPackagePrefix = "com.sun.max";

    protected void addMethodCall(int index, Set<MethodActor> sequence) {
        final MethodRefConstant methodRefConstant = constantPool.methodAt(index);
        if (methodRefConstant.isResolvableWithoutClassLoading(constantPool)) {
            try {
                final MethodActor methodActor = methodRefConstant.resolve(constantPool, index);
                sequence.add(methodActor);
            } catch (HostOnlyMethodError prototypeOnlyMethodError) {
                ProgramError.unexpected(classMethodActor.format("%H.%n(%p) calls prototype-only method " + methodRefConstant.valueString(constantPool)));
            }
        } else {
            final TypeDescriptor holder = methodRefConstant.holder(constantPool);
            final String holderName = holder.toJavaString();
            if (holderName.startsWith(maxPackagePrefix)) {
                ProgramError.unexpected(classMethodActor.format("%H.%n(%p) calls unresolved Maxine method " + methodRefConstant.valueString(constantPool)));
            }
        }
    }

    @Override
    protected void invokestatic(int index) {
        addMethodCall(index, directCalls);
    }

    @Override
    protected void invokespecial(int index) {
        addMethodCall(index, directCalls);
    }

    @Override
    protected void invokevirtual(int index) {
        addMethodCall(index, virtualCalls);
    }

    @Override
    protected void invokeinterface(int index, int count) {
        addMethodCall(index, interfaceCalls);
    }
}
