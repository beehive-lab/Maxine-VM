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
package com.sun.max.vm.reflection;

import java.lang.reflect.*;

import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;


/**
 * This class serves two purposes. Firstly, it is a marker for classes whose bytecodes should not be verified (a la
 * {@link sun.reflect.MagicAccessorImpl}). Secondly, it provides static methods for generating stubs that are used to
 * implement {@link Method#invoke(Object, Object...)} and {@link Constructor#newInstance(Object...)}.
 * 
 * The bytecode generation is derived from the JDK 1.4+ mechanism for the same purpose.
 * 
 * @author Doug Simon
 * @see sun.reflect.MethodAccessorGenerator
 */
public abstract class GeneratedStub {

    GeneratedStub() {
        // MUST BE EMPTY
    }

    /**
     * Determines if this stub can access a given type. Only used to determine if the stub can be run under HotSpot or
     * any other VM that will subject the stub to standard verification.
     */
    public boolean canAccess(Class type) {
        return Modifier.isPublic(type.getModifiers()) || getClass().getPackage().equals(type.getPackage());
    }

    /**
     * Determines if this stub can access all the given types.
     * 
     * @see #canAccess(Class)
     */
    public boolean canAccess(Class... types) {
        for (Class type : types) {
            if (!canAccess(type)) {
                return false;
            }
        }
        return true;
    }

    /**
     * A reference to the constructor or method for which this stub was generated.
     */
    private MethodActor _target;

    final void setTarget(AccessibleObject target) {
        assert _target == null;
        if (target instanceof Constructor) {
            _target = MethodActor.fromJavaConstructor((Constructor) target);
        } else {
            assert target instanceof Method;
            _target = MethodActor.fromJava((Method) target);
        }
    }

    @Override
    public final String toString() {
        return getClass().getSimpleName() + "[" + _target + "]";
    }

    /**
     * Generates a stub for invoking a given method reflectively.
     */
    public static GeneratedMethodStub newMethodStub(Method method, Boxing boxing) {

        final GeneratedMethodStub stub = new InvocationStubGenerator<GeneratedMethodStub>(
                        method,
                        GeneratedMethodStub.class,
                        MethodActor.fromJava(method).name(),
                        method.getDeclaringClass(),
                        method.getReturnType(),
                        method.getParameterTypes(),
                        Modifier.isStatic(method.getModifiers()),
                        Modifier.isPrivate(method.getModifiers()),
                        null,
                        false,
                        boxing).stub();
        stub.setTarget(method);
        return stub;
    }

    /**
     * Generates a stub for invoking a given constructor reflectively.
     */
    public static GeneratedConstructorStub newConstructorStub(Constructor constructor, boolean forSerialization, Boxing boxing) {
        final GeneratedConstructorStub stub = new InvocationStubGenerator<GeneratedConstructorStub>(
                        constructor,
                        GeneratedConstructorStub.class,
                        SymbolTable.INIT,
                        constructor.getDeclaringClass(),
                        Void.TYPE,
                        constructor.getParameterTypes(),
                        false,
                        Modifier.isPrivate(constructor.getModifiers()),
                        constructor.getDeclaringClass(),
                        forSerialization,
                        boxing).stub();
        stub.setTarget(constructor);
        return stub;
    }
}
