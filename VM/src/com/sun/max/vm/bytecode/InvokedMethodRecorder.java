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

import com.sun.max.collect.*;
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
    protected final AppendableSequence<MethodActor> _directCalls;
    protected final AppendableSequence<MethodActor> _virtualCalls;
    protected final AppendableSequence<MethodActor> _interfaceCalls;
    protected final ClassMethodActor _classMethodActor;
    protected final ConstantPool _constantPool;

    public InvokedMethodRecorder(ClassMethodActor classMethodActor,
                    AppendableSequence<MethodActor> directCalls,
                    AppendableSequence<MethodActor> virtualCalls,
                    AppendableSequence<MethodActor> interfaceCalls) {
        _classMethodActor = classMethodActor;
        _constantPool = classMethodActor.codeAttribute().constantPool();
        _directCalls = directCalls;
        _virtualCalls = virtualCalls;
        _interfaceCalls = interfaceCalls;
    }

    private static final String _maxPackagePrefix = new com.sun.max.Package().name();

    protected void addMethodCall(int index, AppendableSequence<MethodActor> sequence) {
        final MethodRefConstant methodRefConstant = _constantPool.methodAt(index);
        if (methodRefConstant.isResolvableWithoutClassLoading(_constantPool)) {
            try {
                final MethodActor methodActor = methodRefConstant.resolve(_constantPool, index);
                sequence.append(methodActor);
            } catch (PrototypeOnlyMethodError prototypeOnlyMethodError) {
                ProgramError.unexpected(_classMethodActor.format("%H.%n(%p) calls prototype-only method " + methodRefConstant.valueString(_constantPool)));
            }
        } else {
            final TypeDescriptor holder = methodRefConstant.holder(_constantPool);
            final String holderName = holder.toJavaString();
            if (holderName.startsWith(_maxPackagePrefix)) {
                ProgramError.unexpected(_classMethodActor.format("%H.%n(%p) calls unresolved Maxine method " + methodRefConstant.valueString(_constantPool)));
            }
        }
    }

    @Override
    protected void invokestatic(int index) {
        addMethodCall(index, _directCalls);
    }

    @Override
    protected void invokespecial(int index) {
        addMethodCall(index, _directCalls);
    }

    @Override
    protected void invokevirtual(int index) {
        addMethodCall(index, _virtualCalls);
    }

    @Override
    protected void invokeinterface(int index, int count) {
        addMethodCall(index, _interfaceCalls);
    }
}
