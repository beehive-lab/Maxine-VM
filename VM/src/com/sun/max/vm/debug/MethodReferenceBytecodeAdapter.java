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
            final ResolutionGuard guard = constantPool.makeResolutionGuard(index, ResolveSpecialMethod.SNIPPET);
            staticAndSpecialMethodActors.add(ResolveSpecialMethod.resolveSpecialMethod(guard));
        }
    }

    @Override
    protected void invokestatic(int index) {
        final ClassMethodRefConstant methodRef = constantPool.classMethodAt(index);
        if (methodRef.isResolvableWithoutClassLoading(constantPool)) {
            final ResolutionGuard guard = constantPool.makeResolutionGuard(index, ResolveStaticMethod.SNIPPET);
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
