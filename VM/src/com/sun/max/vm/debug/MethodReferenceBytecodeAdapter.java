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
package com.sun.max.vm.debug;

import java.util.*;

import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.compiler.snippet.ResolutionSnippet.*;
import com.sun.max.vm.runtime.*;

/**
 * Byte code scanner that finds method references.
 *
 * @author Bernd Mathiske
 */
class MethodReferenceBytecodeAdapter extends BytecodeAdapter {

    private final ConstantPool constantPool;
    private final LinkedList<MethodActor> staticAndSpecialMethodActors;
    private final LinkedList<MethodActor> virtualMethodActors;
    private final LinkedList<MethodActor> interfaceMethodActors;

    MethodReferenceBytecodeAdapter(ConstantPool constantPool, LinkedList<MethodActor> staticAndSpecialMethodActors,
                    final LinkedList<MethodActor> virtualMethodActors, LinkedList<MethodActor> interfaceMethodActors) {
        this.constantPool = constantPool;
        this.staticAndSpecialMethodActors = staticAndSpecialMethodActors;
        this.virtualMethodActors = virtualMethodActors;
        this.interfaceMethodActors = interfaceMethodActors;
    }

    @Override
    protected void invokevirtual(int index) {
        final ClassMethodRefConstant methodRef = constantPool.classMethodAt(index);
        if (methodRef.isResolvableWithoutClassLoading(constantPool)) {
            virtualMethodActors.add(methodRef.resolve(constantPool, index));
        }
    }

    @Override
    protected void invokespecial(int index) {
        final ClassMethodRefConstant methodRef = constantPool.classMethodAt(index);
        if (methodRef.isResolvableWithoutClassLoading(constantPool)) {
            final ResolutionGuard.InPool guard = constantPool.makeResolutionGuard(index, ResolveSpecialMethod.SNIPPET);
            staticAndSpecialMethodActors.add(ResolveSpecialMethod.resolveSpecialMethod(guard));
        }
    }

    @Override
    protected void invokestatic(int index) {
        final ClassMethodRefConstant methodRef = constantPool.classMethodAt(index);
        if (methodRef.isResolvableWithoutClassLoading(constantPool)) {
            final ResolutionGuard.InPool guard = constantPool.makeResolutionGuard(index, ResolveStaticMethod.SNIPPET);
            staticAndSpecialMethodActors.add(ResolveStaticMethod.resolveStaticMethod(guard));
        }
    }

    @Override
    protected void invokeinterface(int index, int count) {
        final InterfaceMethodRefConstant methodRef = constantPool.interfaceMethodAt(index);
        if (methodRef.isResolvableWithoutClassLoading(constantPool)) {
            interfaceMethodActors.add(methodRef.resolve(constantPool, index));
        }
    }
}
