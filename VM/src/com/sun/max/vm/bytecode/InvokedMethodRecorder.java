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
package com.sun.max.vm.bytecode;

import java.util.*;

import com.sun.max.program.*;
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

    private static final String maxPackagePrefix = new com.sun.max.Package().name();

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
