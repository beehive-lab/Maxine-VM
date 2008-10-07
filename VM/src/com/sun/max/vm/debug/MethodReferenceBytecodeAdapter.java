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
import com.sun.max.vm.compiler.snippet.*;

/**
 * Byte code scanner that finds method references.
 * 
 * @author Bernd Mathiske
 */
class MethodReferenceBytecodeAdapter extends BytecodeAdapter {

    private final ConstantPool _constantPool;
    private final LinkedList<MethodActor> _staticAndSpecialMethodActors;
    private final LinkedList<MethodActor> _virtualMethodActors;
    private final LinkedList<MethodActor> _interfaceMethodActors;

    MethodReferenceBytecodeAdapter(ConstantPool constantPool, LinkedList<MethodActor> staticAndSpecialMethodActors,
                    final LinkedList<MethodActor> virtualMethodActors, LinkedList<MethodActor> interfaceMethodActors) {
        _constantPool = constantPool;
        _staticAndSpecialMethodActors = staticAndSpecialMethodActors;
        _virtualMethodActors = virtualMethodActors;
        _interfaceMethodActors = interfaceMethodActors;
    }

    @Override
    protected void invokevirtual(int index) {
        final ClassMethodRefConstant methodRef = _constantPool.classMethodAt(index);
        if (methodRef.isResolvableWithoutClassLoading(_constantPool)) {
            _virtualMethodActors.add(methodRef.resolve(_constantPool, index));
        }
    }

    @Override
    protected void invokespecial(int index) {
        final ClassMethodRefConstant methodRef = _constantPool.classMethodAt(index);
        if (methodRef.isResolvableWithoutClassLoading(_constantPool)) {
            _staticAndSpecialMethodActors.add(ResolutionSnippet.ResolveSpecialMethod.quasiFold(_constantPool, index));
        }
    }

    @Override
    protected void invokestatic(int index) {
        final ClassMethodRefConstant methodRef = _constantPool.classMethodAt(index);
        if (methodRef.isResolvableWithoutClassLoading(_constantPool)) {
            _staticAndSpecialMethodActors.add(ResolutionSnippet.ResolveStaticMethod.quasiFold(_constantPool, index));
        }
    }

    @Override
    protected void invokeinterface(int index, int count) {
        final InterfaceMethodRefConstant methodRef = _constantPool.interfaceMethodAt(index);
        if (methodRef.isResolvableWithoutClassLoading(_constantPool)) {
            _interfaceMethodActors.add(methodRef.resolve(_constantPool, index));
        }
    }
}
